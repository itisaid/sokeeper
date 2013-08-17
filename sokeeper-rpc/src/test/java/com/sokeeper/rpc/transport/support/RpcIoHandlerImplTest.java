/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.sokeeper.rpc.transport.support;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolDecoderException;

import com.sokeeper.rpc.exception.RpcLocalException;
import com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected;
import com.sokeeper.rpc.exception.RpcLocalExceptionIoWriteToTargetFailed;
import com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets;
import com.sokeeper.rpc.exception.RpcLocalExceptionTimeout;
import com.sokeeper.rpc.exception.RpcRemoteException;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.message.RpcRequest;
import com.sokeeper.rpc.message.RpcResponse;
import com.sokeeper.rpc.transport.support.RpcIoHandlerImpl;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

public class RpcIoHandlerImplTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        sessions = null;
        returned = null;
        ioHandler = new MockRpcIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?timeout_ms=10")) {
            protected Set<IoSession> getSessions() {
                return sessions;
            }
        };
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private Set<IoSession>   sessions  = null;
    private Object           returned  = null;
    private RpcIoHandlerImpl ioHandler = null;

    public Object dummy(Object arg1, Object arg2, Object[] arg3) throws Throwable {
        return returned;
    }

    private interface MyInterface {
        public String sayHello(String name);

    }

    private class MyImpl implements MyInterface {
        public String sayHello(String name) {
            returned = name;
            return name;
        }

    }

    public void test_messageReceived_RpcRequest() throws Throwable {
        MockIoSession session = new MockIoSession();
        session.setRemoteAddress(new InetSocketAddress("localhost", 8080));
        session.setLocalAddress(new InetSocketAddress("localhost", 8899));

        // 1, the message type is not RpcRequest nor RpcResponse
        ioHandler.messageReceived(session, "hello");
        // 2, when no serviceHandler registered
        RpcRequest request = new RpcRequest("serviceName", new Object[] {});
        ioHandler.messageReceived(session, request);
        // 3, we do have handler for the RPC request
        ioHandler.registerRequestHandler(MyInterface.class, new MyImpl());

        Method method = MyInterface.class.getMethod("sayHello", new Class<?>[] { String.class });
        request = new RpcRequest(ioHandler.getServiceName(method), new String[] { "JamesFu" });
        ioHandler.messageReceived(session, request);
        while (returned == null) {
            Thread.sleep(10);
        }
        assertEquals(returned, "JamesFu");
        assertNotNull(ioHandler.getRpcInvoker(request.getServiceName()));

        assertEquals(ioHandler.getResponsesSize(), 0);

        ioHandler.messageReceived(session, new Integer(5));

        final AtomicBoolean sessionCloseCalled = new AtomicBoolean(false);
        ioHandler.exceptionCaught(new MockIoSession() {
            public CloseFuture close() {
                sessionCloseCalled.set(true);
                return null;
            }
        }, new ProtocolDecoderException());
        assertTrue(sessionCloseCalled.get());
        ioHandler.shutdown();
    }

    public void test_invoke() throws Throwable {
        Method method = getClass().getMethod("dummy",
                new Class<?>[] { Object.class, Object.class, new Object[0].getClass() });
        // 1, the parameters illegal
        try {
            ioHandler.invoke(null, null, null, null,false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        //try {
        //    ioHandler.invoke(method, null, null, null,false);
        //    fail();
        //} catch (IllegalArgumentException e) {
        //}
        // 2, targets is null sessions has 1 session
        {
            sessions = new HashSet<IoSession>();
            MockIoSession session = new MockIoSession();
            session.setRemoteAddress(new InetSocketAddress("localhost", 8080));
            sessions.add(session);

            // 1, no remote service should report timeout exception
            try {
                ioHandler.invoke(method, new Object[] {}, null, null,false);
                fail("since no remote service,so should report timeout exception");
            } catch (RpcLocalExceptionTimeout e) {
            }

            // 2, write to remote fail,should report exception
            session.setWriteMessageException(new RuntimeException("message"));
            try {
                ioHandler.invoke(method, new Object[] {}, null, null,false);
                fail("should reprot RpcLocalExceptionIoWriteToTargetFailed");
            } catch (RpcLocalExceptionIoWriteToTargetFailed e) {
            }
            session.setWriteMessageException(null);
        }
        // 3, 1 target, sessions is null or mismatch with target
        {
            sessions = new HashSet<IoSession>();
            try {
                Set<RpcAddress> targets = new HashSet<RpcAddress>();
                targets.add(new RpcSocketAddress("localhost", 8080));
                ioHandler.invoke(method, new Object[] {}, targets, null,false);
                fail("should reprot RpcLocalExceptionIoTargetIsNotConnected");
            } catch (RpcLocalExceptionIoTargetIsNotConnected e) {
            }
            sessions = new HashSet<IoSession>();
            MockIoSession session = new MockIoSession();
            session.setRemoteAddress(new InetSocketAddress("localhost", 9090));
            sessions.add(session);
            try {
                Set<RpcAddress> targets = new HashSet<RpcAddress>();
                targets.add(new RpcSocketAddress("localhost", 8080));
                ioHandler.invoke(method, new Object[] {}, targets, null,false);
                fail("should reprot RpcLocalExceptionIoTargetIsNotConnected");
            } catch (RpcLocalExceptionIoTargetIsNotConnected e) {
            }
        }
        // 4, 2 targets, 1 match 1 mismatched
        {
            sessions = new HashSet<IoSession>();
            MockIoSession session = new MockIoSession();
            session.setRemoteAddress(new InetSocketAddress("localhost", 9090));
            sessions.add(session);
            try {
                Set<RpcAddress> targets = new HashSet<RpcAddress>();
                targets.add(new RpcSocketAddress("localhost", 8080));
                targets.add(new RpcSocketAddress("localhost", 9090));
                ioHandler.invoke(method, new Object[] {}, targets, null,false);
                fail("should reprot RpcLocalExceptionMultipleTargets");
            } catch (RpcLocalExceptionMultipleTargets e) {
                assertEquals(e.getFailedTargets().length, 2);
                assertNotNull(e.getFailedMessage(new RpcSocketAddress("localhost", 8080)));
                assertNotNull(e.getFailedMessage(new RpcSocketAddress("localhost", 9090)));
                assertEquals(e.getServiceName(), ioHandler.getServiceName(method));
                assertNull(e.getFailedMessage(null));
            }
        }

        // 5, 1 target, 1 session and matched
        {
            returned = "Hello";
            sessions = new HashSet<IoSession>();
            MockIoSession session = new MockIoSession() {
                public WriteFuture write(Object message) {
                    RpcRequest request = (RpcRequest) message;
                    RpcResponse response = new RpcResponse(request.getId(), returned);
                    try {
                        ioHandler.messageReceived(this, response);
                    } catch (Exception e) {
                    }
                    return null;
                }
            };
            session.setRemoteAddress(new InetSocketAddress("localhost", 9090));
            sessions.add(session);
            Set<RpcAddress> targets = new HashSet<RpcAddress>();
            targets.add(new RpcSocketAddress("localhost", 9090));

            // if just one target, the result will be returned
            assertEquals(ioHandler.invoke(method, new Object[] {}, targets, null,false), "Hello");

            // if the remote peer returned RpcLocalException the exception will be converted to RpcRemoteException
            returned = new RpcLocalException("RpcLocalException");
            try {
                ioHandler.invoke(method, new Object[] {}, targets, null,false);
                fail("the exception should be converted to RpcLocalException");
            } catch (RpcRemoteException e) {
                assertEquals(e.getMessage(), "RpcLocalException");
            }

            // add one more target
            targets.add(new RpcSocketAddress("localhost", 8080));
            try {
                ioHandler.invoke(method, new Object[] {}, targets, null,false);
                fail("the exception should be converted to RpcLocalExceptionMultipleTargets");
            } catch (RpcLocalExceptionMultipleTargets e) {
                assertEquals(e.getFailedTargets().length, 2);
                assertNotNull(e.getFailedMessage(new RpcSocketAddress("localhost", 8080)));
                assertNotNull(e.getFailedMessage(new RpcSocketAddress("localhost", 9090)));
                assertEquals(e.getServiceName(), ioHandler.getServiceName(method));
            }
        }
        // 6, the responses should keep empty
        assertEquals(ioHandler.getResponsesSize(), 0);
    }
}
