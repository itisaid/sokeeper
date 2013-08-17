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
package com.sokeeper.rpc.transport;

import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.util.RpcSocketAddress;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcConnectionTest extends TestCase {

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

    public void test_rpcConnection_create() throws Throwable {
        RpcSocketAddress localAddress = new RpcSocketAddress("localhost", 8080);
        RpcSocketAddress remoteAddress = new RpcSocketAddress("localhost", 9090);
        try {
            new RpcConnection(null, null);
            fail();
        } catch (IllegalArgumentException e) {

        }
        try {
            new RpcConnection(localAddress, null);
            fail();
        } catch (IllegalArgumentException e) {

        }
        assertEquals(new RpcConnection(localAddress, remoteAddress), new RpcConnection(
                localAddress, remoteAddress));
        assertEquals(new RpcConnection(localAddress, remoteAddress), new RpcConnection(
                new RpcSocketAddress("localhost", 8080), new RpcSocketAddress("localhost", 9090)));
        assertEquals(new RpcConnection(localAddress, remoteAddress).getLocalAddress(),
                new RpcSocketAddress("localhost", 8080));
        assertEquals(new RpcConnection(localAddress, remoteAddress).getRemoteAddress(),
                new RpcSocketAddress("localhost", 9090));

        RpcConnection.setCurrentRpcConnection(new RpcConnection(localAddress, remoteAddress));
        assertEquals(RpcConnection.getCurrentRpcConnection(), new RpcConnection(
                new RpcSocketAddress("localhost", 8080), new RpcSocketAddress("localhost", 9090)));
        assertEquals(RpcConnection.getCurrentRpcConnection().hashCode(), new RpcConnection(
                new RpcSocketAddress("localhost", 8080), new RpcSocketAddress("localhost", 9090))
                .hashCode());
        assertEquals(RpcConnection.getCurrentRpcConnection().toString(), new RpcConnection(
                new RpcSocketAddress("localhost", 8080), new RpcSocketAddress("localhost", 9090))
                .toString());


    }

}
