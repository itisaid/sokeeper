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
import java.util.Map;

import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.PersistLayerException;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface ResourceService {
    /**
     * Add or update resourceEntity and the right side associations.This method
     * can be used in these scenarios:
     * <ul>
     * <li><b>create new resource</b>:the right associations will be built if
     * the caller passed in rightResourceType and rightAssociations means
     * rightResourceType != null and rightAssociations not empty.
     * <li><b>update resource</b>:if rightResourceType is not null the old right
     * side associations will be removed and new right side associations will be
     * built based on passed in rightAssociations.
     * </ul>
     *
     * @param resourceEntity:the resourceEntity which will be persisted to
     *            database can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceEntity.type: can not be null and must existed in database
     *            otherwise throw {@link IllegalArgumentException}.
     * @param resourceEntity.name: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param rightResourceType:the right side associated resource's type could
     *            be null when it's null will not impact the associations
     *            otherwise will re-build the right side associations.
     * @param rightAssociations:the right side associated resource's
     *            information, the resource's name will be stored as the key,
     *            the association's attributes will be stored in each item in
     *            rightAssociations.
     * @return: the new created or updated resourceEntity.
     * @throws IllegalArgumentException
     */
    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity,
                                              String rightResourceType,
                                              Map<String, AssociationEntity> rightAssociations,
                                              String clientAddress, String serverAddress)
            throws IllegalArgumentException, PersistLayerException;

    /**
     * Get the given resourceEntity from persistenceLayer.
     *
     * @param resourceType: resource's type could not be empty and must be
     *            supported by persistence layer otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceName: resource's name could not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param loadAttributes: whether load the resource's attributes.
     * @return: qualified resource entity or null.
     * @throws IllegalArgumentException
     */
    public ResourceEntity getResourceEntity(String resourceType, String resourceName,
                                            boolean loadAttributes) throws IllegalArgumentException;

    /**
     * Get given resource type and resource name resources' id.
     *
     * @param resourceType: resource's type could not be empty and must be
     *            supported by persistence layer otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceNames: could not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the resource's name and resource's id paired map.
     * @throws IllegalArgumentException
     */
    public Map<String, Long> getResourceIdsByTypeAndNames(String resourceType,
                                                          Collection<String> resourceNames)
            throws IllegalArgumentException;

    /**
     * Get associationId through the left resource's key and right resource's
     * key.
     *
     * @param leftKey: the left resource's key.
     * @param leftKey.resourceType:can not be empty.
     * @param leftKey.resourceName:can not be empty.
     * @param rightKey:the right resource's key.
     * @param rightKey.resourceType:can not be empty.
     * @param rightKey.resourceName:can not be empty.
     * @return: the association's id or the null.
     * @throws IllegalArgumentException
     */
    public Long geAssociationIdByKeys(ResourceKey leftKey, ResourceKey rightKey)
            throws IllegalArgumentException;

    /**
     * Get the association entity with attributes through left resource key and
     * right resource key.
     *
     * @param leftKey: the left resource's key.
     * @param leftKey.resourceType:can not be empty.
     * @param leftKey.resourceName:can not be empty.
     * @param rightKey:the right resource's key.
     * @param rightKey.resourceType:can not be empty.
     * @param rightKey.resourceName:can not be empty.
     * @return: the association or the null.
     * @throws IllegalArgumentException
     */
    public AssociationEntity getAssociationWithAttributesByKeys(ResourceKey leftKey,
                                                                ResourceKey rightKey)
            throws IllegalArgumentException;

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
     * @throws IllegalArgumentException
     */
    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey)
            throws IllegalArgumentException;

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
     * @throws IllegalArgumentException
     */
    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes,
                                                    String clientAddress, String serverAddress)
            throws IllegalArgumentException, IllegalStateException;

    /**
     * Get the given leftId resource's right associations with association
     * attributes.
     *
     * @param leftId: can not be null otherwise {@link IllegalArgumentException}
     * @return: the collection of associations.
     * @throws IllegalArgumentException
     */
    public Collection<AssociationEntity> getAssociationWithAttributesByLeftId(Long leftId)
            throws IllegalArgumentException;

    /**
     * Get the given rightId resource's left associations with association
     * attributes.
     *
     * @param rightId: can not be null otherwise
     *            {@link IllegalArgumentException}
     * @return: the collection of associations.
     * @throws IllegalArgumentException
     */
    public Collection<AssociationEntity> getAssociationWithAttributesByRightId(Long rightId)
            throws IllegalArgumentException;

    /**
     * Remove the given resource and all the related attributes, associations.
     *
     * @param resourceType:can not be empty and must be supported by back end
     *            service otherwise throw {@link IllegalArgumentException}.
     * @param resourceNames:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @throws IllegalArgumentException
     */
    public void removeNonHistoricResourcesByNames(String resourceType,
                                                  Collection<String> resourceNames,
                                                  String clientAddress, String serverAddress)
            throws IllegalArgumentException;
}
