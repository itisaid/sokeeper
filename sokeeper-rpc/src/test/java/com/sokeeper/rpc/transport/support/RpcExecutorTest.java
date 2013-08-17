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

import java.net.InetSocketAddress;


import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import com.sokeeper.rpc.message.RpcRequest;
import com.sokeeper.rpc.message.RpcResponse;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.support.RpcExecutor;
import com.sokeeper.rpc.transport.support.RpcInvoker;
import com.sokeeper.util.RpcSocketAddress;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcExecutorTest extends TestCase {

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private Object messageWrite  = null;
    private Object dummyReturned = null;

    public Object dummy() throws Throwable {
        if (dummyReturned != null && dummyReturned instanceof Throwable) {
            throw (Throwable) dummyReturned;
        }
        return dummyReturned;
    }

    //public Object dummy(Object p1) throws Throwable {
    //    return null;
    //}

    public void test_run() throws Throwable {
        IoSession session = new MockIoSession() {
            public WriteFuture write(Object message) {
                messageWrite = message;
                return null;
            }
        };
        RpcRequest request = new RpcRequest("serviceName", new Object[] {});
        RpcInvoker invoker = new RpcInvoker(getClass().getMethod("dummy", new Class<?>[] {}), this);
        RpcConnection connection = new RpcConnection(new RpcSocketAddress(new InetSocketAddress(
                "localhost", 3000)), new RpcSocketAddress(new InetSocketAddress("localhost", 4000)));
        try {
            new RpcExecutor(connection, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            new RpcExecutor(null, session, request, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        new RpcExecutor(connection, session, request, invoker).run();
        assertEquals(request.getId(), ((RpcResponse) messageWrite).getId());

        dummyReturned = "Hello";
        new RpcExecutor(connection, session, request, invoker).run();
        assertEquals("Hello", ((RpcResponse) messageWrite).getResult());

        dummyReturned = new Exception("Hello");
        new RpcExecutor(connection, session, request, invoker).run();
        assertEquals("Hello", ((Exception) ((RpcResponse) messageWrite).getResult()).getMessage());

        request = new RpcRequest("serviceName", new Object[] { "Hello" });
        new RpcExecutor(connection, session, request, invoker).run();
        assertEquals(((RpcResponse) messageWrite).getResult().getClass(),
                IllegalArgumentException.class);
    }

}
