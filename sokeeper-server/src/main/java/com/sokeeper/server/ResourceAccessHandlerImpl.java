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
import java.util.Map;

import org.springframework.util.Assert;

import com.sokeeper.cache.Cache;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.RpcException;
import com.sokeeper.handler.ResourceAccessHandler;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.transport.RpcConnection;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceAccessHandlerImpl implements ResourceAccessHandler {

    private SubscriberService                  subscriberService;

    private ResourceService                    resourceService;

    private ChangesService                     changesService;

    private Cache<ResourceKey, ResourceEntity> resourcesCache;

    public void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public void setResourcesCache(Cache<ResourceKey, ResourceEntity> resourcesCache) {
        this.resourcesCache = resourcesCache;
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

    public void setChangesService(ChangesService changesService) {
        this.changesService = changesService;
    }

    public Long getCurrentSequenceOfChanges() {
        return changesService.getCurrentSequenceOfChanges();
    }

    public Cache<ResourceKey, ResourceEntity> getResourcesCache() {
        return resourcesCache;
    }

    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity,
                                              String rightResourceType,
                                              Map<String, AssociationEntity> rightAssociations)
            throws RpcException {
        ResourceEntity.validate(resourceEntity);
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        subscriberService.addResourceChangesSubscriber(resourceEntity.getResourceType(),
                resourceEntity.getResourceName(), clientAddress, serverAddress);
        resourceEntity = resourceService.addOrUpdateResource(resourceEntity, rightResourceType,
                rightAssociations, clientAddress, serverAddress);
        return resourceEntity;
    }

    public ResourceEntity subscribe(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        subscriberService.addResourceChangesSubscriber(resourceKey.getResourceType(), resourceKey
                .getResourceName(), clientAddress, serverAddress);
        return getResourceEntity(resourceKey);
    }

    public void unsubscribe(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        subscriberService.removeSubscriber(resourceKey, clientAddress, serverAddress);
    }

    public void subscribe(Collection<ResourceKey> keys) throws RpcException {
        Assert.notEmpty(keys, "keys can not be empty");
        for (ResourceKey resourceKey : keys) {
            ResourceKey.validate(resourceKey);
        }
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        for (ResourceKey resourceKey : keys) {
            subscriberService.addResourceChangesSubscriber(resourceKey.getResourceType(),
                    resourceKey.getResourceName(), clientAddress, serverAddress);
        }
    }

    public ResourceEntity getResourceEntity(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        ResourceEntity resourceEntity = resourcesCache.get(resourceKey);
        if (resourceEntity == null) {
            resourceEntity = resourceService.getResourceEntity(resourceKey.getResourceType(),
                    resourceKey.getResourceName(), true);
            if (resourceEntity != null) {
                resourcesCache.put(resourceKey, resourceEntity);
            }
        }
        return resourceEntity;
    }

    public void removeResource(ResourceKey resourceKey) throws RpcException {
        ResourceKey.validate(resourceKey);
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        resourceService.removeNonHistoricResourcesByNames(resourceKey.getResourceType(), Arrays
                .asList(resourceKey.getResourceName()), clientAddress, serverAddress);
    }

    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes)
            throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        Assert.notNull(RpcConnection.getCurrentRpcConnection(),
                "RpcConnection.getCurrentRpcConnection is null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        return resourceService.addOrUpdateAssociation(leftKey, rightKey, attributes, clientAddress,
                serverAddress);
    }

    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey) throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        resourceService.removeAssociation(leftKey, rightKey);
    }

    public AssociationEntity getAssociationEntity(ResourceKey leftKey, ResourceKey rightKey)
            throws RpcException {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        return resourceService.getAssociationWithAttributesByKeys(leftKey, rightKey);
    }

    public Collection<ChangesEvent> getLostEvents(Long sequenceGot) throws RpcException {
        Assert.notNull(sequenceGot, "sequenceGot can not be null.");
        RpcConnection connection = RpcConnection.getCurrentRpcConnection();
        String clientAddress = connection.getRemoteAddress().getFullAddress();
        String serverAddress = connection.getLocalAddress().getFullAddress();
        Long sequenceFrom = sequenceGot + 1;
        Long sequenceTo = getCurrentSequenceOfChanges();
        Collection<ChangesEvent> changes = new HashSet<ChangesEvent>();
        if (sequenceFrom <= sequenceTo) {
            Collection<AssociationChangesEvent> associationChanges = changesService
                    .listAssociationChangesEvent(sequenceFrom, sequenceTo, clientAddress,
                            serverAddress);
            Collection<ResourceChangesEvent> resourceChanges = changesService
                    .listResourceChangesEvent(sequenceFrom, sequenceTo, clientAddress,
                            serverAddress);
            changes.addAll(resourceChanges);
            changes.addAll(associationChanges);
        }
        return changes;
    }

}
