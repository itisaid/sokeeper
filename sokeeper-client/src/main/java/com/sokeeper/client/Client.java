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
package com.sokeeper.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.cache.Cache;
import com.sokeeper.cache.support.CacheLRU;
import com.sokeeper.client.listener.ResourceEventListener;
import com.sokeeper.client.listener.RpcIoEventListener;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.RpcException;
import com.sokeeper.handler.ResourceAccessHandler;
import com.sokeeper.handler.ResourceChangesHandler;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.service.RpcServiceBuilder;
import com.sokeeper.rpc.service.support.RpcServiceBuilderImpl;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.RpcIoListener;
import com.sokeeper.rpc.transport.support.RpcClientIoHandlerImpl;
import com.sokeeper.util.Assert;
import com.sokeeper.util.NamedThreadFactory;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class Client implements ResourceChangesHandler, RpcIoListener {
    final protected Logger                                                logger            = LoggerFactory
                                                                                                    .getLogger(getClass());
    private RpcIoHandler                                                  clientIoHandler;
    private String                                                        serverUrl;
    private Cache<ResourceKey, ResourceEntity>                            resourcesCache;
    private ResourceAccessHandler                                         resourceAccessHandler;
    private RpcServiceBuilder                                             serviceBuilder;
    private AtomicBoolean                                                 recoverEnabled    = new AtomicBoolean(
                                                                                                    false);

    private AtomicLong                                                    rcsVisited        = new AtomicLong(
                                                                                                    -1);
    private ConcurrentMap<ResourceKey, Collection<ResourceEventListener>> resourceListeners = new ConcurrentHashMap<ResourceKey, Collection<ResourceEventListener>>();
    private Collection<RpcIoEventListener>                                ioListeners       = new HashSet<RpcIoEventListener>();
    private ExecutorService                                               notifierThreadPool;

    /////////////////////////////////////////getter and setter//////////////////////////////////////////////////////////////////////

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public RpcIoHandler getClientIoHandler() {
        return clientIoHandler;
    }

    public void setResourcesCache(Cache<ResourceKey, ResourceEntity> cache) {
        this.resourcesCache = cache;
    }

    public Cache<ResourceKey, ResourceEntity> getResourcesCache() {
        return resourcesCache;
    }

    public ResourceAccessHandler getResourceHandler() {
        return resourceAccessHandler;
    }

    public void setResourceAccessHandler(ResourceAccessHandler resourceAccessHandler) {
        this.resourceAccessHandler = resourceAccessHandler;
    }

    public Long getVisitedSequenceOfChanges() {
        return rcsVisited.get();
    }

    /////////////////////////////////////////register listeners//////////////////////////////////////////////////////////////////////

    public void registIoListener(RpcIoEventListener ioEventListener) {
        Assert.notNull(ioEventListener, "ioEventListener can not be null.");
        ioListeners.add(ioEventListener);
    }

    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity,
                                              String rightResourceType,
                                              Map<String, AssociationEntity> rightAssociations)
            throws RpcException {
        Assert.notNull(resourceEntity, "resourceEntity can not be null.");
        Assert.hasText(resourceEntity.getResourceName(),
                "resourceEntity.resourceName can not be empty");
        Assert.hasText(resourceEntity.getResourceType(),
                "resourceEntity.resourceType can not be empty");
        return resourceAccessHandler.addOrUpdateResource(resourceEntity, rightResourceType,
                rightAssociations);
    }

    public void removeResource(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        resourceAccessHandler.removeResource(resourceKey);
    }

    public ResourceEntity subscribe(ResourceKey resourceKey, ResourceEventListener listener)
            throws RpcException {
        ResourceKey.validate(resourceKey);
        if (listener != null) {
            synchronized (resourceListeners) {
                resourceListeners.putIfAbsent(resourceKey, new HashSet<ResourceEventListener>());
                resourceListeners.get(resourceKey).add(listener);
            }
        }
        ResourceEntity resourceEntity = null;
        if (resourceAccessHandler != null) {
            resourceEntity = resourceAccessHandler.subscribe(resourceKey);
            if (resourcesCache != null) {
                resourcesCache.remove(resourceKey);
                if (resourceEntity != null) {
                    resourcesCache.put(resourceKey, resourceEntity);
                }
            }
        }
        return resourceEntity;
    }

    public void unsubscribe(ResourceKey resourceKey, ResourceEventListener listener)
            throws RpcException {
        ResourceKey.validate(resourceKey);
        Assert.notNull(listener, "listener can not be null.");
        synchronized (resourceListeners) {
            if (resourceListeners.containsKey(resourceKey)) {
                resourceListeners.get(resourceKey).remove(listener);
                if (resourceListeners.get(resourceKey).size() == 0) {
                    resourceListeners.remove(resourceKey);
                    resourceAccessHandler.unsubscribe(resourceKey);
                }
            }
        }
    }

    public ResourceEntity getResourceEntity(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        ResourceEntity resourceEntity = resourcesCache.get(resourceKey);
        if (resourceEntity == null) {
            resourceEntity = resourceAccessHandler.getResourceEntity(resourceKey);
            if (resourceEntity != null) {
                resourcesCache.put(resourceKey, resourceEntity);
            }
        }
        return resourceEntity;
    }

    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes)
            throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        return resourceAccessHandler.addOrUpdateAssociation(leftKey, rightKey, attributes);
    }

    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey) throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        resourceAccessHandler.removeAssociation(leftKey, rightKey);
    }

    public AssociationEntity getAssociationEntity(ResourceKey leftKey, ResourceKey rightKey)
            throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        return resourceAccessHandler.getAssociationEntity(leftKey, rightKey);
    }

    /////////////////////////////////////////startup & shutdown///////////////////////////////////////////////////////////////////
    public synchronized void startup() throws RpcException {
        Assert.hasText(serverUrl, "serverUrl can not be empty");
        if (serviceBuilder == null) {
            serviceBuilder = new RpcServiceBuilderImpl();
        }
        if (notifierThreadPool != null) {
            notifierThreadPool.shutdown();
        }
        notifierThreadPool = Executors.newCachedThreadPool(new NamedThreadFactory("notifier["
                + serverUrl + "]", true));
        if (clientIoHandler == null) {
            clientIoHandler = new RpcClientIoHandlerImpl(new RpcConfiguration(serverUrl));
            clientIoHandler.registerRequestHandler(ResourceChangesHandler.class, this);
            clientIoHandler.registerIoListener(this);
        }
        if (resourceAccessHandler == null) {
            resourceAccessHandler = serviceBuilder.buildRemoteServiceProxy(
                    ResourceAccessHandler.class, null, null, clientIoHandler, false);
        }
        if (resourcesCache == null) {
            setResourcesCache(new CacheLRU<ResourceKey, ResourceEntity>(
                    PersistedConfiguration.DEFAULT_MAX_CACHED_ENTITIES));
        }
        clientIoHandler.startup();
    }

    public synchronized void shutdown() {
        if (clientIoHandler != null) {
            clientIoHandler.shutdown();
        }
        if (notifierThreadPool != null) {
            notifierThreadPool.shutdown();
        }
        resourceListeners.clear();
        resourcesCache.clear();
        ioListeners.clear();
    }

    /////////////////////////////////////////receive IO events///////////////////////////////////////////////////////////////////
    public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
        for (RpcIoEventListener listener : ioListeners) {
            listener.onConnectionClosed(connection, ioHandler);
        }
    }

    /**
     * To avoid during the recover time the new incoming resource changes impact
     * the recover for lost changes events,we set the recoverEnabled signal
     * variable,when this variable set to true, the onResourcesChanged notify
     * thread will wait until the recover finished.
     */
    public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
        try {
            // enter into recover mode
            recoverEnabled.set(true);

            if (resourcesCache != null) {
                resourcesCache.clear();
            }
            if (rcsVisited.get() < 0L) {
                updateRcsVisited(resourceAccessHandler.getCurrentSequenceOfChanges());

                // quit recover mode
                recoverEnabled.set(false);

            }
            if (resourceListeners.size() > 0) {
                try {
                    synchronized (resourceListeners) {
                        HashSet<ResourceKey> keys = new HashSet<ResourceKey>();
                        keys.addAll(resourceListeners.keySet());
                        resourceAccessHandler.subscribe(keys);
                    }
                    // recover the lost changes events
                    if (recoverEnabled.get()) {
                        Collection<ChangesEvent> changes = resourceAccessHandler
                                .getLostEvents(rcsVisited.get());
                        publishChanges(changes);
                    }
                } catch (RpcException e) {
                    logger.error("recoverLostEvents:", e);
                }
            }
        } finally {
            // quit recover mode
            recoverEnabled.set(false);
        }
        for (RpcIoEventListener listener : ioListeners) {
            listener.onConnectionCreated(connection, ioHandler);
        }
    }

    /////////////////////////////////////////receive resource events/////////////////////////////////////////////////////////////
    public void onResourcesChanged(Map<RpcAddress, Collection<ChangesEvent>> changes) {
        // wait until the recover thread finished
        while (recoverEnabled.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        Collection<ChangesEvent> resourceChanges = null;
        if (changes.size() > 0) {
            resourceChanges = changes.values().iterator().next();
        }
        if (resourceChanges != null) {
            publishChanges(resourceChanges);
        }
    }

    private void updateRcsVisited(Long sequence) {
        if (rcsVisited.get() < sequence) {
            rcsVisited.set(sequence);
        }
    }

    private synchronized void publishChanges(Collection<? extends ChangesEvent> changes) {
        // 1, parse the changes
        Long sequenceMax = 0L;
        Collection<ResourceKey> removedKeys = new HashSet<ResourceKey>();
        Map<ChangesEvent, Collection<ResourceEventListener>> map = new HashMap<ChangesEvent, Collection<ResourceEventListener>>();
        if (changes != null) {
            for (final ChangesEvent change : changes) {
                if (change.getSequence() > sequenceMax) {
                    sequenceMax = change.getSequence();
                }
                // if the changes' sequence smaller than the cached rcsVisited,should ignore the event
                if (change.getSequence() > rcsVisited.get()) {
                    Collection<ResourceEventListener> listeners = new HashSet<ResourceEventListener>();
                    synchronized (resourceListeners) {
                        if (change instanceof ResourceChangesEvent) {
                            final ResourceChangesEvent event = (ResourceChangesEvent) change;
                            removedKeys.add(event.getResourceKey());
                            if (resourceListeners.containsKey(event.getResourceKey())) {
                                listeners.addAll(resourceListeners.get(event.getResourceKey()));
                            }
                        } else {
                            final AssociationChangesEvent event = (AssociationChangesEvent) change;
                            if (resourceListeners.containsKey(event.getLeftKey())) {
                                listeners.addAll(resourceListeners.get(event.getLeftKey()));
                            }
                            if (resourceListeners.containsKey(event.getRightKey())) {
                                listeners.addAll(resourceListeners.get(event.getRightKey()));
                            }
                        }
                    }
                    map.put(change, listeners);
                }
            }
        }
        // 2, calculate max sequence and update it to rcsVisited
        updateRcsVisited(sequenceMax);
        // 3, remove the cached resources if the changes is resource changes
        resourcesCache.removeElements(removedKeys.toArray(new Object[0]));
        // 4, publish changes to subscribers
        for (final Map.Entry<ChangesEvent, Collection<ResourceEventListener>> pair : map.entrySet()) {
            for (final ResourceEventListener listener : pair.getValue()) {
                notifierThreadPool.execute(new Runnable() {
                    public void run() {
                        if (pair.getKey() instanceof ResourceChangesEvent) {
                            listener.onResourceChanged((ResourceChangesEvent) pair.getKey());
                        } else {
                            listener.onAssociationChanged((AssociationChangesEvent) pair.getKey());
                        }
                    }
                });
            }
        }
    }

}
