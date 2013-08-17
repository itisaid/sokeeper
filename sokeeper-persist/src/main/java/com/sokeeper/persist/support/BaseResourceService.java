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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceType;
import com.sokeeper.exception.PersistLayerException;
import com.sokeeper.persist.service.ResourceTypeService;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class BaseResourceService extends SqlMapClientDaoSupport {
    @Autowired
    private ResourceTypeService resourceTypeService;

    protected int addOrUpdateResourceChanges(String resourceType, String resourceId,
                                             String changes, String clientAddress,
                                             String serverAddress) {
        Assert.hasText(clientAddress, "clientAddress can not be empty.");
        Assert.hasText(serverAddress, "clientAddress can not be empty.");
        Assert.hasText(changes, "changes can not be null.");
        Assert.hasText(resourceType, "resourceType can not be empty.");
        Assert.hasText(resourceId, "resourceId can not be empty.");
        ResourceChanges resourceChanges = new ResourceChanges();
        resourceChanges.setResourceType(resourceType);
        resourceChanges.setResourceId(resourceId);
        resourceChanges.setClientAddress(clientAddress);
        resourceChanges.setServerAddress(serverAddress);
        resourceChanges.setChanges(changes);
        ResourceType type = resourceTypeService.getResourceType(resourceType);
        if (type != null) {
            resourceChanges.setAcceptOwnerDiedEvent(type.getOnlineResource());
        }
        return getSqlMapClientTemplate().update("resource.addOrUpdateResourceChanges",
                resourceChanges);
    }

    protected void removeAssociationByIds(Collection<Long> associationIds)
            throws IllegalArgumentException {
        if (associationIds != null && associationIds.size() > 0) {
            getSqlMapClientTemplate().update("resource.removeAssociationByIds",
                    associationIds.toArray(new Long[0]));
        }
    }

    protected void removeAttrsByOwnerIds(Collection<Long> ownerIds) throws IllegalArgumentException {
        if (ownerIds != null && ownerIds.size() > 0) {
            getSqlMapClientTemplate().update("resource.removeAttrsByOwnerIds",
                    ownerIds.toArray(new Long[0]));
        }
    }

    @SuppressWarnings("unchecked")
    public int removeResourcesByIds(Collection<Long> resourceIds) throws IllegalArgumentException {
        Assert.notNull(resourceIds, "resourceIds can not be null.");
        int impacted = 0;
        if (resourceIds.size() > 0) {
            Collection<Long> associationIds = getSqlMapClientTemplate().queryForList(
                    "resource.getAssociationIdsByResourceIds", resourceIds.toArray(new Long[0]));
            // 1, remove attributes
            HashSet<Long> ownerIds = new HashSet<Long>();
            ownerIds.addAll(resourceIds);
            ownerIds.addAll(associationIds);
            removeAttrsByOwnerIds(ownerIds);
            // 2, remove associations
            removeAssociationByIds(associationIds);
            // 3, remove resources
            impacted = getSqlMapClientTemplate().update("resource.removeNonHistoricResourceByIds",
                    resourceIds.toArray(new Long[0]));
        }
        return impacted;
    }

    final private int DEFAULT_BATCH_SIZE = 20;

    protected int batchUpdateEntities(Object[] entities, String updateClause)
            throws PersistLayerException {
        int impacted = 0;
        if (entities != null && entities.length > 0) {
            int batchNum = (entities.length + DEFAULT_BATCH_SIZE - 1) / DEFAULT_BATCH_SIZE;
            try {
                for (int currentBatch = 0; currentBatch < batchNum; currentBatch++) {
                    getSqlMapClient().startBatch();
                    int beginIndex = currentBatch * DEFAULT_BATCH_SIZE;
                    int endIndex = (currentBatch + 1) * DEFAULT_BATCH_SIZE;
                    endIndex = endIndex > entities.length ? entities.length : endIndex;
                    for (int i = beginIndex; i < endIndex; i++) {
                        impacted += getSqlMapClientTemplate().update(updateClause, entities[i]);
                    }
                    getSqlMapClient().executeBatch();
                }
            } catch (SQLException e) {
                throw new PersistLayerException(
                        "batchUpdateEntities:[" + updateClause + "]failed.", e);
            }
        }
        return impacted;
    }

}
