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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.NodeOnlineStatus;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.persist.support.NodeOnlineStatusServiceImpl;
import com.sokeeper.server.NodePresenceTimer;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class NodePresenceTimerTest extends BaseTestCase {

    @Autowired
    private NodeOnlineStatusService nodeOnlineStatusService;

    @Autowired
    private ChangesService          changesService;

    @Autowired
    private SubscriberService       subscriberService;
    private NodePresenceTimer       serverPresenceTimer;

    public void onSetUp() {
        super.onSetUp();
        serverPresenceTimer = new NodePresenceTimer();
    }

    public void onTearDown() {
        super.onTearDown();
        if (serverPresenceTimer != null) {
            serverPresenceTimer.shutdown();
        }
    }

    public void test_basicParametersCheck() throws Throwable {
        try {
            serverPresenceTimer.setLocalServers(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            serverPresenceTimer.setLocalServers(Arrays.asList(new String[] {}));
            fail();
        } catch (IllegalArgumentException e) {
        }
        serverPresenceTimer.setLocalServers(Arrays.asList("localhost:9010", "localhost:9011"));
        serverPresenceTimer.setChangesService(changesService);
        serverPresenceTimer.setSubscriberService(subscriberService);
        assertTrue(serverPresenceTimer.getLocalServers().contains("localhost:9010"));
        assertTrue(serverPresenceTimer.getLocalServers().contains("localhost:9011"));

        try {
            serverPresenceTimer.startup();
            fail();
        } catch (IllegalArgumentException e) {
        }
        serverPresenceTimer.setNodeOnlineStatusService(nodeOnlineStatusService);

        serverPresenceTimer.setNodeOnlineStatusService(nodeOnlineStatusService);
        serverPresenceTimer.setPersistedConfiguration(new PersistedConfiguration());
        serverPresenceTimer.startup();

    }

    public void test_startup_registerMasterNodes_shutdown_turnToOffline() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceSubscribeListTable();
        cleanupResourceTypes();
        registerResourceType("scm.configuration", true);

        assertEquals(nodeOnlineStatusService.listLiveParentNodes().size(), 0);
        initialize(serverPresenceTimer, Arrays.asList("localhost:9010", "localhost:9011"));

        serverPresenceTimer.startup();
        assertEquals("the local servers should be registered", nodeOnlineStatusService
                .listLiveParentNodes().size(), 2);

        nodeOnlineStatusService.registerChildNode("localhost:8010", "localhost:9010", 10);
        nodeOnlineStatusService.registerChildNode("localhost:8011", "localhost:9011", 10);

        assertEquals("the child node should be registered also", nodeOnlineStatusService
                .listChildrenNodes("localhost:9010").size(), 1);
        assertEquals("the child node should be registered also", nodeOnlineStatusService
                .listChildrenNodes("localhost:9011").size(), 1);

        ChangesSubscriber resourceChangesSubscriber = new ChangesSubscriber();
        resourceChangesSubscriber.setResourceType("scm.configuration");
        resourceChangesSubscriber.setResourceId("1");
        resourceChangesSubscriber.setServerAddress("localhost:9010");
        resourceChangesSubscriber.setClientAddress("localhost:8010");
        subscriberService.addResourceChangesSubscriber(resourceChangesSubscriber);
        assertEquals(subscriberService.getSubscribedResources("localhost:8010", "localhost:9010")
                .size(), 1);

        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "1",
                "localhost:8010", "localhost:9010");
        assertEquals(
                executeCountSQL("select count(*) from resource_changes where resource_type='scm.configuration'"),
                1);

        serverPresenceTimer.shutdown();
        while (nodeOnlineStatusService.listLiveParentNodes().size() != 0) {
            Thread.sleep(100);
        }
        assertEquals("the server node will be updated to offline", nodeOnlineStatusService
                .listLiveParentNodes().size(), 0);
        assertEquals("the child node will be updated to offline", nodeOnlineStatusService
                .listChildrenNodes("localhost:9010").size(), 0);
        assertEquals("the child node will be updated to offline", nodeOnlineStatusService
                .listChildrenNodes("localhost:9011").size(), 0);
        while (subscriberService.getSubscribedResources("localhost:8010", "localhost:9010").size() != 0) {
            Thread.sleep(100);
        }
        assertEquals("the subscribe list will be cleanup", subscriberService
                .getSubscribedResources("localhost:8010", "localhost:9010").size(), 0);
        assertEquals(executeCountSQL("select count(*) from resource_changes"), 1);
        while (executeCountSQL("select count(*) from resource_changes where changes='DIED'") != 1) {
            Thread.sleep(100);
        }
    }

    public void test_the_master_node_will_cleanup_expired_online_server_nodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        // 1, prepare expired online server nodes
        nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010",
                "localhost:9011"), 100);
        executeUpdateSQL("update node_online_status set gmt_expired=date_add(now(),interval -1 second)");
        assertEquals(nodeOnlineStatusService.listExpiredParentNodesAddresses().size(), 2);
        // 2, prepare the presenceTimer
        NodePresenceTimer presenceTimer1 = new NodePresenceTimer();
        initialize(presenceTimer1, Arrays.asList("localhost:9012"));
        presenceTimer1.startup();
        while (nodeOnlineStatusService.listExpiredParentNodesAddresses().size() > 0) {
            Thread.sleep(100);
        }
        assertEquals(
                "the expired server nodes will be removed by master node",
                executeCountSQL("select count(*) from node_online_status where node_address='localhost:9010' or node_address='localhost:9011'"),
                0);
        presenceTimer1.shutdown();
    }

    public void test_when_registerMasterNodes_failed() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();

        NodePresenceTimer presenceTimer1 = new NodePresenceTimer() {
            protected void cleanup(Collection<String> servers) throws IllegalArgumentException {
            }
        };
        initialize(presenceTimer1, Arrays.asList("localhost:9012"));
        presenceTimer1.getPersistedConfiguration().setSecondsOfPresenceTimer(5);
        presenceTimer1.getPersistedConfiguration().setSecondsOfNodeKeepAlive(20);
        presenceTimer1.setNodeOnlineStatusService(new NodeOnlineStatusServiceImpl() {
            public void registerParentNodes(Collection<String> parentAddressList,
                                            int secondsOfNodeKeepAlive)
                    throws IllegalArgumentException {
            }

            public int extendNodesLifeTime(Collection<String> parentNodeAddressList,
                                           int secondsOfNodeKeepAlive)
                    throws IllegalArgumentException {
                return 1;
            }

            public void registerMasterNodes(Collection<String> serverAddressList)
                    throws IllegalArgumentException, IllegalStateException {
                throw new IllegalStateException("illegal state");
            }

            public Collection<String> getMasterNodes(Collection<String> nodesExcluded) {
                return Arrays.asList(new String[] {});
            }

            public Collection<String> listExpiredParentNodesAddresses() {
                return Arrays.asList(new String[] {});
            }

            public Collection<NodeOnlineStatus> listLiveParentNodes() {
                return Arrays.asList(new NodeOnlineStatus[] {});
            }
        });
        presenceTimer1.startup();

        while (presenceTimer1.getMasterServers() == null) {
            Thread.sleep(100);
        }

        assertEquals(presenceTimer1.getMasterServers().size(), 0);
        presenceTimer1.shutdown();
    }

    public void test_when_server_been_removed_by_others_it_can_process_it() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();

        final Collection<String> removedLocalNodes = new HashSet<String>();
        NodePresenceTimer presenceTimer1 = new NodePresenceTimer() {

            public void onLocalServersRemoved(Collection<String> serverNodes) {
                removedLocalNodes.addAll(serverNodes);
            }
        };
        initialize(presenceTimer1, Arrays.asList("localhost:9010"));

        presenceTimer1.startup();

        cleanupNodeOnlineStatusTable();
        while (removedLocalNodes.size() == 0) {
            Thread.sleep(100);
        }
        presenceTimer1.shutdown();
        assertTrue(removedLocalNodes.contains("localhost:9010"));
    }

    public void test_two_when_master_stopped_slave_will_becomes_master() throws Throwable {
        cleanupNodeOnlineStatusTable();
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();

        NodePresenceTimer presenceTimer1 = new NodePresenceTimer();
        NodePresenceTimer presenceTimer2 = new NodePresenceTimer();
        initialize(presenceTimer1, Arrays.asList("localhost:9010"));
        initialize(presenceTimer2, Arrays.asList("localhost:9011"));

        presenceTimer1.startup();

        while (presenceTimer1.getMasterServers() == null) {
            Thread.sleep(100);
        }

        assertEquals(presenceTimer1.getMasterServers().size(), 1);
        assertTrue(presenceTimer1.getMasterServers().contains("localhost:9010"));

        presenceTimer2.startup();
        while (presenceTimer2.getMasterServers() == null) {
            Thread.sleep(100);
        }

        presenceTimer1.shutdown();
        while (!presenceTimer2.getMasterServers().contains("localhost:9011")) {
            Thread.sleep(100);
        }
        assertEquals(presenceTimer2.getMasterServers().size(), 1);
        assertTrue(presenceTimer2.getMasterServers().contains("localhost:9011"));
        presenceTimer2.shutdown();
    }

    private void initialize(NodePresenceTimer serverPresenceTimer, Collection<String> servers)
            throws IllegalArgumentException {
        serverPresenceTimer.setLocalServers(servers);
        serverPresenceTimer.setChangesService(changesService);
        serverPresenceTimer.setSubscriberService(subscriberService);
        serverPresenceTimer.setPersistedConfiguration(new PersistedConfiguration());
        serverPresenceTimer.setNodeOnlineStatusService(nodeOnlineStatusService);
        serverPresenceTimer.getPersistedConfiguration().setSecondsOfNodeKeepAlive(10);
        serverPresenceTimer.getPersistedConfiguration().setSecondsOfPresenceTimer(1);
    }
}
