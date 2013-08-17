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
package com.sokeeper.domain.resource;

import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceEntity extends AttributedDomainEntity {

    private static final long serialVersionUID = 1608532232917783729L;

    public static final Long  INITIAL_VERSION  = 1L;

    private ResourceKey       resourceKey      = new ResourceKey();
    private String            description;
    private Long              version          = INITIAL_VERSION;

    public static void validate(ResourceEntity resourceEntity) {
        Assert.notNull(resourceEntity, "resourceEntity can not be null.");
        ResourceKey.validate(resourceEntity.resourceKey);
    }

    public Long getVersion() {
        return version;
    }

    public ResourceKey getResourceKey() {
        return resourceKey;
    }

    public void setVersion(Long version) {
        Assert.notNull(version, "version can not be null.");
        this.version = version;
    }

    public String getResourceType() {
        return resourceKey.getResourceType();
    }

    public void setResourceType(String type) {
        resourceKey.setResourceType(type);
    }

    public String getResourceName() {
        return resourceKey.getResourceName();
    }

    public void setResourceName(String name) {
        resourceKey.setResourceName(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
