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
package com.sokeeper.handler;

import java.util.Collection;
import java.util.Map;

import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.RpcException;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface ResourceAccessHandler {
    /**
     * Get the current changes event's sequence value from database.
     *
     * @return: the sequence value of changes event.
     */
    public Long getCurrentSequenceOfChanges();

    /**
     * Add or update resourceEntity, if the resourceEntity existed, will update
     * it otherwise create it.
     *
     * @param resourceEntity:could not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceEntity.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceEntity.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param rightResourceType: could be null,when it's null, the right side
     *            association will not be impacted.
     * @param rightAssociations: could be null,when it's null, it will be
     *            converted to empty map.
     * @return: the new added or updated resource entity.
     * @throws RpcException
     */
    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity,
                                              String rightResourceType,
                                              Map<String, AssociationEntity> rightAssociations)
            throws RpcException;

    /**
     * Remove the resource entity and all associations related with it.
     *
     * @param resourceKey: can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @throws RpcException
     */
    public void removeResource(ResourceKey resourceKey) throws RpcException;

    /**
     * Subscribe the given resourceEntity's changes event:resourceEntity
     * created,updated,removed or owner died, association created from/to this
     * resourceEntity,association's attributes updated related with this
     * resourceEntity.
     *
     * @param resourceKey: can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the given resourceEntity's full value(with attributes).
     * @throws RpcException
     */
    public ResourceEntity subscribe(ResourceKey resourceKey) throws RpcException;

    /**
     * Cancel the subscriber.
     *
     * @param resourceKey: can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @throws RpcException
     */
    public void unsubscribe(ResourceKey resourceKey) throws RpcException;

    /**
     * Subscribe the resources in batch.
     *
     * @param keys: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @throws RpcException
     */
    public void subscribe(Collection<ResourceKey> keys) throws RpcException;

    /**
     * When the client reconnected with server, will call this method to recover
     * the lost events during the client off-line time.
     *
     * @param sequenceGot: so far, the changes events' sequence client side
     *            already have.
     * @return: the resource/association changes whose sequence between
     *          sequenceGot and the server side cached sequence.
     * @throws RpcException
     */
    public Collection<ChangesEvent> getLostEvents(Long sequenceGot) throws RpcException;

    /**
     * Get the given resourceEntity.
     *
     * @param resourceKey: can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the given resourceEntity or null when it is not existed.
     * @throws RpcException
     */
    public ResourceEntity getResourceEntity(ResourceKey resourceKey) throws RpcException;

    /**
     * Add or update association.
     *
     * @param leftKey: the left resource's key,must existed in the persistence
     *            layer otherwise throw {@link IllegalStateException}.
     * @param leftKey.resourceType:can not be empty.
     * @param leftKey.resourceName:can not be empty.
     * @param rightKey:the right resource's key,must existed in the persistence
     *            layer otherwise throw {@link IllegalStateException}.
     * @param rightKey.resourceType:can not be empty.
     * @param rightKey.resourceName:can not be empty.
     * @param attributes: the attributes of association.
     * @param clientAddress: the client address could be null when it's null
     *            will be filled with default value.
     * @param serverAddress: the client address could be null when it's null
     *            will be filled with default value.
     * @return: the new created or updated association.
     * @throws RpcException
     */
    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes)
            throws RpcException, IllegalStateException;

    /**
     * Remove the association between left resource and right resource.Once the
     * remove succeed, the association changes will be recorded.
     *
     * @param leftKey: the left resource's key.
     * @param leftKey.resourceType:can not be empty.
     * @param leftKey.resourceName:can not be empty.
     * @param rightKey:the right resource's key.
     * @param rightKey.resourceType:can not be empty.
     * @param rightKey.resourceName:can not be empty.
     * @throws RpcException
     */
    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey) throws RpcException;

    /**
     * Get association through the left resource's key and right resource's key.
     *
     * @param leftKey: the left resource's key.
     * @param leftKey.resourceType:can not be empty.
     * @param leftKey.resourceName:can not be empty.
     * @param rightKey:the right resource's key.
     * @param rightKey.resourceType:can not be empty.
     * @param rightKey.resourceName:can not be empty.
     * @return: the association's id or the null.
     * @throws RpcException
     */
    public AssociationEntity getAssociationEntity(ResourceKey leftKey, ResourceKey rightKey)
            throws RpcException;
}
