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
package com.sokeeper.persist.support;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.NodeOnlineStatus;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.persist.service.PersistedConfigurationService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class NodeOnlineStatusServiceImplTest extends BaseTestCase {
    @Autowired
    private NodeOnlineStatusService       nodeOnlineStatusService;

    @Autowired
    private PersistedConfigurationService persistedConfigurationService;

    @Test
    public void test_getCurrentTime() throws Throwable {
        assertNotNull(persistedConfigurationService.getCurrentTime());
    }

    @Test
    public void test_getNodeOnlineStatusTimerConfig() throws Throwable {
        PersistedConfiguration config = persistedConfigurationService.getPersistedConfiguration();
        assertNotNull(config);
        int sop = config.getSecondsOfPresenceTimer();
        int son = config.getSecondsOfNodeKeepAlive();
        assertTrue(config.getSecondsOfPresenceTimer() > 0);
        assertTrue(config.getSecondsOfNodeKeepAlive() > 0);
        config.setSecondsOfNodeKeepAlive(son * 2);
        config.setSecondsOfPresenceTimer(sop * 2);
        assertEquals(sop * 2, config.getSecondsOfPresenceTimer());
        assertEquals(son * 2, config.getSecondsOfNodeKeepAlive());
        assertNotNull(config.getSuffixOfServer());
        assertNotNull(config.getSecondsOfResourceChangesWatcherTimer() > 0);

    }

    @Test
    public void test_batchAddOrUpdateNodeStatusToOnline_parameters_check() throws Throwable {
        try {
            nodeOnlineStatusService.batchAddOrUpdateNodes(null, 0, false);
            fail("nodes can not be null.");
        } catch (IllegalArgumentException e) {
        }
        nodeOnlineStatusService.batchAddOrUpdateNodes(new HashSet<NodeOnlineStatus>(), 0, false);
        try {
            nodeOnlineStatusService.batchAddOrUpdateNodes(new HashSet<NodeOnlineStatus>(), -1,
                    false);
            fail("secondsOfNodeKeepAlive should >=0 ");
        } catch (IllegalArgumentException e) {
        }

        try {
            new NodeOnlineStatus().setServerAddress(null);
            fail("parentAddress could not be null");
        } catch (IllegalArgumentException e) {
        }
        HashSet<NodeOnlineStatus> nodes = new HashSet<NodeOnlineStatus>();
        for (int i = 0; i < 5; i++) {
            nodes.add(new NodeOnlineStatus());
        }
        try {
            nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, 5, false);
            fail("nodes[i].address can not be null.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void test_batchAddOrUpdateNodeStatusToOnline_parentNodes_withoutIdFillBack()
            throws Throwable {
        cleanupNodeOnlineStatusTable();
        // 2, query the configuration
        PersistedConfiguration config = persistedConfigurationService.getPersistedConfiguration();

        // 3, prepare records
        HashSet<NodeOnlineStatus> nodes = new HashSet<NodeOnlineStatus>();
        for (int i = 0; i < 2; i++) {
            NodeOnlineStatus node = new NodeOnlineStatus();
            node.setClientAddress("localhost:901" + i);
            nodes.add(node);
        }
        // 4, call the method
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                false);
        // 5, validate the result
        ResultSet resultSet = executeQuerySQL("select * from node_online_status where p_node_address='"
                + NodeOnlineStatus.NO_SERVER_ADDRESS
                + "' and  node_address in ('localhost:9010','localhost:9011')");
        assertNotNull(resultSet);
        int count = 0;
        while (resultSet.next()) {
            long keepAlive = resultSet.getTimestamp("gmt_expired").getTime()
                    - resultSet.getTimestamp("gmt_modified").getTime();
            assertEquals("the created record's expiredTime should=gmtModified+secondsOfKeepAlive",
                    keepAlive, config.getSecondsOfNodeKeepAlive() * 1000);
            assertEquals("if the record is new added, the gmt_create should = gmt_modified",
                    resultSet.getTimestamp("gmt_create").getTime(), resultSet.getTimestamp(
                            "gmt_modified").getTime());
            count++;
        }
        assertEquals(count, 2);
        // 6, now trigger the update for existed records
        Thread.sleep(1000);
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                false);
        resultSet = executeQuerySQL("select * from node_online_status where p_node_address='"
                + NodeOnlineStatus.NO_SERVER_ADDRESS
                + "' and  node_address in ('localhost:9010','localhost:9011')");
        assertNotNull(resultSet);
        count = 0;
        while (resultSet.next()) {
            long keepAlive = resultSet.getTimestamp("gmt_expired").getTime()
                    - resultSet.getTimestamp("gmt_modified").getTime();
            assertEquals("the created record's expiredTime should=gmtModified+secondsOfKeepAlive",
                    keepAlive, config.getSecondsOfNodeKeepAlive() * 1000);
            assertTrue("if the record is updated, the gmt_create should < gmt_modified", resultSet
                    .getTimestamp("gmt_create").getTime() < resultSet.getTimestamp("gmt_modified")
                    .getTime());
            count++;
        }
        assertEquals(count, 2);
    }

    @Test
    public void test_registerServerNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        try {
            nodeOnlineStatusService.registerParentNodes(null, 0);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010"), -1);
            fail();
        } catch (IllegalArgumentException e) {
        }
        nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010"), 10);
        assertEquals(nodeOnlineStatusService.listLiveParentNodes().size(), 1);
        assertEquals(nodeOnlineStatusService.removeNodeAndChildrenNodes(Arrays
                .asList("localhost:9010")), 1);
    }

    @Test
    public void test_registerChildNode() throws Throwable {
        cleanupNodeOnlineStatusTable();
        nodeOnlineStatusService.registerChildNode("localhost:8080", "localhost:8010", 10);
        assertEquals(nodeOnlineStatusService.listChildrenNodes("localhost:8010").size(), 1);
        assertEquals(nodeOnlineStatusService
                .unregisterChildNode("localhost:8080", "localhost:8010"), 1);
    }

    @Test
    public void test_batchAddOrUpdateNodeStatusToOnline_parentNodes_withIdFillBack()
            throws Throwable {
        cleanupNodeOnlineStatusTable();
        // 2, query the configuration
        PersistedConfiguration config = persistedConfigurationService.getPersistedConfiguration();

        // 3, prepare records
        HashSet<NodeOnlineStatus> nodes = new HashSet<NodeOnlineStatus>();
        for (int i = 0; i < 2; i++) {
            NodeOnlineStatus node = new NodeOnlineStatus();
            node.setClientAddress("localhost:901" + i);
            nodes.add(node);
        }
        // 4, call the method
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                true);
        for (NodeOnlineStatus node : nodes) {
            assertNotNull("the nodeId should be filled with the persistence layer's id", node
                    .getId());
            node.setId(null);
        }
        // 5, now trigger the update for existed records
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                true);
        for (NodeOnlineStatus node : nodes) {
            assertNotNull("the nodeId should be filled with the persistence layer's id", node
                    .getId());
            node.setId(null);
        }
    }

    @Test
    public void test_batchAddOrUpdateNodeStatusToOnline_childrenNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        // 1, query the configuration
        PersistedConfiguration config = persistedConfigurationService.getPersistedConfiguration();

        // 2, prepare parent node
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:9010')");
        ResultSet resultSet = executeQuerySQL("select id,p_node_address from node_online_status where p_node_address='"
                + NodeOnlineStatus.NO_SERVER_ADDRESS + "' limit 0,1");
        resultSet.next();
        Integer parentNodeId = resultSet.getInt("id");
        assertNotNull("parentNodeId can not be null.", parentNodeId);
        assertEquals(resultSet.getString("p_node_address"),
                NodeOnlineStatus.NO_SERVER_ADDRESS);

        // 3, prepare records
        HashSet<NodeOnlineStatus> nodes = new HashSet<NodeOnlineStatus>();
        for (int i = 0; i < 2; i++) {
            NodeOnlineStatus node = new NodeOnlineStatus();
            node.setClientAddress("localhost:801" + i);
            node.setServerAddress("localhost:9010");
            nodes.add(node);
        }
        // 4, call the method
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                true);
        for (NodeOnlineStatus node : nodes) {
            assertNotNull("the nodeId should be filled with the persistence layer's id", node
                    .getId());
            node.setId(null);
        }
        // 5, now trigger the update for existed records
        nodeOnlineStatusService.batchAddOrUpdateNodes(nodes, config.getSecondsOfNodeKeepAlive(),
                true);
        for (NodeOnlineStatus node : nodes) {
            assertNotNull("the nodeId should be filled with the persistence layer's id", node
                    .getId());
            node.setId(null);
        }
    }

    @Test
    public void test_listAllOnlineParentNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        // this node is the match result
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:9011')");
        // this node has expired
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval -10 second),'localhost:9012')");
        // this node is not parent node
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address,p_node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:9013','localhost:8010')");
        Collection<NodeOnlineStatus> parents = nodeOnlineStatusService.listLiveParentNodes();
        assertEquals(parents.size(), 1);
        NodeOnlineStatus node = parents.iterator().next();
        assertTrue(node.isParentNode());
        assertEquals(node.getClientAddress(), "localhost:9011");
        assertEquals(node.getGmtModified().getTime() + 20 * 1000, node.getGmtExpired().getTime());
        assertEquals(node.getGmtCreated().getTime() + 20 * 1000, node.getGmtExpired().getTime());
        assertEquals(nodeOnlineStatusService.listExpiredParentNodesAddresses().size(), 1);
    }

    @Test
    public void test_extendOnlineNodesLifeTime() throws Throwable {
        cleanupNodeOnlineStatusTable();

        try {
            nodeOnlineStatusService.extendNodesLifeTime(null, 0);
            fail();
        } catch (IllegalArgumentException e) {
        }

        Collection<String> pids = new HashSet<String>();
        try {
            nodeOnlineStatusService.extendNodesLifeTime(pids, -1);
            fail();
        } catch (IllegalArgumentException e) {
        }

        assertEquals(nodeOnlineStatusService.extendNodesLifeTime(pids, 20), 0);
        pids.add(null);
        try {
            nodeOnlineStatusService.extendNodesLifeTime(pids, 20);
            fail();
        } catch (IllegalArgumentException e) {
        }
        pids.clear();
        // 1, just the parent nodes
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:9010')");
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:9011')");
        ResultSet resultSet = executeQuerySQL("select node_address from node_online_status");
        while (resultSet.next()) {
            pids.add(resultSet.getString(1));
        }
        assertEquals(pids.size(), 2);
        assertEquals(nodeOnlineStatusService.extendNodesLifeTime(pids, 20), 2);
        assertEquals(nodeOnlineStatusService.extendNodesLifeTime(pids, 20), 2);
        assertEquals(nodeOnlineStatusService.listLiveParentNodesAddresses().size(), 2);

        NodeOnlineStatus pNode = nodeOnlineStatusService.listLiveParentNodes().iterator().next();
        assertEquals(
                "extendOnlineNodesLifeTime should not impact the gmtModified field, otherwise it will trigger node online event",
                pNode.getGmtCreated(), pNode.getGmtModified());
    }

    public void test_listOnlineChildrenNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:8010')");
        NodeOnlineStatus pNode = nodeOnlineStatusService.listLiveParentNodes().iterator().next();
        executeUpdateSQL("insert into node_online_status (gmt_create, gmt_modified, gmt_expired,node_address,p_node_address)values "
                + "(now(),now(),date_add(now(),interval 20 second),'localhost:8011','"
                + pNode.getClientAddress() + "')");
        assertEquals(nodeOnlineStatusService.listChildrenNodes(pNode.getClientAddress()).size(), 1);
    }

    public void test_getMasterNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010",
                "localhost:9011", "localhost:9012"), 20);
        executeUpdateSQL("update node_online_status set is_master='1'");
        assertEquals(
                nodeOnlineStatusService.getMasterNodes(Arrays.asList("localhost:9010")).size(), 2);
        assertTrue(nodeOnlineStatusService.getMasterNodes(null).contains("localhost:9012"));

    }

    public void test_registerMasterNodes() throws Throwable {
        cleanupNodeOnlineStatusTable();
        // no master nodes there
        nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010",
                "localhost:9011", "localhost:9012"), 20);
        assertFalse(nodeOnlineStatusService.listLiveParentNodes().toArray(new NodeOnlineStatus[0])[0]
                .getIsMaster());

        nodeOnlineStatusService.registerMasterNodes(Arrays.asList("localhost:9010",
                "localhost:9011"));
        // register again, with same address
        nodeOnlineStatusService.registerMasterNodes(Arrays.asList("localhost:9010",
                "localhost:9011"));
        // register again, with different address,since the previous master still alive, so the register should fail
        try {
            nodeOnlineStatusService.registerMasterNodes(Arrays.asList("localhost:9012"));
            fail("since still has alive master node there, this operation should fail.");
        } catch (IllegalStateException e) {
        }
        // if the master node is expired, the register should succeed
        executeUpdateSQL("delete from node_online_status");
        nodeOnlineStatusService.registerParentNodes(Arrays.asList("localhost:9010",
                "localhost:9011"), 20);
        nodeOnlineStatusService.registerMasterNodes(Arrays.asList("localhost:9010",
                "localhost:9011"));
        executeUpdateSQL("update node_online_status set gmt_expired=date_add(now(),interval -40 second) where node_address in ('localhost:9010','localhost:9011')");
        nodeOnlineStatusService.registerMasterNodes(Arrays.asList("localhost:9012"));
    }
}
