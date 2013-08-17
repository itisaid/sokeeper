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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.cache.Cache;
import com.sokeeper.cache.support.CacheLRU;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.handler.ResourceAccessHandler;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.NodeOnlineStatusService;
import com.sokeeper.persist.service.PersistedConfigurationService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.service.RpcServiceBuilder;
import com.sokeeper.rpc.service.support.RpcServiceBuilderImpl;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;
import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class Server {
    final protected Logger                     logger          = LoggerFactory
                                                                       .getLogger(getClass());
    private NodePresenceTimer                  nodePresenceTimer;
    private PersistedConfigurationService      persistedConfigurationService;
    private PersistedConfiguration             persistedConfiguration;
    private SubscriberService                  subscriberService;
    private ChangesService                     changesService;
    private NodeOnlineStatusService            nodeOnlineStatusService;
    private ResourceService                    resourceService;

    private boolean                            enableLocalHost = true;
    private String                             serverIp        = RpcAddress.ALL_ZERO_ADDRESS;
    private int                                serverPort      = 9010;
    private RpcConfiguration                   serverRpcConfiguration;
    private RpcIoHandler                       serverIoHandler;
    private ResourceChangesDispatcher          resourceChangesDispatcher;
    private RpcServiceBuilder                  serviceBuilder;
    private Cache<ResourceKey, ResourceEntity> resourcesCache;
    private ResourceAccessHandler              resourceAccessHandler;

    public synchronized void startup() {
        Assert.notNull(subscriberService, "subscriberService can not be null.");
        Assert.notNull(changesService, "changesService can not be null.");
        Assert.notNull(nodeOnlineStatusService, "nodeOnlineStatusService can not be null.");
        Assert.notNull(persistedConfigurationService,
                "persistedConfigurationService can not be null.");
        Assert.notNull(resourceService, "resourceService can not be null.");
        if (persistedConfiguration == null) {
            persistedConfiguration = persistedConfigurationService.getPersistedConfiguration();
        }
        if (resourcesCache == null) {
            setResourcesCache(new CacheLRU<ResourceKey, ResourceEntity>(persistedConfiguration
                    .getMaxCachedEntities()));
        }
        if (resourceAccessHandler == null) {
            ResourceAccessHandlerImpl resourceHandlerImpl = new ResourceAccessHandlerImpl();
            resourceHandlerImpl.setResourceService(resourceService);
            resourceHandlerImpl.setSubscriberService(subscriberService);
            resourceHandlerImpl.setResourcesCache(resourcesCache);
            resourceHandlerImpl.setChangesService(changesService);
            setResourceAccessHandler(resourceHandlerImpl);
        }
        serviceBuilder = new RpcServiceBuilderImpl();
        Collection<String> localServers = null;
        // initialize serverIoHandler's configuration
        {
            String url = null;
            url = "tcp://" + serverIp + ":" + serverPort
                    + persistedConfiguration.getSuffixOfServer();
            serverRpcConfiguration = new RpcConfiguration(url);
            serverIoHandler = new RpcServerIoHandlerImpl(serverRpcConfiguration);
            localServers = serverRpcConfiguration.getMainAddress()
                    .getFullAddresses(enableLocalHost);
            serverIoHandler.registerRequestHandler(ResourceAccessHandler.class,
                    resourceAccessHandler);
        }
        // initialize resourceChangesDispatcher
        {
            resourceChangesDispatcher = new ResourceChangesDispatcher();
            resourceChangesDispatcher.setLocalServers(localServers);
            resourceChangesDispatcher.setNotifierIoHandler(serverIoHandler);
            resourceChangesDispatcher.setChangesService(changesService);
            resourceChangesDispatcher.setServiceBuilder(serviceBuilder);
            resourceChangesDispatcher.setResourcesCache(resourcesCache);
            resourceChangesDispatcher.setPersistedConfiguration(persistedConfiguration);
        }
        // initialize nodePresenceTimer
        {
            nodePresenceTimer = new NodePresenceTimer();
            nodePresenceTimer.setPersistedConfiguration(persistedConfiguration);
            nodePresenceTimer.setNodeOnlineStatusService(nodeOnlineStatusService);
            nodePresenceTimer.setLocalServers(localServers);
            nodePresenceTimer.setChangesService(changesService);
            nodePresenceTimer.setSubscriberService(subscriberService);
            nodePresenceTimer.setServerIoHandler(serverIoHandler);
            serverIoHandler.registerIoListener(nodePresenceTimer);
        }
        // startup
        if (serverIoHandler.startup()) {
            resourceChangesDispatcher.startup();
            nodePresenceTimer.startup();
            logger.info("startup server succeed on:"
                    + serverRpcConfiguration.getMainAddress().getFullAddress());
        } else {
            logger.error("startup rpc server failed on:"
                    + serverRpcConfiguration.getMainAddress().getFullAddress());
        }
    }

    public synchronized void shutdown() {
        if (nodePresenceTimer != null) {
            nodePresenceTimer.shutdown();
        }
        if (resourceChangesDispatcher != null) {
            resourceChangesDispatcher.shutdown();
        }
        if (serverIoHandler != null) {
            serverIoHandler.shutdown();
            serverIoHandler = null;
        }
        if (resourcesCache != null) {
            resourcesCache.clear();
        }
    }

    public void setSubscriberService(SubscriberService subscriberService) {
        Assert.notNull(subscriberService, "subscriberService can not be null.");
        this.subscriberService = subscriberService;
    }

    public void setChangesService(ChangesService changesService) {
        Assert.notNull(changesService, "changesService can not be null.");
        this.changesService = changesService;
    }

    public void setNodeOnlineStatusService(NodeOnlineStatusService nodeOnlineStatusService) {
        Assert.notNull(nodeOnlineStatusService, "nodeOnlineStatusService can not be null.");
        this.nodeOnlineStatusService = nodeOnlineStatusService;
    }

    public void setResourceService(ResourceService resourceService) {
        Assert.notNull(resourceService, "resourceService can not be null.");
        this.resourceService = resourceService;
    }

    public void setPersistedConfigurationService(
                                                 PersistedConfigurationService persistedConfigurationService) {
        Assert.notNull(persistedConfigurationService,
                "persistedConfigurationService can not be null.");
        this.persistedConfigurationService = persistedConfigurationService;
    }

    public void setServerIp(String serverIp) {
        Assert.hasText(serverIp, "serverIp can not be empty");
        this.serverIp = serverIp;
    }

    public void setServerPort(int serverPort) {
        Assert.isTrue(serverPort > 0, "serverPort should > 0.");
        this.serverPort = serverPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setEnableLocalHost(boolean enableLocalHost) {
        this.enableLocalHost = enableLocalHost;
    }

    public RpcConfiguration getServerRpcConfiguration() {
        return serverRpcConfiguration;
    }

    public PersistedConfiguration getPersistedConfiguration() {
        return persistedConfiguration;
    }

    public void setPersistedConfiguration(PersistedConfiguration persistedConfiguration) {
        this.persistedConfiguration = persistedConfiguration;
    }

    public void setResourcesCache(Cache<ResourceKey, ResourceEntity> cache) {
        this.resourcesCache = cache;
    }

    public Cache<ResourceKey, ResourceEntity> getResourcesCache() {
        return resourcesCache;
    }

    public void setResourceAccessHandler(ResourceAccessHandler resourceHandler) {
        this.resourceAccessHandler = resourceHandler;
    }

    public ResourceAccessHandler getResourceHandler() {
        return resourceAccessHandler;
    }

}
