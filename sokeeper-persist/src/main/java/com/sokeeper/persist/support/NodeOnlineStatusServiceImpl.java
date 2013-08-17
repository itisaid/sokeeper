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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sokeeper.domain.NodeOnlineStatus;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("nodeOnlineStatusService")
public class NodeOnlineStatusServiceImpl extends SqlMapClientDaoSupport implements
        NodeOnlineStatusService {
    final protected Logger logger = LoggerFactory.getLogger(getClass());

    @Transactional
    public void registerParentNodes(Collection<String> serverAddressList, int secondsOfNodeKeepAlive)
            throws IllegalArgumentException {
        Assert.notNull(serverAddressList, "serverAddressList can not be null.");
        Collection<NodeOnlineStatus> servers = new HashSet<NodeOnlineStatus>();
        for (String addr : serverAddressList) {
            NodeOnlineStatus serverNode = new NodeOnlineStatus();
            serverNode.setClientAddress(addr);
            serverNode.setServerAddress(NodeOnlineStatus.NO_SERVER_ADDRESS);
            servers.add(serverNode);
        }
        batchAddOrUpdateNodes(servers, secondsOfNodeKeepAlive, false);
    }

    @Transactional
    public void registerMasterNodes(Collection<String> serverAddressList)
            throws IllegalArgumentException, IllegalStateException {
        Assert.notEmpty(serverAddressList, "serverAddressList can not be null.");
        getSqlMapClientTemplate().update("presence.unregisterExpiredMasterNodes");
        getSqlMapClientTemplate().update("presence.registerMasterNodes",
                serverAddressList.toArray(new String[0]));
        if (getMasterNodes(serverAddressList).size() > 0) {
            throw new IllegalStateException("can not register duplicated master nodes.");
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getMasterNodes(Collection<String> nodesExcluded)
            throws IllegalArgumentException {
        if (nodesExcluded == null) {
            nodesExcluded = Collections.emptyList();
        }
        return getSqlMapClientTemplate().queryForList("presence.getMasterNodes",
                nodesExcluded.toArray(new String[0]));
    }

    @Transactional
    public void registerChildNode(String childAddress, String parentAddress,
                                  int secondsOfNodeKeepAlive) throws IllegalArgumentException {
        NodeOnlineStatus node = new NodeOnlineStatus();
        node.setClientAddress(childAddress);
        node.setServerAddress(parentAddress);
        addOrUpdateNode(node, secondsOfNodeKeepAlive, false);
    }

    @Transactional
    public int unregisterChildNode(String childAddress, String parentAddress)
            throws IllegalArgumentException {
        Assert.hasText(childAddress, "childAddress can not be empty.");
        Assert.hasText(parentAddress, "parentAddress can not be empty.");
        NodeOnlineStatus node = new NodeOnlineStatus();
        node.setClientAddress(childAddress);
        node.setServerAddress(parentAddress);
        return getSqlMapClientTemplate().delete("presence.unregisterChildNode", node);
    }

    @Transactional
    public void addOrUpdateNode(NodeOnlineStatus node, int secondsOfNodeKeepAlive,
                                boolean fillbackNodeId) throws IllegalArgumentException {
        Assert.notNull(node, "node can not be null.");
        Assert.isTrue(secondsOfNodeKeepAlive >= 0, "secondsOfNodeKeepAlive should >=0");
        Assert.hasText(node.getClientAddress(), "node.address can not be empty");
        NodeOnlineStatusDTO nodeDTO = new NodeOnlineStatusDTO();
        nodeDTO.setNodeOnlineStatus(node);
        nodeDTO.setSecondsOfNodeKeepAlive(secondsOfNodeKeepAlive);
        if (fillbackNodeId) {
            Long nodeId = (Long) getSqlMapClientTemplate().insert("presence.addOrUpdateNodeReturn",
                    nodeDTO);
            nodeDTO.getNodeOnlineStatus().setId(nodeId);
        } else {
            getSqlMapClientTemplate().insert("presence.addOrUpdateNodeVoid", nodeDTO);
        }
    }

    @Transactional
    public void batchAddOrUpdateNodes(Collection<NodeOnlineStatus> nodes,
                                      int secondsOfNodeKeepAlive, boolean fillbackNodeId)
            throws IllegalArgumentException {
        Assert.notNull(nodes, "nodes can not be null.");
        Assert.isTrue(secondsOfNodeKeepAlive >= 0, "secondsOfNodeKeepAlive should >=0");
        if (nodes.size() > 0) {
            for (NodeOnlineStatus node : nodes) {
                addOrUpdateNode(node, secondsOfNodeKeepAlive, fillbackNodeId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<NodeOnlineStatus> listLiveParentNodes() {
        return getSqlMapClientTemplate().queryForList("presence.listLiveParentNodes");
    }

    @SuppressWarnings("unchecked")
    public Collection<String> listExpiredParentNodesAddresses() {
        return getSqlMapClientTemplate().queryForList("presence.listExpiredParentNodesAddresses");
    }

    @SuppressWarnings("unchecked")
    public Collection<String> listLiveParentNodesAddresses() {
        return getSqlMapClientTemplate().queryForList("presence.listLiveParentNodesAddresses");
    }

    public int removeNodeAndChildrenNodes(Collection<String> parentNodeAddressList)
            throws IllegalArgumentException {
        Assert.notNull(parentNodeAddressList, "parentNodeAddressList can not be null.");
        for (String pNodeAddr : parentNodeAddressList) {
            Assert.notNull(pNodeAddr, "pNodeAddr can not be null");
        }
        int impacted = 0;
        if (parentNodeAddressList.size() != 0) {
            impacted = getSqlMapClientTemplate().delete("presence.removeNodeAndChildrenNodes",
                    parentNodeAddressList.toArray(new String[0]));
        }
        return impacted;
    }

    @Transactional
    public int extendNodesLifeTime(Collection<String> parentNodeAddressList,
                                   int secondsOfNodeKeepAlive) throws IllegalArgumentException {
        Assert.notNull(parentNodeAddressList, "parentNodeAddressList can not be null.");
        Assert.isTrue(secondsOfNodeKeepAlive >= 0, "secondsOfNodeKeepAlive should >=0");
        for (String parentAddress : parentNodeAddressList) {
            Assert.notNull(parentAddress, "id can not be null");
        }
        int impacted = 0;
        if (parentNodeAddressList.size() != 0) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("secondsOfNodeKeepAlive", secondsOfNodeKeepAlive);
            map.put("parentNodeAddressList", parentNodeAddressList.toArray(new String[0]));
            impacted = getSqlMapClientTemplate().update("presence.extendNodesLifeTime", map);
        }
        return impacted;
    }

    @SuppressWarnings("unchecked")
    public Collection<NodeOnlineStatus> listChildrenNodes(String parentAddress)
            throws IllegalArgumentException {
        Assert.hasText(parentAddress, "parentAddress can not be empty");
        return getSqlMapClientTemplate().queryForList("presence.listChildrenNodes", parentAddress);
    }

}
