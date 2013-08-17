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
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.domain.NodeOnlineStatus;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.RpcIoListener;
import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class NodePresenceTimer extends TimerTask implements RpcIoListener {
    final protected Logger          logger = LoggerFactory.getLogger(getClass());
    private PersistedConfiguration  persistedConfiguration;
    private Collection<String>      localServers;
    private Collection<String>      masterServers;
    private Collection<String>      onlineServers;
    private RpcIoHandler            serverIoHandler;
    private NodeOnlineStatusService nodeOnlineStatusService;
    private SubscriberService       subscriberService;
    private ChangesService          changesService;
    private Timer                   serverPresenceTimer;

    public synchronized void startup() {
        // 1, check parameters
        afterPropertiesSet();
        // 2, register as servers
        cleanup(localServers);
        nodeOnlineStatusService.registerParentNodes(localServers, persistedConfiguration
                .getSecondsOfNodeKeepAlive());
        // 3, startup timer
        serverPresenceTimer = new Timer(getTimerName(), false);
        serverPresenceTimer.schedule(this, 0,
                persistedConfiguration.getSecondsOfPresenceTimer() * 1000);
        logger.info(getTimerName() + " started");
    }

    public synchronized void shutdown() {
        if (serverPresenceTimer != null) {
            serverPresenceTimer.cancel();
            serverPresenceTimer = null;
            logger.info(getTimerName() + " stopped");
        }
        if (localServers != null) {
            cleanup(localServers);
            localServers = new HashSet<String>();
        }
        if (masterServers != null) {
            masterServers = new HashSet<String>();
        }
        if (onlineServers != null) {
            onlineServers = new HashSet<String>();
        }
    }

    public synchronized void run() {
        logger.info("NodePresenceTimer execution......");
        if (serverPresenceTimer != null) {
            // 1, extend life time
            int impacted = nodeOnlineStatusService.extendNodesLifeTime(localServers,
                    persistedConfiguration.getSecondsOfNodeKeepAlive());
            if (impacted < localServers.size()) {
                onLocalServersRemoved(localServers);
            }
            // 2, query all online servers
            final Collection<String> servers = new HashSet<String>();
            final Collection<String> masterNodes = new HashSet<String>();
            Collection<NodeOnlineStatus> onlines = nodeOnlineStatusService.listLiveParentNodes();
            for (NodeOnlineStatus node : onlines) {
                if (node.getIsMaster()) {
                    masterNodes.add(node.getClientAddress());
                }
                servers.add(node.getClientAddress());
            }
            // 3, no master nodes there, now need race to register self as master nodes
            if (masterNodes.size() <= 0) {
                raceMasterNodes(servers, masterNodes);
            }
            // 4, check whether the master nodes changed, check whether the servers changed
            boolean masterNodesChanged = !collectionEquals(masterServers, masterNodes);
            boolean serverNodesChanged = !collectionEquals(onlineServers, servers);
            if (masterNodesChanged) {
                masterServers = new HashSet<String>();
                masterServers.addAll(masterNodes);
            }
            if (serverNodesChanged) {
                onlineServers = new HashSet<String>();
                onlineServers.addAll(servers);
            }
            // 5, if I'm the master server,
            //    I need responsible for cleanup the expired online server nodes
            final boolean imMasterNode = collectionEquals(masterServers, localServers);
            if (imMasterNode) {
                cleanupExpiredParentNodes();
            }
            if (serverNodesChanged) {
                onServersNodesChanged(servers);
            }
        }
    }

    protected void raceMasterNodes(final Collection<String> servers,
                                   final Collection<String> masterNodes)
            throws IllegalArgumentException {
        try {
            nodeOnlineStatusService.registerMasterNodes(localServers);
            masterNodes.addAll(localServers);
        } catch (IllegalStateException e) {
            logger.warn("register master servers failed:" + localServers);
            masterNodes.addAll(nodeOnlineStatusService.getMasterNodes(null));
            servers.addAll(masterNodes);
        }
    }

    protected void cleanupExpiredParentNodes() throws IllegalArgumentException {
        cleanup(nodeOnlineStatusService.listExpiredParentNodesAddresses());
    }

    protected void onLocalServersRemoved(final Collection<String> localServers) {
        logger.error("the server:" + localServers
                + " has been removed by others,we need register it again.");
        nodeOnlineStatusService.registerParentNodes(localServers, persistedConfiguration
                .getSecondsOfNodeKeepAlive());
        serverIoHandler.disconnectAll();
    }

    protected void onServersNodesChanged(final Collection<String> servers)
            throws IllegalArgumentException {
        logger.info("online servers changed:" + servers);
        Collection<String> whiteIpList = new HashSet<String>();
        Set<RpcAddress> addrs = new HashSet<RpcAddress>();
        for (String server : servers) {
            RpcAddress addr = RpcSocketAddress.fromFullAddress(server);
            addrs.add(addr);
            whiteIpList.add(addr.getIpAddress());
        }
        if (serverIoHandler != null) {
            serverIoHandler.getConfiguration().setWhiteIpList(whiteIpList);
            serverIoHandler.publishNewAddedServers(addrs);
        }
    }

    public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        nodeOnlineStatusService.unregisterChildNode(clientAddress, serverAddress);
        subscriberService.removeSubscriber(clientAddress, serverAddress);
        changesService.updateStatusToOwnerDied(clientAddress, serverAddress);
    }

    public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        nodeOnlineStatusService.registerChildNode(clientAddress, serverAddress,
                persistedConfiguration.getSecondsOfNodeKeepAlive());
    }

    private boolean collectionEquals(Collection<String> first, Collection<String> second) {
        boolean isEquals = false;
        if (first != null && second != null && first.size() == second.size()) {
            isEquals = true;
            for (Object elem : first) {
                if (!second.contains(elem)) {
                    isEquals = false;
                    break;
                }
            }
        }
        return isEquals;
    }

    private String getTimerName() {
        return "ServerPresenceTimer[" + localServers.iterator().next() + "]";
    }

    private void afterPropertiesSet() {
        Assert.notNull(persistedConfiguration, "persistedConfiguration can not be null.");
        Assert.notEmpty(localServers, "localServers can not be empty");
        Assert.notNull(nodeOnlineStatusService, "nodeOnlineStatusService can not be empty");
        Assert.notNull(subscriberService, "resourceChangesSubscriberService can not be null.");
        Assert.notNull(changesService, "resourceChangesService can not be null.");
    }

    protected void cleanup(final Collection<String> servers) throws IllegalArgumentException {
        if (servers.size() > 0) {
            if (nodeOnlineStatusService != null) {
                nodeOnlineStatusService.removeNodeAndChildrenNodes(servers);
            }
            subscriberService.removeSubscribersOfServers(servers);
            changesService.updateStatusToServerDied(servers);
        }
    }

    /**
     * Local servers the presence timer represented.
     *
     * @param localServers: the servers' address list,each server's address is
     *            ip_address:listen_port,can not be null or empty, otherwise
     *            throw {@link IllegalArgumentException}.
     * @throws IllegalArgumentException
     */
    public void setLocalServers(Collection<String> localServers) throws IllegalArgumentException {
        Assert.notEmpty(localServers, "localServers can not be empty");
        this.localServers = localServers;
    }

    public Collection<String> getLocalServers() {
        return localServers;
    }

    public void setNodeOnlineStatusService(NodeOnlineStatusService nodeOnlineStatusService) {
        Assert.notNull(nodeOnlineStatusService, "nodeOnlineStatusService can not be null.");
        this.nodeOnlineStatusService = nodeOnlineStatusService;
    }

    public PersistedConfiguration getPersistedConfiguration() {
        return persistedConfiguration;
    }

    /**
     * Set the configuration which will be used by the timer.
     *
     * @param persistedConfiguration: the configuration can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @throws IllegalArgumentException
     */
    public void setPersistedConfiguration(PersistedConfiguration persistedConfiguration)
            throws IllegalArgumentException {
        Assert.notNull(persistedConfiguration, "persistedConfiguration can not be null.");
        this.persistedConfiguration = persistedConfiguration;
    }

    public Collection<String> getMasterServers() {
        return masterServers;
    }

    public void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    public void setChangesService(ChangesService resourceChangesService) {
        this.changesService = resourceChangesService;
    }

    public void setServerIoHandler(RpcIoHandler serverIoHandler) {
        this.serverIoHandler = serverIoHandler;
    }

}
