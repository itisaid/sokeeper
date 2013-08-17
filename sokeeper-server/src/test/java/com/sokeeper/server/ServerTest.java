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
package com.sokeeper.server;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.persist.service.PersistedConfigurationService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.transport.support.RpcClientIoHandlerImpl;
import com.sokeeper.server.Server;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ServerTest extends BaseTestCase {
    @Autowired
    private ResourceService               resourceService;

    @Autowired
    private SubscriberService             subscriberService;
    @Autowired
    private ChangesService                changesService;
    @Autowired
    private NodeOnlineStatusService       nodeOnlineStatusService;
    @Autowired
    private PersistedConfigurationService persistedConfigurationService;

    public void test_startup() throws Throwable {

        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();

        Server server = new Server();
        initialize(server, NetUtils.selectAvailablePort(9010), true, 1, 5);
        server.startup();
        assertNotNull(server.getServerRpcConfiguration());
        assertEquals(server.getServerRpcConfiguration().getMainAddress().getFullAddresses(true)
                .size(), nodeOnlineStatusService.listLiveParentNodes().size());
        assertNotNull(server.getResourcesCache());
        assertNotNull(server.getResourceHandler());
        server.shutdown();

        server = new Server();
        server.setNodeOnlineStatusService(nodeOnlineStatusService);
        server.setPersistedConfigurationService(persistedConfigurationService);
        server.setChangesService(changesService);
        server.setSubscriberService(subscriberService);
        server.setServerPort(NetUtils.selectAvailablePort(9010));
        server.setEnableLocalHost(false);
        server.setResourceService(resourceService);
        server.startup();
        server.shutdown();

    }

    public void test_two_servers() throws Throwable {

        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();

        Server server1 = new Server();
        Server server2 = new Server();

        initialize(server1, NetUtils.selectAvailablePort(9010), true, 1, 5);
        initialize(server2, NetUtils.selectAvailablePort(9011), true, 1, 5);
        server1.startup();
        server2.startup();
        server1.shutdown();
        server2.shutdown();
    }

    public void test_servers_will_treat_each_as_white_list() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();

        Server server1 = new Server();
        Server server2 = new Server();

        initialize(server1, NetUtils.selectAvailablePort(9010), true, 1, 5);
        initialize(server2, NetUtils.selectAvailablePort(9011), true, 1, 5);

        {
            String[] addrs = NetUtils.getAllLocalAddresses().toArray(new String[0]);
            if (addrs.length >= 2) {
                server1.setServerIp(addrs[0]);
                server2.setServerIp(addrs[1]);

                server1.startup();
                server2.startup();
                while (!server1.getServerRpcConfiguration().isIpInWhiteList(addrs[1])) {
                    Thread.sleep(100);
                }
                while (!server2.getServerRpcConfiguration().isIpInWhiteList(addrs[0])) {
                    Thread.sleep(100);
                }
                server1.shutdown();
                server2.shutdown();
            }
        }
    }

    public void test_three_servers() throws Throwable {

        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();

        Server server1 = new Server();
        Server server2 = new Server();
        Server server3 = new Server();

        initialize(server1, NetUtils.selectAvailablePort(9010), true, 1, 5);
        initialize(server2, NetUtils.selectAvailablePort(9011), true, 1, 5);
        initialize(server3, NetUtils.selectAvailablePort(9012), true, 1, 5);

        server1.startup();
        server2.startup();
        server3.startup();
        server1.shutdown();
        server2.shutdown();
        server3.shutdown();
    }

    public void test_processOnlineServersChanged_newServersJoined_client_will_receive_it()
            throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();

        Server server1 = new Server();
        Server server2 = new Server();
        initialize(server1, NetUtils.selectAvailablePort(9010), true, 1, 5);
        initialize(server2, NetUtils.selectAvailablePort(9011), true, 1, 5);
        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:" + server1.getServerPort()));
        server1.startup();
        client.startup();
        server2.startup();
        int serverTotal = nodeOnlineStatusService.listLiveParentNodes().size();
        while (client.getConfiguration().getServers().size() < serverTotal) {
            Thread.sleep(100);
        }
        server1.shutdown();
        server2.shutdown();
        client.shutdown();
    }

    public void test_expired_server_will_be_cleaned_up_by_master_server() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();
        resetResourceChangesSequence();
        // prepare masterNode and slaveNode
        // final Collection<String> removedLocalServers = new HashSet<String>();
        Server masterNode = new Server();
        Server slaveNode = new Server();
        int masterPort = NetUtils.selectAvailablePort(9010);
        int slavePort = NetUtils.selectAvailablePort(9011);
        initialize(masterNode, masterPort, true, 1, 5);
        // let the slave server update it's life time after 20 seconds, with this approach the master will trade the slave expired and will cleanup it
        initialize(slaveNode, slavePort, true, 3, 1);
        masterNode.startup();
        while (nodeOnlineStatusService.getMasterNodes(null).size() == 0) {
            Thread.sleep(100);
        }
        slaveNode.startup();
        while (nodeOnlineStatusService.listLiveParentNodesAddresses().contains(
                "127.0.0.1:" + slavePort)) {
            Thread.sleep(100);
        }
        masterNode.shutdown();
        slaveNode.shutdown();
    }

    public void test_when_master_node_changed_receiver_will_auto_switch_to_new_master()
            throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();
        resetResourceChangesSequence();
        // prepare servers
        Server[] servers = new Server[3];
        int[] ports = new int[servers.length];
        for (int i = 0; i < servers.length; i++) {
            ports[i] = NetUtils.selectAvailablePort(9010 + i);
            servers[i] = new Server();
            initialize(servers[i], ports[i], true, 1, 5);
        }
        // startup servers
        servers[0].startup();
        while (nodeOnlineStatusService.getMasterNodes(null).size() == 0) {
            Thread.sleep(100);
        }
        assertTrue("servers[0] should becomes the masterNode", nodeOnlineStatusService
                .getMasterNodes(null).contains("127.0.0.1:" + ports[0]));
        servers[1].startup();
        servers[2].startup();
        // now shutdown server[0],the receiverIoHandler on servers[1],servers[2] should changes
        servers[0].shutdown();
        // wait until other servers registered as master node
        while (nodeOnlineStatusService.getMasterNodes(null).contains("127.0.0.1:" + ports[0])) {
            Thread.sleep(100);
        }
        while (nodeOnlineStatusService.getMasterNodes(null).size() == 0) {
            Thread.sleep(100);
        }
        servers[1].shutdown();
        servers[2].shutdown();
    }

    public void test_when_server_was_removed_by_other_server_it_will_register_itself_again()
            throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();
        resetResourceChangesSequence();
        // 1, prepare server
        Server server = new Server();
        int port = NetUtils.selectAvailablePort(9010);
        initialize(server, port, true, 1, 5);
        server.startup();
        Collection<String> addrs = server.getServerRpcConfiguration().getMainAddress()
                .getFullAddresses(true);
        // 2, at this point,the server should registered itself to node_online_status
        assertEquals(nodeOnlineStatusService.listLiveParentNodes().size(), addrs.size());
        // 3, now remove the server from node_online_status
        nodeOnlineStatusService.removeNodeAndChildrenNodes(addrs);
        // 4, wait for a while, the server should register it again
        while (nodeOnlineStatusService.listLiveParentNodes().size() < addrs.size()) {
            Thread.sleep(100);
        }
        // 5, shutdown the server
        server.shutdown();
    }

    public void test_when_master_died_other_server_will_take_place_of_master() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();
        resetResourceChangesSequence();
        // 1, prepare server1 and server2
        Server server1 = new Server();
        int port1 = NetUtils.selectAvailablePort(9010);
        Server server2 = new Server();
        int port2 = NetUtils.selectAvailablePort(9011);
        initialize(server1, port1, true, 1, 5);
        initialize(server2, port2, true, 1, 5);
        server1.startup();
        // wait until server1 registered as masterNode
        while (nodeOnlineStatusService.getMasterNodes(null).size() == 0) {
            Thread.sleep(100);
        }
        assertTrue("when server1 started,it should register himself as masterNode",
                nodeOnlineStatusService.getMasterNodes(null).contains("127.0.0.1:" + port1));
        // startup server2,and stop server1, after a while server2 should becomes masterNode
        server2.startup();
        server1.shutdown();
        while (nodeOnlineStatusService.getMasterNodes(null).contains("127.0.0.1:" + port1)) {
            Thread.sleep(100);
        }
        while (nodeOnlineStatusService.getMasterNodes(null).size() == 0) {
            Thread.sleep(100);
        }
        assertTrue("when server1 stopped,server2 should becomes the masterNode",
                nodeOnlineStatusService.getMasterNodes(null).contains("127.0.0.1:" + port2));
        server2.shutdown();
    }

    public void test_when_new_server_up_client_can_detect_it() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceChangesTable();
        resetResourceChangesSequence();
        cleanupResourceTypes();
        registerResourceType("t1", true);

        // 1, prepare server1 and server2
        Server server1 = new Server();
        int port1 = NetUtils.selectAvailablePort(9010);
        Server server2 = new Server();
        int port2 = NetUtils.selectAvailablePort(9011);
        initialize(server1, port1, true, 1, 5);
        initialize(server2, port2, true, 1, 5);
        server1.startup();

        RpcClientIoHandlerImpl client = new RpcClientIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:" + port1));
        client.startup();
        server2.startup();
        // when new server started,the client should got new server's address
        while (!client.getConfiguration().getServers().contains(
                RpcSocketAddress.fromFullAddress("localhost:" + port2))) {
            Thread.sleep(100);
        }
        while (!client.isAlive()) {
            Thread.sleep(100);
        }

        String clientAddress = client.getConnections().iterator().next().getLocalAddress()
                .getFullAddress();
        String serverAddress = client.getConnections().iterator().next().getRemoteAddress()
                .getFullAddress();
        // when client connected with server,client address should be recorded to node_online_status table.
        while (executeCountSQL("select count(*) from node_online_status where node_address='"
                + clientAddress + "'") != 1) {
            Thread.sleep(100);
        }
        // when client dropped,it should be removed from node_online_status
        {
            // prepare resource changes which performed by the client
            changesService.addOrUpdateResourceCreatedChanges("t1", "r1", clientAddress,
                    serverAddress);
            assertEquals(
                    executeCountSQL("select count(*) from resource_changes where changes='CREATED' and  client_address='"
                            + clientAddress + "'"), 1);
        }
        {
            // prepare subscriber list for the client
            ChangesSubscriber subscriber = new ChangesSubscriber();
            subscriber.setClientAddress(clientAddress);
            subscriber.setServerAddress(serverAddress);
            subscriber.setResourceType("t1");
            subscriber.setResourceId("r1");
            subscriberService.addResourceChangesSubscriber(subscriber);
            assertEquals(subscriberService.getSubscribedResources(clientAddress, serverAddress)
                    .size(), 1);
        }

        client.shutdown();
        while (executeCountSQL("select count(*) from node_online_status where node_address='"
                + clientAddress + "'") > 0) {
            Thread.sleep(100);
        }
        // the resources which created by it will be updated to owner DIED
        assertEquals(
                executeCountSQL("select count(*) from resource_changes where changes='DIED' and client_address='"
                        + clientAddress + "'"), 1);
        // the subscriber list will be cleanup
        assertEquals(subscriberService.getSubscribedResources(clientAddress, serverAddress).size(),
                0);

        server1.shutdown();
        server2.shutdown();
    }

    public void initialize(Server server, int serverPort, boolean enableLocalHost,
                           int secondsOfTimerPeriod, int secondsOfNodeKeepAlive) {
        server.setNodeOnlineStatusService(nodeOnlineStatusService);
        server.setPersistedConfigurationService(persistedConfigurationService);
        server.setChangesService(changesService);
        server.setSubscriberService(subscriberService);
        server.setResourceService(resourceService);
        server.setServerPort(serverPort);
        server.setEnableLocalHost(enableLocalHost);
        server.setPersistedConfiguration(persistedConfigurationService.getPersistedConfiguration());
        server.getPersistedConfiguration().setSecondsOfPresenceTimer(secondsOfTimerPeriod);
        server.getPersistedConfiguration().setSecondsOfNodeKeepAlive(secondsOfNodeKeepAlive);
    }

}
