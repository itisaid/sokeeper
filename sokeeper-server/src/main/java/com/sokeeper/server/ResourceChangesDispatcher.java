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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.cache.Cache;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.handler.ResourceChangesHandler;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.rpc.service.RpcServiceBuilder;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * The dispatcher responsible for dispatch the resource changes to the clients
 * on demand(means when the client subscribed the changes event it will receive
 * the changes).The dispatcher will listen the receiverIoHandler's IO events,
 * when it reconnected with master server, need recover the lost resource
 * changes events during the receiverIoHandler off line time.
 *
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceChangesDispatcher extends TimerTask {
    final protected Logger                     logger     = LoggerFactory.getLogger(getClass());
    private Timer                              watcherTimer;
    private PersistedConfiguration             persistedConfiguration;
    private RpcServiceBuilder                  serviceBuilder;
    private RpcIoHandler                       notifierIoHandler;
    private ChangesService                     changesService;
    private Collection<String>                 localServers;
    private AtomicLong                         rcsVisited = new AtomicLong(0);
    private ResourceChangesHandler             toClientsNotifier;
    private Cache<ResourceKey, ResourceEntity> resourcesCache;

    public synchronized void startup() {
        Assert.notNull(serviceBuilder, "serviceBuilder can not be null.");
        Assert.notNull(notifierIoHandler, "notifierIoHandler can not be null.");
        Assert.notNull(changesService, "changesService can not be null.");
        Assert.notEmpty(localServers, "localServers can not be empty");
        toClientsNotifier = serviceBuilder.buildRemoteServiceProxy(ResourceChangesHandler.class,
                null, null, notifierIoHandler, true);
        if (watcherTimer == null) {
            rcsVisited.set(changesService.getCurrentSequenceOfChanges());
            watcherTimer = new Timer(getTimerName(), true);
            watcherTimer.schedule(this, 0, persistedConfiguration
                    .getSecondsOfResourceChangesWatcherTimer());
        }
    }

    private String getTimerName() {
        return notifierIoHandler.getConfiguration().getMainAddress().getFullAddress()
                + "-resourceTimer";
    }

    public synchronized void shutdown() {
        if (watcherTimer != null) {
            watcherTimer.cancel();
            watcherTimer = null;
        }
    }

    public void run() {
        Long sequenceTo = changesService.getCurrentSequenceOfChanges();
        Long sequenceFrom = rcsVisited.get() + 1;
        if (sequenceTo >= sequenceFrom) {
            rcsVisited.set(sequenceTo);
            // 1, query changes events
            Collection<ResourceChangesEvent> resourceChanges = changesService
                    .listResourceChangesEvent(sequenceFrom, sequenceTo, localServers);
            Collection<AssociationChangesEvent> associationChanges = changesService
                    .listAssociationChangesEvent(sequenceFrom, sequenceTo, localServers);
            // 2, remove the cached resources
            Collection<ResourceKey> removedKeys = new HashSet<ResourceKey>();
            for (ResourceChangesEvent event : resourceChanges) {
                removedKeys.add(event.getResourceKey());
            }
            if (resourcesCache != null) {
                resourcesCache.removeElements(removedKeys.toArray(new Object[0]));
            }
            // 3, notify subscribers
            Map<RpcAddress, Collection<ChangesEvent>> changesToClients = new HashMap<RpcAddress, Collection<ChangesEvent>>();
            Map<String, Collection<ChangesEvent>> changes = new HashMap<String, Collection<ChangesEvent>>();
            for (ResourceChangesEvent event : resourceChanges) {
                if (!changes.containsKey(event.getSubscriber())) {
                    changes.put(event.getSubscriber(), new HashSet<ChangesEvent>());
                }
                changes.get(event.getSubscriber()).add(event);
            }
            for (AssociationChangesEvent event : associationChanges) {
                if (!changes.containsKey(event.getSubscriber())) {
                    changes.put(event.getSubscriber(), new HashSet<ChangesEvent>());
                }
                changes.get(event.getSubscriber()).add(event);
            }
            for (Map.Entry<String, Collection<ChangesEvent>> pair : changes.entrySet()) {
                changesToClients.put(RpcSocketAddress.fromFullAddress(pair.getKey()), pair
                        .getValue());
            }
            if (changesToClients.size() > 0) {
                try {
                    toClientsNotifier.onResourcesChanged(changesToClients);
                } catch (Throwable e) {
                    logger.error("send association changes to clients failed,due to:", e);
                }
            }
        }
    }

    public void setServiceBuilder(RpcServiceBuilder serviceBuilder) {
        this.serviceBuilder = serviceBuilder;
    }

    public void setNotifierIoHandler(RpcIoHandler notifierIoHandler) {
        this.notifierIoHandler = notifierIoHandler;
    }

    public void setChangesService(ChangesService resourceChangesService) {
        this.changesService = resourceChangesService;
    }

    public void setLocalServers(Collection<String> localServers) {
        this.localServers = localServers;
    }

    public void setResourcesCache(Cache<ResourceKey, ResourceEntity> cache) {
        this.resourcesCache = cache;
    }

    public void setPersistedConfiguration(PersistedConfiguration persistedConfiguration) {
        this.persistedConfiguration = persistedConfiguration;
    }
}
