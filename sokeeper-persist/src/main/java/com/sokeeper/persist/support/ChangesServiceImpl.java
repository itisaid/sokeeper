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

import java.util.Collection;
import java.util.HashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("changesService")
public class ChangesServiceImpl extends BaseResourceService implements ChangesService {

    @SuppressWarnings("unchecked")
    @Transactional
    public int updateStatusToOwnerDied(String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        Assert.hasText(clientAddress, "clientAddress can not be empty.");
        Assert.hasText(serverAddress, "serverAddress can not be empty.");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        int impacted = getSqlMapClientTemplate().update(
                "resource.updateResourceChangesToOwnerDied", map);
        if (impacted > 0) {
            Collection<Long> resourceIds = getSqlMapClientTemplate().queryForList(
                    "resource.getOwnerDiedResourceIdsByOwnerAddress", map);
            if (resourceIds.size() > 0) {
                removeResourcesByIds(resourceIds);
                getSqlMapClientTemplate().update("resource.updateAssociationChangesToOwnerDied",
                        map);
            }
        }
        return impacted;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public int updateStatusToServerDied(Collection<String> diedServerAddresses)
            throws IllegalArgumentException {
        Assert.notNull(diedServerAddresses, "diedServerAddresses can not be null.");
        int impacted = 0;
        String[] servers = diedServerAddresses.toArray(new String[0]);
        if (servers.length > 0) {
            impacted = getSqlMapClientTemplate().update(
                    "resource.updateResourceChangesToServerDied", servers);
            if (impacted > 0) {
                Collection<Long> resourceIds = getSqlMapClientTemplate().queryForList(
                        "resource.getOwnerDiedResourceIdsByServerAddresses", servers);
                if (resourceIds.size() > 0) {
                    removeResourcesByIds(resourceIds);
                    getSqlMapClientTemplate().update(
                            "resource.updateAssociationChangesToServerDied", servers);
                }
            }
        }
        return impacted;
    }

    public Long getCurrentSequenceOfChanges() {
        return (Long) getSqlMapClientTemplate().queryForObject(
                "resource.getCurrentSequenceOfChanges");
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceChangesEvent> listResourceChangesEvent(Long sequenceFrom,
                                                                     Long sequenceTo,
                                                                     String clientAddress,
                                                                     String serverAddress)
            throws IllegalArgumentException {
        Assert.isTrue(sequenceFrom != null && sequenceFrom >= 0, "sequenceFrom should >= 0");
        Assert.isTrue(sequenceTo != null && sequenceTo >= sequenceFrom,
                "sequenceTo should >= sequenceFrom");
        Assert.hasText(clientAddress, "clientAddress can not be empty.");
        Assert.hasText(serverAddress, "serverAddress can not be empty.");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("sequenceFrom", sequenceFrom);
        map.put("sequenceTo", sequenceTo);
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        return getSqlMapClientTemplate().queryForList(
                "resource.listResourceChangesEventByClientAddress", map);
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceChangesEvent> listResourceChangesEvent(Long sequenceFrom,
                                                                     Long sequenceTo,
                                                                     Collection<String> servers)
            throws IllegalArgumentException {
        Assert.isTrue(sequenceFrom != null && sequenceFrom >= 0, "sequenceFrom should >= 0");
        Assert.isTrue(sequenceTo != null && sequenceTo >= sequenceFrom,
                "sequenceTo should >= sequenceFrom");
        Assert.notEmpty(servers, "servers can not be empty.");
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("sequenceFrom", sequenceFrom);
        map.put("sequenceTo", sequenceTo);
        map.put("servers", servers.toArray(new String[0]));
        return getSqlMapClientTemplate().queryForList("resource.listResourceChangesEvent", map);
    }

    @SuppressWarnings("unchecked")
    public Collection<AssociationChangesEvent> listAssociationChangesEvent(Long sequenceFrom,
                                                                           Long sequenceTo,
                                                                           String clientAddress,
                                                                           String serverAddress)
            throws IllegalArgumentException {
        Assert.isTrue(sequenceFrom != null && sequenceFrom >= 0, "sequenceFrom should >= 0");
        Assert.isTrue(sequenceTo != null && sequenceTo >= sequenceFrom,
                "sequenceTo should >= sequenceFrom");
        Assert.hasText(clientAddress, "clientAddress can not be empty.");
        Assert.hasText(serverAddress, "serverAddress can not be empty.");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("sequenceFrom", sequenceFrom);
        map.put("sequenceTo", sequenceTo);
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        return getSqlMapClientTemplate().queryForList(
                "resource.listAssociationChangesEventByClientAddress", map);
    }

    @SuppressWarnings("unchecked")
    public Collection<AssociationChangesEvent> listAssociationChangesEvent(
                                                                           Long sequenceFrom,
                                                                           Long sequenceTo,
                                                                           Collection<String> servers)
            throws IllegalArgumentException {
        Assert.isTrue(sequenceFrom != null && sequenceFrom >= 0, "sequenceFrom should >= 0");
        Assert.isTrue(sequenceTo != null && sequenceTo >= sequenceFrom,
                "sequenceTo should >= sequenceFrom");
        Assert.notEmpty(servers, "servers can not be empty.");
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("sequenceFrom", sequenceFrom);
        map.put("sequenceTo", sequenceTo);
        map.put("servers", servers.toArray(new String[0]));
        return getSqlMapClientTemplate().queryForList("resource.listAssociationChangesEvent", map);
    }

    @Transactional
    public int addOrUpdateResourceCreatedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException {
        return addOrUpdateResourceCreatedChanges(resourceType, resourceId,
                ResourceChanges.NO_CLIENT_ADDRESS, ResourceChanges.NO_SERVER_ADDRESS);
    }

    @Transactional
    public int addOrUpdateResourceCreatedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        return addOrUpdateResourceChanges(resourceType, resourceId,
                ResourceChanges.CHANGES_CREATED, clientAddress, serverAddress);
    }

    @Transactional
    public int addOrUpdateResourceRemovedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException {
        return addOrUpdateResourceRemovedChanges(resourceType, resourceId,
                ResourceChanges.NO_CLIENT_ADDRESS, ResourceChanges.NO_SERVER_ADDRESS);
    }

    @Transactional
    public int addOrUpdateResourceRemovedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        return addOrUpdateResourceChanges(resourceType, resourceId,
                ResourceChanges.CHANGES_DELETED, clientAddress, serverAddress);
    }

    @Transactional
    public int addOrUpdateResourceUpdatedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException {
        return addOrUpdateResourceUpdatedChanges(resourceType, resourceId,
                ResourceChanges.NO_CLIENT_ADDRESS, ResourceChanges.NO_SERVER_ADDRESS);
    }

    @Transactional
    public int addOrUpdateResourceUpdatedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        return addOrUpdateResourceChanges(resourceType, resourceId,
                ResourceChanges.CHANGES_UPDATED, clientAddress, serverAddress);
    }

}
