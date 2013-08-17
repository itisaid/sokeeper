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
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.IoSession;

import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */ 
public class RpcServerIoHandlerImplTest extends TestCase {
    RpcServerIoHandlerImpl server;

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
        if (server != null) {
            server.shutdown();
            server.shutdown();
        }
    }

    public void test_startup() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        server.startup();
    }

//    public void test_isAlive() throws Throwable {
//        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
//        server.startup();
//
//        assertTrue(NetUtils.selectAvailablePort(9010) != 9010);
//        RpcServerIoHandlerImpl server2 = new RpcServerIoHandlerImpl(new RpcConfiguration(
//                "tcp://localhost:9010/server"));
//        server2.startup();
//        assertFalse(server2.isAlive());
//    }
//
//    public void test_whiteList() throws Throwable {
//        int port = NetUtils.selectAvailablePort(9010);
//        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://0.0.0.0:" + port
//                + "/server?max_connections=0"));
//        server.startup();
//        Collection<String> addrs = NetUtils.getAllLocalAddresses();
//        addrs.remove(RpcAddress.LOCAL_HOST_ADDRESS);
//        if (addrs.size() > 0) {
//            server.getConfiguration().setWhiteIpList(addrs);
//            // at this point 127.0.0.1 is not in white list,so,if we create a client and connect with it will be dropped by server
//            final Collection<SocketAddress> droppedAddrs = new HashSet<SocketAddress>();
//            RpcClientIoHandlerImpl clientLocalHost = new RpcClientIoHandlerImpl(
//                    new RpcConfiguration("tcp://localhost:" + port + "/client")) {
//                public void sessionClosed(IoSession session) throws Exception {
//                    droppedAddrs.add(session.getRemoteAddress());
//                }
//            };
//            // other addresses is in white list,so,it will not be dropped
//            RpcClientIoHandlerImpl clientInWhiteList = new RpcClientIoHandlerImpl(
//                    new RpcConfiguration("tcp://" + addrs.iterator().next() + ":" + port
//                            + "/client"));
//            clientLocalHost.startup();
//            clientInWhiteList.startup();
//            while (droppedAddrs.size() == 0) {
//                Thread.sleep(100);
//            }
//            assertEquals(droppedAddrs.iterator().next(), new InetSocketAddress("localhost", port));
//            while (!clientInWhiteList.isAlive()) {
//                Thread.sleep(100);
//            }
//            clientLocalHost.shutdown();
//            clientInWhiteList.shutdown();
//        }
//
//        server.shutdown();
//    }
//
//    public void test_publishNewAddedServers() throws Throwable {
//        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
//        server.startup();
//        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
//                "tcp://localhost:9010/server"));
//        client.startup();
//        Set<RpcAddress> servers = new HashSet<RpcAddress>();
//        servers.add(RpcSocketAddress.fromFullAddress("localhost:9011"));
//        while (client.getSessions().size() == 0) {
//            Thread.sleep(100);
//        }
//        while (server.getSessions().size() == 0) {
//            Thread.sleep(100);
//        }
//        server.publishNewAddedServers(servers);
//        assertTrue(server.getConfiguration().getServers().contains(servers.iterator().next()));
//        while (client.getConfiguration().getServers().size() <= 1) {
//            Thread.sleep(100);
//        }
//        assertEquals(client.getConfiguration().getServers().size(), 2);
//        client.shutdown();
//
//    }
}
