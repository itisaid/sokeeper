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
package com.sokeeper.persist.service;

import java.util.Collection;

import com.sokeeper.domain.NodeOnlineStatus;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface NodeOnlineStatusService {

    /**
     * Register the given servers as server nodes into node_online_status
     * table.This method is a wrap for
     * {@link #batchAddOrUpdateNodes(Collection, int, boolean)}
     *
     * @param parentAddressList: parent address list can not be null but could
     *            be empty when its empty will do nothing.
     * @param secondsOfNodeKeepAlive: how long this registered node will be
     *            expired.
     * @throws IllegalArgumentException
     */
    public void registerParentNodes(Collection<String> parentAddressList, int secondsOfNodeKeepAlive)
            throws IllegalArgumentException;

    /**
     * Register the given server nodes as master nodes, only when the
     * persistenceLayer don't have other alive master nodes this operation can
     * be execution succeed.
     *
     * @param serverAddressList: the candidate master server's address list can
     *            not be null otherwise throw {@link IllegalArgumentException}.
     * @throws IllegalArgumentException
     * @throws IllegalStateException: if the register failed will throw this
     *             exception.
     */
    public void registerMasterNodes(Collection<String> serverAddressList)
            throws IllegalArgumentException, IllegalStateException;

    /**
     * Get validated master nodes from persistenceLayer.
     *
     * @param nodesExcluded:the server address list which we don't want included
     *            in the result,could be empty or null.
     * @return: the master node record list.
     */
    public Collection<String> getMasterNodes(Collection<String> nodesExcluded);

    /**
     * Register the given client address into the node_online_status table.This
     * method is a wrap for
     * {@link #addOrUpdateNode(NodeOnlineStatus, int, boolean)}
     *
     * @param childAddress:the child address can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param parentAddress:the parent address can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param secondsOfNodeKeepAlive: how long this registered node will be
     *            expired.
     * @throws IllegalArgumentException
     */
    public void registerChildNode(String childAddress, String parentAddress,
                                  int secondsOfNodeKeepAlive) throws IllegalArgumentException;

    /**
     * Unregister the given client from node_online_status,the record will be
     * removed.
     *
     * @param childAddress:the child address can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param parentAddress:the parent address can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the impacted records.
     * @throws IllegalArgumentException
     */
    public int unregisterChildNode(String childAddress, String parentAddress)
            throws IllegalArgumentException;

    /**
     * Remove the given parentNodes and all its children nodes'.
     *
     * @param parentNodeAddressList: can not be null otherwise throw
     *            {@link IllegalArgumentException}, could be empty when its
     *            empty will do nothing.
     * @return: the impacted nodes' number
     * @throws IllegalArgumentException
     */
    public int removeNodeAndChildrenNodes(Collection<String> parentNodeAddressList)
            throws IllegalArgumentException;

    /**
     * Update the given nodes in batch.During the update operation,each node's
     * gmtModified will be updated with persistenceLayer'sCurrentTime,the
     * gmtExpired will be updated with
     * persistenceLayer'sCurrentTime+secondsOfNodeKeepAlive.The parentAddress
     * field will be updated with nodes[i].parentAddress.
     *
     * @param nodes:the nodes' address list.can not be null, could be empty when
     *            it's empty will do nothing.
     * @param nodes[i].address can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param nodes[i].parentAddress: could not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param secondsOfNodeKeepAlive:how long(in seconds) this node keep
     *            alive.The node's gmtExpired time will be updated with
     *            persistenceLayer'sCurrentTime+secondsOfNodeKeepAlive,it must
     *            be >=0 otherwise throw {@link IllegalArgumentException}
     * @param fillbackNodeId: once the node was persisted to persistence layer
     *            whether we need fill back it's nodeId.
     * @throws {@link IllegalArgumentException} when nodeAddressList is null
     *         will throw this exception.
     */
    public void batchAddOrUpdateNodes(Collection<NodeOnlineStatus> nodes,
                                      int secondsOfNodeKeepAlive, boolean fillbackNodeId)
            throws IllegalArgumentException;

    /**
     * please refer to {@link #batchAddOrUpdateNodes(Collection, int, boolean)}
     *
     * @param node:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param secondsOfNodeKeepAlive:how long(in seconds) this node keep
     *            alive.The node's gmtExpired time will be updated with
     *            persistenceLayer'sCurrentTime+secondsOfNodeKeepAlive,it must
     *            be >=0 otherwise throw {@link IllegalArgumentException}
     * @param fillbackNodeId:once the node was persisted to persistence layer
     *            whether we need fill back it's nodeId.
     * @throws IllegalArgumentException
     */
    public void addOrUpdateNode(NodeOnlineStatus node, int secondsOfNodeKeepAlive,
                                boolean fillbackNodeId) throws IllegalArgumentException;

    /**
     * Extend the given parent nodes expiredTime to
     * persistenceLayer'sCurrentTime+secondsOfKeepAlive.
     *
     * @param parentNodeAddressList: the parent nodes' id list, can not be null
     *            but could be empty, when its null will throw
     *            {@link IllegalArgumentException}
     * @param secondsOfNodeKeepAlive: should >=0 otherwise throw
     *            {@link IllegalArgumentException}
     * @return: impacted nodes number.
     * @throws IllegalArgumentException
     */
    public int extendNodesLifeTime(Collection<String> parentNodeAddressList,
                                   int secondsOfNodeKeepAlive) throws IllegalArgumentException;

    /**
     * Get all online parent nodes whose expiredTime >
     * persistenceLayer'sCurrentTime.And it's parentAddress=
     * {@link NodeOnlineStatus#NO_PARENT_ADDRESS}
     *
     * @return: all online parent nodes.
     */
    public Collection<NodeOnlineStatus> listLiveParentNodes();

    /**
     * Get all online parent nodes whose expiredTime <
     * persistenceLayer'sCurrentTime.And it's parentAddress=
     * {@link NodeOnlineStatus#NO_PARENT_ADDRESS}
     *
     * @return: all online parent nodes.
     */
    public Collection<String> listExpiredParentNodesAddresses();

    /**
     * Get all online parent nodes whose expiredTime >=
     * persistenceLayer'sCurrentTime.And it's parentAddress=
     * {@link NodeOnlineStatus#NO_PARENT_ADDRESS}
     *
     * @return: all online parent nodes.
     */
    public Collection<String> listLiveParentNodesAddresses();

    /**
     * Query given parent node's all online children nodes.
     *
     * @param parentAddress:parent node's address(format:ip_address:port),can
     *            not be null otherwise throw {@link IllegalArgumentException}.
     * @return: the qualified {@link NodeOnlineStatus} array list.
     * @throws IllegalArgumentException
     */
    public Collection<NodeOnlineStatus> listChildrenNodes(String parentAddress)
            throws IllegalArgumentException;
}
