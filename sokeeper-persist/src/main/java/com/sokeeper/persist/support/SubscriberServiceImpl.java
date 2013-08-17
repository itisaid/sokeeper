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

import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.stereotype.Component;

import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("subscriberService")
public class SubscriberServiceImpl extends SqlMapClientDaoSupport implements SubscriberService {
    public int removeSubscriber(ResourceKey resourceKey, String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        ResourceKey.validate(resourceKey);
        Assert.hasText(clientAddress, "clientAddress can not be null.");
        Assert.hasText(serverAddress, "serverAddress can not be null.");
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        map.put("resourceKey", resourceKey);
        return getSqlMapClientTemplate().delete("subscribe.removeSubscriberFromGivenResource", map);
    }

    public int removeSubscriber(String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        Assert.hasText(clientAddress, "clientAddress can not be null.");
        Assert.hasText(serverAddress, "serverAddress can not be null.");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        return getSqlMapClientTemplate().delete("subscribe.removeSubscriber", map);
    }

    public void addResourceChangesSubscriber(ChangesSubscriber resourceChangesSubscriber)
            throws IllegalArgumentException {
        Assert.notNull(resourceChangesSubscriber, "resourceChangesSubscriber can not be null.");
        Assert.hasText(resourceChangesSubscriber.getResourceType(),
                "resourceChangesSubscriber.resourceType can not be empty.");
        Assert.hasText(resourceChangesSubscriber.getResourceId(),
                "resourceChangesSubscriber.resourceId can not be empty.");
        Assert.hasText(resourceChangesSubscriber.getClientAddress(),
                "resourceChangesSubscriber.clientAddress can not be empty.");
        Assert.hasText(resourceChangesSubscriber.getServerAddress(),
                "resourceChangesSubscriber.serverAddress can not be empty.");
        getSqlMapClientTemplate().insert("subscribe.addChangesSubscriber",
                resourceChangesSubscriber);
    }

    public void addResourceChangesSubscriber(String resourceType, String resourceId,
                                             String clientAddress, String serverAddress)
            throws IllegalArgumentException {
        ChangesSubscriber subscriber = new ChangesSubscriber();
        subscriber.setClientAddress(clientAddress);
        subscriber.setServerAddress(serverAddress);
        subscriber.setResourceType(resourceType);
        subscriber.setResourceId(resourceId);
        addResourceChangesSubscriber(subscriber);
    }

    @SuppressWarnings("unchecked")
    public Collection<ChangesSubscriber> getSubscribedResources(String clientAddress,
                                                                String serverAddress)
            throws IllegalArgumentException {
        Assert.hasText(clientAddress, "clientAddress can not be null.");
        Assert.hasText(serverAddress, "serverAddress can not be null.");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("clientAddress", clientAddress);
        map.put("serverAddress", serverAddress);
        return (Collection<ChangesSubscriber>) getSqlMapClientTemplate().queryForList(
                "subscribe.getSubscribedResources", map);
    }

    public int removeSubscribersOfServers(Collection<String> serverAddresses)
            throws IllegalArgumentException {
        Assert.notNull(serverAddresses, "serverAddresses can not be null.");
        int impacted = 0;
        if (serverAddresses.size() > 0) {
            impacted = getSqlMapClientTemplate().delete("subscribe.removeSubscribersOfServers",
                    serverAddresses.toArray(new String[0]));
        }
        return impacted;
    }

}
