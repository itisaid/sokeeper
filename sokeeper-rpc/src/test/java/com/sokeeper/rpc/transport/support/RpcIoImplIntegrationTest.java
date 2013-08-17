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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected;
import com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets;
import com.sokeeper.rpc.exception.RpcRemoteException;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.service.support.RpcServiceBuilderImpl;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.RpcIoListener;
import com.sokeeper.rpc.transport.support.RpcClientIoHandlerImpl;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcIoImplIntegrationTest extends TestCase {
    final protected Logger         logger  = LoggerFactory.getLogger(getClass());

    private RpcServerIoHandlerImpl server;
    private RpcClientIoHandlerImpl client;
    private RpcServiceBuilderImpl  builder = new RpcServiceBuilderImpl();

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
        if (client != null) {
            client.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    public void test_addClientToGroups() throws Throwable {
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client"));
        RpcServerIoHandlerImpl server = new RpcServerIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client"));
        server.startup();
        client.startup();
        while (server.getSessions().size() < 1) {
            Thread.sleep(100);
        }
        IoSession session = server.getSessions().iterator().next();
        server.addClientToGroups(session, "a,b");
        server.addClientToGroups(session, "c");
        assertTrue(server.isClientInGroup(session, "a"));
        assertTrue(server.isClientInGroup(session, "b"));
        assertTrue(server.isClientInGroup(session, "c"));
        client.shutdown();
        server.shutdown();
    }

    public void test_call_clients_byGroupName() throws Throwable {
        final ConcurrentMap<String, String> messages = new ConcurrentHashMap<String, String>();
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[10];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client?groups=group" + (i / 4)));
            final int finalI = i;
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterface() {
                        public String sayHello(String msg) throws Throwable {
                            messages.put("" + finalI, "" + finalI);
                            return msg + finalI;
                        }

                        public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                            return null;
                        }
                    });
            clients[i].startup();
        }
        // 1, wait for all clients connected
        while (server.getSessions().size() < clients.length) {
            Thread.sleep(100);
        }
        assertEquals(server.getConnections().size(), clients.length);
        // 2, wait for all client join the group
        Set<IoSession> sessions = server.getSessions();
        while (true) {
            boolean continued = false;
            for (IoSession session : sessions) {
                if (!session.containsAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS)) {
                    continued = true;
                }
            }
            Thread.sleep(100);
            if (!continued)
                break;
        }
        // 3, call the group0
        RpcServiceBuilderImpl builder = new RpcServiceBuilderImpl();
        RpcIoImplIntegrationTestInterface callGroup0 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, null, "group0", server, false);
        RpcIoImplIntegrationTestInterface callGroup1 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, null, "group1", server, false);
        RpcIoImplIntegrationTestInterface callGroup2 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, null, "group2", server, false);

        callGroup0.sayHello("Hello");
        assertEquals(messages.size(), 4);//client 0,1,2,3
        assertEquals(messages.get("" + 0), "0");
        assertEquals(messages.get("" + 1), "1");
        assertEquals(messages.get("" + 2), "2");
        assertEquals(messages.get("" + 3), "3");
        messages.clear();

        callGroup1.sayHello("Hello");
        assertEquals(messages.size(), 4);//client 4,5,6,7
        assertEquals(messages.get("" + 4), "4");
        assertEquals(messages.get("" + 5), "5");
        assertEquals(messages.get("" + 6), "6");
        assertEquals(messages.get("" + 7), "7");
        messages.clear();

        callGroup2.sayHello("Hello");
        assertEquals(messages.size(), 2);//client 8,9
        assertEquals(messages.get("" + 8), "8");
        assertEquals(messages.get("" + 9), "9");
        messages.clear();

        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
        server.shutdown();
    }

    public void test_call_clients_byGroupName_and_targets() throws Throwable {
        final ConcurrentMap<String, String> messages = new ConcurrentHashMap<String, String>();
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();

        // 1, start the standalone client
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?groups=standaloneGroup"));
        client.registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                new RpcIoImplIntegrationTestInterface() {
                    public String sayHello(String msg) throws Throwable {
                        messages.put("client", "client");
                        return msg;
                    }

                    public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                        return null;
                    }

                });
        client.startup();

        while (server.getSessions().size() == 0) {
            Thread.sleep(100);
        }
        // 2, record the targets
        Set<RpcAddress> targets = new HashSet<RpcAddress>();
        targets.add(new RpcSocketAddress(((InetSocketAddress) server.getSessions().iterator()
                .next().getRemoteAddress())));

        // 3, start other clients
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[10];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client?groups=group" + (i / 4)));
            final int finalI = i;
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterface() {
                        public String sayHello(String msg) throws Throwable {
                            messages.put("" + finalI, "" + finalI);
                            return msg + finalI;
                        }

                        public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                            return null;
                        }

                    });
            clients[i].startup();
        }

        // 1, wait for all clients connected
        while (server.getSessions().size() < clients.length + 1) {
            Thread.sleep(100);
        }
        // 2, wait for all client join the group
        Set<IoSession> sessions = server.getSessions();
        while (true) {
            boolean continued = false;
            for (IoSession session : sessions) {
                if (!session.containsAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS)) {
                    continued = true;
                    break;
                }
            }
            Thread.sleep(100);
            if (!continued)
                break;
        }
        // 3, call the group0
        RpcServiceBuilderImpl builder = new RpcServiceBuilderImpl();
        RpcIoImplIntegrationTestInterface callGroup0 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, targets, "group0", server, false);
        RpcIoImplIntegrationTestInterface callGroup1 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, targets, "group1", server, false);
        RpcIoImplIntegrationTestInterface callGroup2 = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, targets, "group2", server, false);
        RpcIoImplIntegrationTestInterface callAll = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, null, null, server, false);

        callGroup0.sayHello("Hello");
        assertEquals(messages.size(), 5);//client 0,1,2,3
        assertEquals(messages.get("" + 0), "0");
        assertEquals(messages.get("" + 1), "1");
        assertEquals(messages.get("" + 2), "2");
        assertEquals(messages.get("" + 3), "3");
        assertEquals(messages.get("client"), "client");

        messages.clear();

        callGroup1.sayHello("Hello");
        assertEquals(messages.size(), 5);//client 4,5,6,7
        assertEquals(messages.get("" + 4), "4");
        assertEquals(messages.get("" + 5), "5");
        assertEquals(messages.get("" + 6), "6");
        assertEquals(messages.get("" + 7), "7");
        assertEquals(messages.get("client"), "client");
        messages.clear();

        callGroup2.sayHello("Hello");
        assertEquals(messages.size(), 3);//client 8,9
        assertEquals(messages.get("" + 8), "8");
        assertEquals(messages.get("" + 9), "9");
        assertEquals(messages.get("client"), "client");
        messages.clear();

        callAll.sayHello("Hello");
        assertEquals(messages.size(), 11);
        messages.clear();

        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
        server.shutdown();
    }

    public void test_client_connect_server() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/client"));
        server.startup();
        client.startup();
        assertEquals(client.getSessions().size(), 1);
    }

    public void test_client_auto_connect() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=1000"));
        client.startup();
        assertEquals(client.getSessions().size(), 0);
        server.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            logger.info("wait for client connected with server 1");
            Thread.sleep(1000);
        }
        assertEquals(client.getSessions().size(), 1);
        //once server shutdown,the client should able detect it.
        server.shutdown();
        while (client.isConnected(client.getConfiguration().getMainAddress())) {
            logger.info("wait for client disconnected from server 2");
            Thread.sleep(1000);
        }
        //when server startup again,client should able connect with it
        server.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            logger.info("wait for client connected with server 3");
            Thread.sleep(1000);
        }
        assertEquals(client.getSessions().size(), 1);
    }

    public void test_integrate_with_service_builder() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=10&timeout_ms=5000"));

        Set<RpcAddress> targets = new HashSet<RpcAddress>();
        targets.add(new RpcSocketAddress("localhost", 9010));

        RpcIoImplIntegrationTestInterface service = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, targets, null, client, false);

        // client not started,should report IoException
        try {
            service.sayHello("JamesFu");
            fail("RpcLocalExceptionIoTargetIsNotConnected");
        } catch (RpcLocalExceptionIoTargetIsNotConnected e) {
        }
        // client started,but server not started
        client.startup();
        try {
            service.sayHello("JamesFu");
            fail("RpcLocalExceptionIoTargetIsNotConnected");
        } catch (RpcLocalExceptionIoTargetIsNotConnected e) {
        }
        // server started but no registered service
        server.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        assertTrue(client.isConnected(client.getConfiguration().getMainAddress()));
        try {
            service.sayHello("JamesFu");
            fail("RpcRemoteException");
        } catch (RpcRemoteException e) {
            logger.error(e.getMessage());
        }
        // register service
        RpcIoImplIntegrationTestInterfaceImpl serviceProvider = new RpcIoImplIntegrationTestInterfaceImpl(
                null);
        server.registerRequestHandler(RpcIoImplIntegrationTestInterface.class, serviceProvider);
        assertEquals(service.sayHello("JamesFu"), "JamesFu");
        assertEquals(serviceProvider.connection.getLocalAddress(), new RpcSocketAddress(
                "localhost", 9010));

        serviceProvider.msg = new Exception("MyException");
        try {
            service.sayHello("JamesFu");
            fail("MyException");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "MyException");
        }
        serviceProvider.msg = null;

    }

    public void test_disconnectAll() throws Throwable {
        // 1 client 2 servers, client disconnect all servers
        RpcServerIoHandlerImpl[] servers = new RpcServerIoHandlerImpl[2];
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];

        int[] ports = new int[2];
        for (int i = 0; i < servers.length; i++) {
            ports[i] = NetUtils.selectAvailablePort(9010 + i);
            servers[i] = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:"
                    + ports[i] + "/server"));
            servers[i].startup();
        }
        clients[0] = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:" + ports[0]
                + "/client"));
        clients[0].getConfiguration().addServers("localhost:" + ports[1]);
        clients[0].getConfiguration().setConnectPolicy(RpcConfiguration.CONNECT_POLICY_ALL);
        if (clients[0].startup()) {
            while (clients[0].getConnections().size() < 2) {
                Thread.sleep(100);
            }
            assertEquals(clients[0].getConnections().size(), 2);
            clients[0].disconnectAll();
            while (servers[0].getConnections().size() > 0 || servers[1].getConnections().size() > 0) {
                Thread.sleep(100);
            }
            clients[0].shutdown();
        }
        // 1 server 2 clients, server disconnect all clients
        clients[0] = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:" + ports[0]
                + "/client"));
        clients[1] = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:" + ports[0]
                + "/client"));
        if (clients[0].startup() && clients[1].startup()) {
            while (servers[0].getConnections().size() < 2) {
                Thread.sleep(100);
            }
            servers[0].disconnectAll();
            while (servers[0].getConnections().size() > 0) {
                Thread.sleep(100);
            }
        }
        // shutdown
        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
        for (int i = 0; i < servers.length; i++) {
            servers[i].shutdown();
        }
    }

    public void test_server_call_clients_exceptions() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        RpcIoImplIntegrationTestInterface service = new RpcServiceBuilderImpl()
                .buildRemoteServiceProxy(RpcIoImplIntegrationTestInterface.class, null, null,
                        server, false);
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        Exception[] exceptions = new Exception[clients.length];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
            exceptions[i] = new Exception("" + i);
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterfaceImpl(exceptions[i]));
        }
        try {
            service.sayHello("Hello");
        } catch (RpcLocalExceptionMultipleTargets e) {
            assertEquals(e.getFailedTargets().length, 2);
        }
        for (RpcClientIoHandlerImpl myClient : clients) {
            myClient.shutdown();
        }
    }

    public void test_server_call_clients_results() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        RpcIoImplIntegrationTestInterface service = new RpcServiceBuilderImpl()
                .buildRemoteServiceProxy(RpcIoImplIntegrationTestInterface.class, null, null,
                        server, false);
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterfaceImpl(null));
        }
        while (server.getSessions().size() < clients.length) {
            Thread.sleep(100);
        }

        assertEquals(service.sayHello("Hello"), "Hello");
        for (RpcClientIoHandlerImpl myClient : clients) {
            myClient.shutdown();
        }
    }

    public void test_server_call_clients_argsFilterEnabled() throws Throwable {
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();

        Map<RpcAddress, String> messages = new HashMap<RpcAddress, String>();
        final Map<RpcAddress, Map<RpcAddress, String>> receivedMessages = new HashMap<RpcAddress, Map<RpcAddress, String>>();

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterfaceImpl(null) {
                        public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                            receivedMessages.put(RpcConnection.getCurrentRpcConnection()
                                    .getLocalAddress(), messages);
                            return null;
                        }
                    });
        }
        while (server.getConnections().size() < 2) {
            Thread.sleep(100);
        }
        for (RpcConnection connection : server.getConnections()) {
            messages.put(connection.getRemoteAddress(), connection.getRemoteAddress()
                    .getFullAddress());
        }
        RpcIoImplIntegrationTestInterface service = new RpcServiceBuilderImpl()
                .buildRemoteServiceProxy(RpcIoImplIntegrationTestInterface.class,
                        messages.keySet(), null, server, true);
        service.sayHello(messages);

        // client[0] will receive client[0]'s rpc address as result
        // client[1] will receive client[1]'s rpc address as result
        assertEquals(receivedMessages.size(), 2);
        RpcAddress client0 = messages.keySet().toArray(new RpcAddress[0])[0];
        RpcAddress client1 = messages.keySet().toArray(new RpcAddress[0])[1];

        assertTrue(receivedMessages.containsKey(client0));
        assertTrue(receivedMessages.containsKey(client1));
        assertEquals(receivedMessages.get(client0).size(), 1);
        assertEquals(receivedMessages.get(client1).size(), 1);
        assertEquals(receivedMessages.get(client0).values().iterator().next(), client0
                .getFullAddress());
        assertEquals(receivedMessages.get(client1).values().iterator().next(), client1
                .getFullAddress());

        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
        server.shutdown();
    }

    public void test_server_call_clients_result() throws Throwable {
        final Collection<RpcConnection> connections = new HashSet<RpcConnection>();
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();

        server.registerIoListener(new RpcIoListener() {
            public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
            }

            public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
                connections.add(connection);

            }
        });
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
            clients[i].registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                    new RpcIoImplIntegrationTestInterfaceImpl(null));
        }
        while (connections.size() < 2) {
            Thread.sleep(100);
        }
        Set<RpcAddress> targets = new HashSet<RpcAddress>();
        targets.add(connections.toArray(new RpcConnection[0])[0].getRemoteAddress());
        RpcIoImplIntegrationTestInterface service = new RpcServiceBuilderImpl()
                .buildRemoteServiceProxy(RpcIoImplIntegrationTestInterface.class, targets, null,
                        server, false);
        assertEquals(service.sayHello("Hello"), "Hello");
        for (RpcClientIoHandlerImpl myClient : clients) {
            myClient.shutdown();
        }
    }

    public void test_server_call_clients_oneFailure_onePass() throws Throwable {
        final Collection<RpcConnection> connections = new HashSet<RpcConnection>();
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();

        server.registerIoListener(new RpcIoListener() {
            public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
            }

            public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
                connections.add(connection);

            }
        });
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
            clients[i]
                    .registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                            new RpcIoImplIntegrationTestInterfaceImpl(
                                    i == 0 ? new Exception("" + i) : null));
        }
        while (connections.size() < 2) {
            Thread.sleep(100);
        }
        Set<RpcAddress> targets = new HashSet<RpcAddress>();
        targets.add(connections.toArray(new RpcConnection[0])[0].getRemoteAddress());
        targets.add(connections.toArray(new RpcConnection[0])[1].getRemoteAddress());

        RpcIoImplIntegrationTestInterface service = new RpcServiceBuilderImpl()
                .buildRemoteServiceProxy(RpcIoImplIntegrationTestInterface.class, targets, null,
                        server, false);
        try {
            service.sayHello("Hello");
        } catch (RpcLocalExceptionMultipleTargets e) {
            assertEquals(e.getFailedTargets().length, 1);
        }
        for (RpcClientIoHandlerImpl myClient : clients) {
            myClient.shutdown();
        }
    }

    public void test_disconnect_client() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
        }
        while (server.getSessions().size() < 2) {
            Thread.sleep(100);
        }
        IoSession session = server.getSessions().iterator().next();
        RpcConnection connection = new RpcConnection(new RpcSocketAddress(
                (InetSocketAddress) session.getLocalAddress()), new RpcSocketAddress(
                (InetSocketAddress) session.getRemoteAddress()));
        server.disconnect(connection.getRemoteAddress());
        while (server.getSessions().size() >= 2) {
            Thread.sleep(100);
        }
        assertEquals(server.getSessions().size(), 1);
        for (RpcClientIoHandlerImpl myClient : clients) {
            myClient.shutdown();
        }
    }

    public void test_server_maxConnections() throws Throwable {
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];

        server = new RpcServerIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/server?max_connections=" + clients.length));
        server.startup();

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client?timeout_ms=5000"));
            clients[i].startup();
        }
        while (server.getSessions().size() < 2) {
            Thread.sleep(100);
        }

        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?timeout_ms=5000"));
        client.startup();
        while (client.getSessions().size() == 1) {
            Thread.sleep(100);
        }
        assertFalse(client.isConnected(client.getConfiguration().getMainAddress()));
        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
    }

    public void test_heartbeat_server_disable_client_disable() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/client"));
        server.startup();
        client.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        assertTrue(client.isConnected(client.getConfiguration().getMainAddress()));
    }

    public void test_heartbeat_server_no_hb() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?hb_sec=4"));
        server.startup();
        client.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        IoSession session = client.getSessions().iterator().next();
        // wait until client got configuration updated from server
        while (session.getIdleTime(IdleStatus.READER_IDLE) != 0) {
            Thread.sleep(100);
        }
        client.setHeartBeat(session, 2);
        // wait until the client disconnect from server
        while (client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        assertFalse(client.isConnected(client.getConfiguration().getMainAddress()));
    }

    public void test_heartbeat_client_no_hb() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/server?hb_sec=2"));
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?hb_sec=4"));
        server.startup();
        client.startup();
        while (!client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        IoSession session = client.getSessions().iterator().next();
        // wait the server side pushed configuration
        while (session.getIdleTime(IdleStatus.WRITER_IDLE) != 2) {
            Thread.sleep(100);
        }
        // disable client side's heart beat
        client.setHeartBeat(session, 0);
        // wait until the server disconnect client
        while (client.isConnected(client.getConfiguration().getMainAddress())) {
            Thread.sleep(100);
        }
        assertFalse(client.isConnected(client.getConfiguration().getMainAddress()));
    }

    public void test_server_bind() throws Throwable {
        RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[2];

        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://0.0.0.0:9010/server"));
        server.startup();

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                    "tcp://localhost:9010/client"));
            clients[i].startup();
        }

        //assertEquals(server.getSessions().size(), 2);

        for (int i = 0; i < clients.length; i++) {
            clients[i].shutdown();
        }
    }

    public void test_integrate_with_service_performance_art() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        server.registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                new RpcIoImplIntegrationTestInterface() {
                    public String sayHello(String msg) throws Throwable {
                        return msg;
                    }

                    public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                        return null;
                    }

                });
        client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010/client?auto_reconnect_ms=10&timeout_ms=5000"));
        client.startup();
        RpcIoImplIntegrationTestInterface service = builder.buildRemoteServiceProxy(
                RpcIoImplIntegrationTestInterface.class, null, null, client, false);
        // test simple performance
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            service.sayHello("JamesFu");
        }
        long endTime = System.nanoTime();
        logger.info("1 client call sayHello 1000 times cost:" + (endTime - startTime) / (1000000)
                + " ms");

    }

    public void test_integrate_with_service_performance_tps() throws Throwable {
        server = new RpcServerIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server"));
        server.startup();
        server.registerRequestHandler(RpcIoImplIntegrationTestInterface.class,
                new RpcIoImplIntegrationTestInterface() {
                    public String sayHello(String msg) throws Throwable {
                        RpcConnection.getCurrentRpcConnection();
                        return msg;
                    }

                    public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
                        return null;
                    }

                });

        final int loopTimes = 10;
        final RpcClientIoHandlerImpl[] clients = new RpcClientIoHandlerImpl[50];
        final long[] costs = new long[clients.length];
        final AtomicInteger locks = new AtomicInteger(clients.length);
        final AtomicLong totalTimes = new AtomicLong(0);
        final RpcIoImplIntegrationTestInterface[] services = new RpcIoImplIntegrationTestInterface[clients.length];
        try {
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new RpcClientIoHandlerImpl(new RpcConfiguration(
                        "tcp://localhost:9010/client"));
                clients[i].startup();
                services[i] = builder.buildRemoteServiceProxy(
                        RpcIoImplIntegrationTestInterface.class, null, null, clients[i], false);
            }
            for (int i = 0; i < clients.length; i++) {
                final int finalI = i;
                new Thread(new Runnable() {
                    public void run() {

                        for (int loopTime = 0; loopTime < loopTimes; loopTime++) {
                            try {
                                long startTime = System.nanoTime();
                                services[finalI].sayHello("JamesFu");
                                long endTime = System.nanoTime();
                                if (loopTime != 0) {//ignore the first call
                                    costs[finalI] += (endTime - startTime);
                                    totalTimes.incrementAndGet();
                                }
                            } catch (Throwable e) {
                                logger.error(e.getMessage());
                            }
                        }
                        //costs[finalI] /= 1000000;

                        locks.decrementAndGet();
                    }
                }).start();
            }
            while (locks.get() > 0) {
                Thread.sleep(1000);
            }
            long total_costs = 0L;
            for (long cost : costs) {
                total_costs += cost;
            }
            float art = total_costs / (totalTimes.get());
            float tps = (1000 * 1000000 / art) * clients.length;
            logger.info("total times:" + totalTimes.get() + " total costs:" + total_costs
                    + "ns art:" + art + "ns,tps:" + tps);

        } finally {
            for (RpcClientIoHandlerImpl client : clients) {
                if (client != null) {
                    try {
                        client.shutdown();
                    } catch (Throwable e) {
                    }
                }
            }
        }
    }
}
