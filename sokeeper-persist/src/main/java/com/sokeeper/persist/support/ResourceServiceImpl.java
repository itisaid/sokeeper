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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sokeeper.domain.AssociationChanges;
import com.sokeeper.domain.ChangesEntity;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceType;
import com.sokeeper.domain.resource.AssociationAttributeDTO;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.AttributeEntity;
import com.sokeeper.domain.resource.AttributedDomainEntity;
import com.sokeeper.domain.resource.ResourceAttributeDTO;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.PersistLayerException;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.ResourceTypeService;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("resourceService")
public class ResourceServiceImpl extends BaseResourceService implements ResourceService {

    @Autowired
    private ResourceTypeService resourceTypeService;

    @Transactional
    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity, String rightResourceType,
                                              Map<String, AssociationEntity> associations, String clientAddress,
                                              String serverAddress) throws IllegalArgumentException,
                                                                   PersistLayerException {
        ResourceType leftType = null;
        ResourceType rightType = null;
        // 1, validate parameters
        {
            Assert.notNull(resourceEntity, "resourceEntity can not be null.");
            Assert.hasText(resourceEntity.getResourceType(), "resourceEntity.type can not be null.");
            Assert.hasText(resourceEntity.getResourceName(), "resourceEntity.name can not be null.");
            leftType = resourceTypeService.getResourceType(resourceEntity.getResourceType());
            Assert.notNull(leftType, "Unsupported resourceType:" + resourceEntity.getResourceType() + ".");
            validateAttributes(resourceEntity);

            if (rightResourceType != null) {
                rightType = resourceTypeService.getResourceType(rightResourceType);
                Assert.notNull(rightType, "Unsupported rightResourceType:" + rightResourceType + ".");
                if (associations != null) {
                    for (AssociationEntity association : associations.values()) {
                        validateAttributes(association);
                    }
                }
            }
        }
        // 2, if the resource need keep historic, before we update it,we need backup the current one
        if (leftType.getKeepHistoric()) {
            // TODO:
        }
        // 3, add or update the resourceEntity
        Collection<Long> removedAssociationIds = new HashSet<Long>();
        Collection<AssociationChanges> associationChanges = new HashSet<AssociationChanges>();
        ResourceChanges resourceChanges = prepareResourceChanges(resourceEntity);
        // 4, process associations
        if (rightResourceType != null) {
            // 4.1 resolve associations' leftId and rightId
            Map<Long, String> rightResourceIdNameMap = new HashMap<Long, String>();
            Map<Long, AssociationEntity> rightResourceIdAssociationMap = new HashMap<Long, AssociationEntity>();
            prepareAssociations(resourceEntity, rightResourceType, associations, rightResourceIdNameMap,
                                rightResourceIdAssociationMap);
            // 4.2 add or update each association
            batchUpdateEntities(associations.values().toArray(new AssociationEntity[0]),
                                "resource.addOrUpdateAssociationEntity");
            // 4.3 prepare association changes
            prepareAssociationChanges(resourceEntity, resourceChanges, removedAssociationIds, associationChanges,
                                      rightResourceIdNameMap, rightResourceIdAssociationMap);
            // 4.4 drop the unused associations and their attributes
            removeAttrsByOwnerIds(removedAssociationIds);
            removeAssociationByIds(removedAssociationIds);
        }
        // 5, process changes event
        if (clientAddress == null) {
            clientAddress = ResourceChanges.NO_CLIENT_ADDRESS;
        }
        if (serverAddress == null) {
            serverAddress = ResourceChanges.NO_SERVER_ADDRESS;
        }
        if (resourceChanges != null) {
            resourceChanges.setClientAddress(clientAddress);
            resourceChanges.setServerAddress(serverAddress);
            resourceChanges.setResourceType(leftType.getTypeName());
            resourceChanges.setResourceId(resourceEntity.getResourceName());
            resourceChanges.setAcceptOwnerDiedEvent(leftType.getOnlineResource());
            getSqlMapClientTemplate().update("resource.addOrUpdateResourceChanges", resourceChanges);
        }
        if (removedAssociationIds.size() > 0) {
            getSqlMapClientTemplate().update("resource.updateAssociationChangesToDeletedByAssociationIds",
                                             removedAssociationIds.toArray(new Long[0]));
        }
        if (associationChanges.size() > 0) {
            for (AssociationChanges ac : associationChanges) {
                ac.setLeftType(leftType.getTypeName());
                ac.setRightType(rightType.getTypeName());
                ac.setAcceptOwnerDiedEvent(leftType.getOnlineResource());
                ac.setClientAddress(clientAddress);
                ac.setServerAddress(serverAddress);
                ac.setLeftId(resourceEntity.getResourceName());
            }
            batchUpdateEntities(associationChanges.toArray(new AssociationChanges[0]),
                                "resource.addOrUpdateAssociationChanges");
        }
        return resourceEntity;
    }

    private ResourceChanges prepareResourceChanges(ResourceEntity resourceEntity) throws IllegalArgumentException {
        ResourceChanges resourceChanges = null;
        getSqlMapClientTemplate().insert("resource.addOrUpdateResourceEntity", resourceEntity);
        ResourceEntity persistedEntity = getResourceEntity(resourceEntity.getResourceType(),
                                                           resourceEntity.getResourceName(), true);
        resourceEntity.setId(persistedEntity.getId());
        resourceEntity.setVersion(persistedEntity.getVersion());
        int impacted = addOrUpdateAttributes(resourceEntity, persistedEntity.getAttributes());
        if (resourceEntity.getVersion().equals(ResourceEntity.INITIAL_VERSION)) {
            resourceChanges = new ResourceChanges();
            resourceChanges.setChanges(ResourceChanges.CHANGES_CREATED);
        } else if (impacted > 0) {
            resourceChanges = new ResourceChanges();
            resourceChanges.setChanges(ResourceChanges.CHANGES_UPDATED);
        }
        return resourceChanges;
    }

    private void prepareAssociations(ResourceEntity resourceEntity, String rightResourceType,
                                     Map<String, AssociationEntity> associations,
                                     Map<Long, String> rightResourceIdNameMap,
                                     Map<Long, AssociationEntity> rightResourceIdAssociationMap)
                                                                                                throws IllegalArgumentException,
                                                                                                PersistLayerException {
        Map<String, Long> rightResourceNameIdMap = getResourceIdsByTypeAndNames(rightResourceType,
                                                                                associations.keySet());
        for (Entry<String, AssociationEntity> pair : associations.entrySet()) {
            String rightName = pair.getKey();
            Long rightId = rightResourceNameIdMap.get(rightName);
            if (rightId == null) {
                throw new PersistLayerException("the right side resource:[type=" + rightResourceType + ",name="
                                                + rightName + "] does not exist.");
            }
            AssociationEntity associationEntity = pair.getValue();
            associationEntity.setLeftId(resourceEntity.getId());
            associationEntity.setRightId(rightId);
            rightResourceIdNameMap.put(rightId, rightName);
            rightResourceIdAssociationMap.put(rightId, associationEntity);
        }
    }

    private void prepareAssociationChanges(ResourceEntity resourceEntity, ResourceChanges resourceChanges,
                                           Collection<Long> removedAssociationIds,
                                           Collection<AssociationChanges> associationChanges,
                                           Map<Long, String> rightResourceIdNameMap,
                                           Map<Long, AssociationEntity> rightResourceIdAssociationMap)
                                                                                                      throws IllegalArgumentException {
        Collection<AssociationEntity> associationsInDb = getAssociationWithAttributesByLeftId(resourceEntity.getId());
        for (AssociationEntity associationInDb : associationsInDb) {
            if (rightResourceIdAssociationMap.containsKey(associationInDb.getRightId())) {
                AssociationEntity created = rightResourceIdAssociationMap.remove(associationInDb.getRightId());
                created.setId(associationInDb.getId());
                int impacted = addOrUpdateAttributes(created, associationInDb.getAttributes());
                AssociationChanges ac = null;
                if (!associationInDb.isChanged()) {
                    ac = new AssociationChanges();
                    ac.setChanges(AssociationChanges.CHANGES_CREATED);
                } else if (impacted > 0) {
                    ac = new AssociationChanges();
                    ac.setChanges(AssociationChanges.CHANGES_UPDATED);
                }
                // when the resources is updated,but the association keep unchanged
                // should generate association changed events.
                else if (resourceChanges != null && resourceChanges.getChanges().equals(ChangesEntity.CHANGES_UPDATED)) {
                    ac = new AssociationChanges();
                    ac.setChanges(AssociationChanges.CHANGES_UPDATED);
                }
                if (ac != null) {
                    ac.setAssociationId(created.getId());
                    ac.setRightId(rightResourceIdNameMap.get(created.getRightId()));
                    associationChanges.add(ac);
                }
            } else {
                removedAssociationIds.add(associationInDb.getId());
            }
        }
    }

    public Long geAssociationIdByKeys(ResourceKey leftKey, ResourceKey rightKey) {
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        Map<String, ResourceKey> map = new HashMap<String, ResourceKey>();
        map.put("leftKey", leftKey);
        map.put("rightKey", rightKey);
        return (Long) getSqlMapClientTemplate().queryForObject("resource.geAssociationIdByKeys", map);
    }

    @SuppressWarnings("unchecked")
    public AssociationEntity getAssociationWithAttributesByKeys(ResourceKey leftKey, ResourceKey rightKey) {
        AssociationEntity associationEntity = null;
        Long associationId = geAssociationIdByKeys(leftKey, rightKey);
        if (associationId != null) {
            Collection<AssociationAttributeDTO> attributes = getSqlMapClientTemplate().queryForList(
                                                                                                    "resource.geAssociationWithAttributesById",
                                                                                                    associationId);
            Map<Long, AssociationEntity> associations = mergeAssociationAttributes(attributes);
            if (associations.size() > 0) {
                associationEntity = associations.values().iterator().next();
            }
            return associationEntity;
        }
        return associationEntity;
    }

    @Transactional
    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey) {
        Long associationId = geAssociationIdByKeys(leftKey, rightKey);
        if (associationId != null) {
            removeAttrsByOwnerIds(Arrays.asList(associationId));
            removeAssociationByIds(Arrays.asList(associationId));
            getSqlMapClientTemplate().update("resource.updateAssociationChangesToDeletedByAssociationIds",
                                             new Long[] { associationId });
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes, String clientAddress,
                                                    String serverAddress) throws IllegalArgumentException {
        // 1, validate parameters
        ResourceKey.validate(leftKey);
        ResourceKey.validate(rightKey);
        if (clientAddress == null) {
            clientAddress = ResourceChanges.NO_CLIENT_ADDRESS;
        }
        if (serverAddress == null) {
            serverAddress = ResourceChanges.NO_SERVER_ADDRESS;
        }
        // 2, check leftEntity and rightEntity
        ResourceEntity leftEntity = getResourceEntity(leftKey.getResourceType(), leftKey.getResourceName(), false);
        Assert.state(leftEntity != null, "left entity[type=" + leftKey.getResourceType() + ",name="
                                         + leftKey.getResourceName() + "] can not be found.");
        ResourceEntity rightEntity = getResourceEntity(rightKey.getResourceType(), rightKey.getResourceName(), false);
        Assert.state(rightEntity != null, "right entity[type=" + rightKey.getResourceType() + ",name="
                                          + rightKey.getResourceName() + "] can not be found.");

        // 3, construct the association entity
        AssociationEntity created = new AssociationEntity();
        created.setLeftId(leftEntity.getId());
        created.setRightId(rightEntity.getId());
        if (attributes != null) {
            for (Map.Entry<String, String> attrPair : attributes.entrySet()) {
                AttributeEntity attrEntity = new AttributeEntity();
                attrEntity.setKey(attrPair.getKey());
                attrEntity.setValue(attrPair.getValue());
                created.addAttribute(attrEntity);
            }
        }

        // 4, add or update the association entity
        getSqlMapClientTemplate().update("resource.addOrUpdateAssociationEntity", created);

        // 5, query the saved entity
        AssociationEntity associationInDb = null;
        {
            Map<String, Long> map = new HashMap<String, Long>();
            map.put("leftId", leftEntity.getId());
            map.put("rightId", rightEntity.getId());
            Collection<AssociationAttributeDTO> savedAssociationAttributes = getSqlMapClientTemplate().queryForList(
                                                                                                                    "resource.geAssociationWithAttributesByLeftIdRightId",
                                                                                                                    map);
            Map<Long, AssociationEntity> associations = mergeAssociationAttributes(savedAssociationAttributes);
            associationInDb = associations.values().iterator().next();
        }
        created.setId(associationInDb.getId());
        // 6, update the attributes
        int impacted = addOrUpdateAttributes(created, associationInDb.getAttributes());
        // 7, calculate the association is created or updated
        AssociationChanges ac = null;
        if (!associationInDb.isChanged()) {
            ac = new AssociationChanges();
            ac.setChanges(AssociationChanges.CHANGES_CREATED);
        } else if (impacted > 0) {
            ac = new AssociationChanges();
            ac.setChanges(AssociationChanges.CHANGES_UPDATED);
        }
        if (ac != null) {
            ResourceType leftType = resourceTypeService.getResourceType(leftKey.getResourceType());
            ac.setAssociationId(created.getId());
            ac.setAcceptOwnerDiedEvent(leftType.getOnlineResource());
            ac.setLeftType(leftKey.getResourceType());
            ac.setLeftId(leftKey.getResourceName());
            ac.setRightType(rightKey.getResourceType());
            ac.setRightId(rightKey.getResourceName());
            ac.setClientAddress(clientAddress);
            ac.setServerAddress(serverAddress);
            getSqlMapClientTemplate().update("resource.addOrUpdateAssociationChanges", ac);
        }
        return created;
    }

    @SuppressWarnings("unchecked")
    public Collection<AssociationEntity> getAssociationWithAttributesByLeftId(Long leftId)
                                                                                          throws IllegalArgumentException {
        Assert.notNull(leftId, "leftId can not be null.");
        Collection<AssociationAttributeDTO> attributes = getSqlMapClientTemplate().queryForList(
                                                                                                "resource.geAssociationWithAttributesByLeftId",
                                                                                                leftId);
        Map<Long, AssociationEntity> associations = mergeAssociationAttributes(attributes);
        return associations.values();
    }

    @SuppressWarnings("unchecked")
    public Collection<AssociationEntity> getAssociationWithAttributesByRightId(Long rightId)
                                                                                            throws IllegalArgumentException {
        Assert.notNull(rightId, "leftId can not be null.");
        Collection<AssociationAttributeDTO> attributes = getSqlMapClientTemplate().queryForList(
                                                                                                "resource.geAssociationWithAttributesByRightId",
                                                                                                rightId);
        Map<Long, AssociationEntity> associations = mergeAssociationAttributes(attributes);
        return associations.values();
    }

    private Map<Long, AssociationEntity> mergeAssociationAttributes(Collection<AssociationAttributeDTO> attributes) {
        Map<Long, AssociationEntity> associations = new HashMap<Long, AssociationEntity>();
        for (AssociationAttributeDTO dto : attributes) {
            if (!associations.containsKey(dto.getAssociation().getId())) {
                associations.put(dto.getAssociation().getId(), dto.getAssociation());
            }
            if (dto.getAttribute().getId() != null) {
                dto.getAssociation().addAttribute(dto.getAttribute());
            }
        }
        return associations;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Long> getResourceIdsByTypeAndNames(String resourceType, Collection<String> resourceNames)
                                                                                                                throws IllegalArgumentException {
        Assert.hasText(resourceType, "resourceType can not be empty");
        Assert.notNull(resourceNames, "resourceNames can not be null");
        Map<String, Long> ids = new HashMap<String, Long>();
        if (resourceNames.size() > 0) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("resourceType", resourceType);
            map.put("resourceNames", resourceNames.toArray(new String[0]));
            Collection<ResourceEntity> entities = getSqlMapClientTemplate().queryForList(
                                                                                         "resource.getResourceIdsByTypeAndNames",
                                                                                         map);
            for (ResourceEntity entity : entities) {
                ids.put(entity.getResourceName(), entity.getId());
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public ResourceEntity getResourceEntity(String resourceType, String resourceName, boolean loadAttributes)
                                                                                                             throws IllegalArgumentException {
        Assert.hasText(resourceType, "resourceType can not be empty.");
        Assert.hasText(resourceName, "resourceName can not be empty.");
        Assert.notNull(resourceTypeService.getResourceType(resourceType), "resourceType does not exist.");
        Map<String, String> map = new HashMap<String, String>();
        map.put("resourceType", resourceType);
        map.put("resourceName", resourceName);
        ResourceEntity resourceEntity = null;
        if (loadAttributes) {
            Collection<ResourceAttributeDTO> attributes = getSqlMapClientTemplate().queryForList(
                                                                                                 "resource.geResourceWithAttributes",
                                                                                                 map);
            for (ResourceAttributeDTO dto : attributes) {
                if (resourceEntity == null) {
                    resourceEntity = dto.getResource();
                    resourceEntity.setResourceType(resourceType);
                    resourceEntity.setResourceName(resourceName);
                }
                if (dto.getAttribute().getId() != null) {
                    resourceEntity.addAttribute(dto.getAttribute());
                }
            }
        } else {
            resourceEntity = (ResourceEntity) getSqlMapClientTemplate().queryForObject("resource.getResourceEntity",
                                                                                       map);
        }
        return resourceEntity;
    }

    private void validateAttributes(AttributedDomainEntity resourceEntity) {
        for (AttributeEntity attr : resourceEntity.getAttributes()) {
            Assert.hasText(attr.getKey(), "attribute's key can not be empty");
            Assert.hasText(attr.getValue(), "attribute's value can not be empty");
        }
    }

    private int addOrUpdateAttributes(AttributedDomainEntity attributedEntity, Collection<AttributeEntity> existedAttrs)
                                                                                                                        throws IllegalArgumentException {
        int impacted = 0;
        // 1, assign the attributes' ownerId to resourceEntity's id
        for (AttributeEntity attr : attributedEntity.getAttributes()) {
            attr.setOwnerId(attributedEntity.getId());
        }
        // 2, calculate the removed,updated and added attributes
        Collection<Long> removedAttrs = new HashSet<Long>();
        Collection<AttributeEntity> updatedAttrs = new HashSet<AttributeEntity>();
        Collection<AttributeEntity> addedAttrs = new HashSet<AttributeEntity>();
        addedAttrs.addAll(attributedEntity.getAttributes());
        if (existedAttrs != null) {
            for (AttributeEntity attr : existedAttrs) {
                AttributeEntity inAttr = attributedEntity.getAttribute(attr.getKey());
                if (inAttr == null) {
                    removedAttrs.add(attr.getId());
                } else {
                    addedAttrs.remove(inAttr);
                    if (!attr.getValue().equals(inAttr.getValue())) {
                        inAttr.setId(attr.getId());
                        updatedAttrs.add(inAttr);
                    }
                }
            }
            Long[] removed = removedAttrs.toArray(new Long[0]);
            AttributeEntity[] added = addedAttrs.toArray(new AttributeEntity[0]);
            for (int i = 0; i < removed.length && i < added.length; i++) {
                added[i].setId(removed[i]);
                updatedAttrs.add(added[i]);
                removedAttrs.remove(removed[i]);
                addedAttrs.remove(added[i]);
            }
        }
        // 3, remove the unused attributes
        impacted += removeAttrsByAttrIds(removedAttrs);
        // 4, add the new added attributes
        impacted += batchUpdateEntities(addedAttrs.toArray(new AttributeEntity[0]), "resource.addAttributeEntity");

        // 5, update the changed attributes
        impacted += batchUpdateEntities(updatedAttrs.toArray(new AttributeEntity[0]), "resource.updateAttributeEntity");
        return impacted;
    }

    private int removeAttrsByAttrIds(Collection<Long> attrIds) throws IllegalArgumentException {
        if (attrIds != null && attrIds.size() > 0) {
            return getSqlMapClientTemplate().update("resource.removeAttrsByAttrIds", attrIds.toArray(new Long[0]));
        }
        return 0;
    }

    @Transactional
    public void removeNonHistoricResourcesByNames(String resourceType, Collection<String> resourceNames,
                                                  String clientAddress, String serverAddress)
                                                                                             throws IllegalArgumentException {
        Assert.hasText(resourceType, "resourceType can not be empty.");
        Assert.notNull(resourceNames, "resourceNames can not be empty.");
        if (resourceNames.size() > 0) {
            ResourceType type = resourceTypeService.getResourceType(resourceType);
            Assert.notNull(type, "can not find resourceType:" + resourceType + ".");
            Assert.isTrue(!type.getKeepHistoric(), "historic resource[type=" + resourceType + ",name=" + resourceNames
                                                   + "] can not be removed.");
            Map<String, Long> resourceIds = getResourceIdsByTypeAndNames(resourceType, resourceNames);
            removeResourcesByIds(resourceIds.values());
            {
                if (clientAddress == null) {
                    clientAddress = ResourceChanges.NO_CLIENT_ADDRESS;
                }
                if (serverAddress == null) {
                    serverAddress = ResourceChanges.NO_SERVER_ADDRESS;
                }
                Collection<ResourceChanges> changes = new HashSet<ResourceChanges>();
                for (String resourceName : resourceNames) {
                    ResourceChanges change = new ResourceChanges();
                    change.setChanges(ResourceChanges.CHANGES_DELETED);
                    change.setResourceType(resourceType);
                    change.setResourceId(resourceName);
                    change.setAcceptOwnerDiedEvent(type.getOnlineResource());
                    change.setClientAddress(clientAddress);
                    change.setServerAddress(serverAddress);
                    changes.add(change);
                }
                batchUpdateEntities(changes.toArray(new ResourceChanges[0]), "resource.addOrUpdateResourceChanges");
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("resourceType", resourceType);
                map.put("resourceNames", resourceNames.toArray(new String[0]));
                getSqlMapClientTemplate().update("resource.updateAssociationChangesToDeletedByResourceTypeAndNames",
                                                 map);
            }
        }
    }
}
