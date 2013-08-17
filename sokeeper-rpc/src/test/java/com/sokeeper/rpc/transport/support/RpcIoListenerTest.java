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

import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.transport.support.RpcClientIoHandlerImpl;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcIoListenerTest extends TestCase {
    private RpcServerIoHandlerImpl server;
    private RpcClientIoHandlerImpl client;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9210"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:9210"));
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        client.shutdown();
        server.shutdown();
    }

    public void test_onConnectionCreated() throws Throwable {
        RpcIoListenerImpl serverIoListener = new RpcIoListenerImpl();
        RpcIoListenerImpl clientIoListener = new RpcIoListenerImpl();
        server.registerIoListener(serverIoListener);
        client.registerIoListener(clientIoListener);

        server.startup();
        client.startup();
        while (clientIoListener.getRemoteAddress() == null) {
            Thread.sleep(100);
        }
        while (serverIoListener.getRemoteAddress() == null) {
            Thread.sleep(100);
        }
        assertNotNull(clientIoListener.getRemoteAddress());
        assertNotNull(clientIoListener.getLocalAddress());

        assertEquals(clientIoListener.getEvent(), "onConnectionCreated");
        assertEquals(serverIoListener.getEvent(), "onConnectionCreated");

        assertEquals(clientIoListener.getRemoteAddress(), serverIoListener.getLocalAddress());

        clientIoListener.setConnection(null);
        serverIoListener.setConnection(null);
        client.shutdown();

        while (clientIoListener.getRemoteAddress() == null) {
            Thread.sleep(100);
        }
        while (serverIoListener.getRemoteAddress() == null) {
            Thread.sleep(100);
        }
        assertNotNull(clientIoListener.getRemoteAddress());
        assertNotNull(clientIoListener.getLocalAddress());
        assertEquals(clientIoListener.getEvent(), "onConnectionClosed");
        assertEquals(serverIoListener.getEvent(), "onConnectionClosed");
        assertEquals(clientIoListener.getRemoteAddress(), serverIoListener.getLocalAddress());
        client.unregisterIoListener(clientIoListener);
    }

}
