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

import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sokeeper.domain.ResourceType;
import com.sokeeper.persist.service.ResourceTypeService;
import com.sokeeper.util.Assert;

@Component("resourceTypeService")
public class ResourceTypeServiceImpl extends SqlMapClientDaoSupport implements ResourceTypeService {

    @Transactional
    public void registerResourceType(ResourceType resourceType) throws IllegalArgumentException {
        Assert.notNull(resourceType, "resourceType can not be null.");
        Assert.hasText(resourceType.getTypeName(), "resourceType.typeName can not be empty");
        getSqlMapClientTemplate().insert("resource.registerResourceType", resourceType);
    }

    public ResourceType getResourceType(String typeName) throws IllegalArgumentException {
        Assert.hasText(typeName, "typeName can not be empty");
        return (ResourceType) getSqlMapClientTemplate().queryForObject("resource.getResourceType",
                typeName);
    }

    public void flushCachedResourceTypes() {
        getSqlMapClientTemplate().queryForObject("resource.flushCachedResourceTypes");
    }
}
