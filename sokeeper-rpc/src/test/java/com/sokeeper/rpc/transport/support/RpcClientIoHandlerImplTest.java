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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;

import com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected;
import com.sokeeper.rpc.message.HandshakeMessage;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.transport.support.RpcClientIoHandlerImpl;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcClientIoHandlerImplTest extends TestCase {

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

    public void test_auto_reconnect() throws Throwable {
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=10"));
        for (int i = 0; i < 2; i++) {
            client.startup();
        }
        client.shutdown();
    }

    public void dummy() {

    }

    public void test_client_not_connected_no_server() throws Throwable {
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000"));
        try {
            client.invoke(RpcClientIoHandlerImplTest.class.getMethod("dummy", new Class<?>[] {}),
                    new Object[] {}, null, null,false);
            fail("RpcLocalExceptionIoTargetIsNotConnected");
        } catch (RpcLocalExceptionIoTargetIsNotConnected e) {

        }

    }

    public void test_one_client_connect_with_servers() throws Throwable {
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=all&"
                        + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS
                        + "=localhost:9011,localhost:9012"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 3);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i));
            servers[i].startup();
        }
        client.startup();
        // 2, once client connected succeed, should has 3 sessions
        assertEquals(client.getSessions().size(), 3);
        assertTrue(client.isAlive());
        client.shutdown();
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }

    }

    public void test_one_client_connect_with_servers_autoreconnect() throws Throwable {
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=all&"
                        + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS
                        + "=localhost:9011,localhost:9012"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 3);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i));
            servers[i].startup();
        }
        client.startup();
        // 2, once client connected succeed, should has 3 sessions
        assertEquals(client.getSessions().size(), 3);
        servers[0].shutdown();
        while (client.getSessions().size() == 3) {
            Thread.sleep(100);
        }
        servers[0].startup();
        while (client.getSessions().size() != 3) {
            Thread.sleep(100);
        }
        client.shutdown();
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }

    }

    public void test_one_client_connect_with_main_server() throws Throwable {
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=main&"
                        + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS
                        + "=localhost:9011,localhost:9012"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 3);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i));
            servers[i].startup();
        }
        client.startup();
        // 2, once client connected succeed, should has 1 session
        assertEquals(client.getSessions().size(), 1);
        assertEquals(client.getSessions().iterator().next().getRemoteAddress(),
                new InetSocketAddress("localhost", 9010));
        client.shutdown();
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }

    }

    public void test_client_receive_target_settings_from_server_through_handshake()
            throws Throwable {
        Map<SocketAddress, RpcServerIoHandlerImpl> serverMap = new HashMap<SocketAddress, RpcServerIoHandlerImpl>();
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=anyone"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 1);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i
                    + "/server?servers=localhost:9010,localhost:9011,localhost:9012"));
            servers[i].startup();
            serverMap.put((SocketAddress) servers[i].getConfiguration().getMainAddress(),
                    servers[i]);
        }
        client.startup();
        while (client.getConfiguration().getServers().size() < 3) {
            Thread.sleep(100);
        }
        // 2, once client connected succeed, should has 1 session
        assertEquals(client.getSessions().size(), 1);
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }
        client.shutdown();
    }

    public void test_client_receive_target_settings_from_server_through_handshake_not_accept_servers()
            throws Throwable {
        Map<SocketAddress, RpcServerIoHandlerImpl> serverMap = new HashMap<SocketAddress, RpcServerIoHandlerImpl>();
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=anyone&hb_sec=5"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 1);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i
                    + "/server?servers=localhost:9010,localhost:9011,localhost:9012"));
            servers[i].startup();
            serverMap.put((SocketAddress) servers[i].getConfiguration().getMainAddress(),
                    servers[i]);
        }
        client.getConfiguration().setAcceptServersThroughHandshake(false);
        client.startup();
        while(!client.isAlive()){
            Thread.sleep(100);
        }
        IoSession session=client.getSessions().iterator().next();
        while(session.getIdleTime(IdleStatus.WRITER_IDLE)==5){
            Thread.sleep(100);
        }
        // once client received settings from server but the accept_servers is disabled
        // it should not change the configuration's servers settings
        assertEquals(client.getConfiguration().getServers().size(),1);

        client.shutdown();
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }
    }

    public void test_one_client_connect_with_anyone_server() throws Throwable {
        Map<SocketAddress, RpcServerIoHandlerImpl> serverMap = new HashMap<SocketAddress, RpcServerIoHandlerImpl>();
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[3];
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000&connect_policy=anyone&"
                        + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS
                        + "=localhost:9011,localhost:9012"));
        // 1, the targets should has 3 RpcAddress
        assertEquals(client.getConfiguration().getServers().size(), 3);

        for (int i = 0; i < servers.length; i++) {
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:901" + i));
            servers[i].startup();
            serverMap.put((SocketAddress) servers[i].getConfiguration().getMainAddress(),
                    servers[i]);
        }
        client.startup();
        // 2, once client connected succeed, should has 1 session
        assertEquals(client.getSessions().size(), 1);
        SocketAddress connected = client.getSessions().iterator().next().getRemoteAddress();
        // 3, if we shutdown the connected server, should automatically route to next activated server
        serverMap.get(connected).shutdown();
        serverMap.remove(connected);
        while (true) {
            if (client.getSessions().size() == 0) {
                Thread.sleep(100);
                continue;
            }
            if (client.getSessions().iterator().next().getRemoteAddress().equals(connected)) {
                Thread.sleep(100);
                continue;
            }
            break;
        }
        // the new connected server must be the left two servers
        connected = client.getSessions().iterator().next().getRemoteAddress();
        assertEquals(client.getSessions().size(), 1);
        assertTrue(serverMap.containsKey(connected));
        serverMap.get(connected).shutdown();
        serverMap.remove(connected);
        while (true) {
            if (client.getSessions().size() == 0) {
                Thread.sleep(100);
                continue;
            }
            if (client.getSessions().iterator().next().getRemoteAddress().equals(connected)) {
                Thread.sleep(100);
                continue;
            }
            break;
        }
        client.shutdown();
        serverMap.values().iterator().next().shutdown();

    }

    public void test_client_can_tell_server_client_groups() throws Throwable {
        RpcClientIoHandlerImpl clientInGroup = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?groups=slaveGroup"));
        RpcServerIoHandlerImpl server = new RpcServerIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/server"));
        server.startup();
        clientInGroup.startup();
        while (server.getSessions().size() < 1) {
            Thread.sleep(100);
        }
        IoSession session = server.getSessions().iterator().next();
        while (!session.containsAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS)) {
            Thread.sleep(100);
        }
        assertTrue(server.isClientInGroup(session, "slaveGroup"));
        clientInGroup.shutdown();
        server.shutdown();
    }

    public void test_client_startup_fail_fast() throws Throwable {
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?startup_retry_times=3"));
        assertFalse(client.startup());
        client.shutdown();
    }

    public void test_client_connect_non_localhost_server_should_accept_localhost_server_address()
            throws Throwable {
        String nonLocal = null;
        for (String nonLocalIp : NetUtils.getAllLocalAddresses()) {
            if (!nonLocalIp.equals(RpcAddress.LOCAL_HOST_ADDRESS)) {
                nonLocal = nonLocalIp;
                break;
            }
        }
        if (nonLocal != null) {
            RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://" + nonLocal + ":9010/client"));
            MockIoSession session = new MockIoSession();
            session.setRemoteAddress(RpcSocketAddress.fromFullAddress(nonLocal + ":9010"));
            session.setLocalAddress(RpcSocketAddress.fromFullAddress(nonLocal + ":9022"));
            HandshakeMessage message = new HandshakeMessage();
            message.getServers().add(RpcSocketAddress.fromFullAddress(nonLocal + ":9020"));
            message.getServers().add(RpcSocketAddress.fromFullAddress("localhost:9020"));
            client.onHandshakeMessageGot(session, message);
            assertTrue(client.getConfiguration().getServers().contains(
                    RpcSocketAddress.fromFullAddress(nonLocal + ":9010")));
            assertTrue(client.getConfiguration().getServers().contains(
                    RpcSocketAddress.fromFullAddress(nonLocal + ":9020")));
            assertFalse(client.getConfiguration().getServers().contains(
                    RpcSocketAddress.fromFullAddress("localhost:9010")));

        }
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client"));
        MockIoSession session = new MockIoSession();
        session.setRemoteAddress(RpcSocketAddress.fromFullAddress("localhost:9010"));
        session.setLocalAddress(RpcSocketAddress.fromFullAddress("localhost:8022"));
        HandshakeMessage message = new HandshakeMessage();
        message.getServers().add(RpcSocketAddress.fromFullAddress("localhost:9020"));
        message.getServers().add(RpcSocketAddress.fromFullAddress("localhost:9022"));
        client.onHandshakeMessageGot(session, message);
        assertTrue(client.getConfiguration().getServers().contains(
                RpcSocketAddress.fromFullAddress("localhost:9020")));
        assertTrue(client.getConfiguration().getServers().contains(
                RpcSocketAddress.fromFullAddress("localhost:9022")));

    }
}
