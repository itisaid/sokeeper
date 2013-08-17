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

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
import java.io.Serializable;

import com.sokeeper.util.Assert;

public class ResourceKey implements Serializable {

    private static final long serialVersionUID = -7208511448495190596L;
    private String            resourceType;
    private String            resourceName;

    public static void validate(ResourceKey resourceKey) {
        Assert.notNull(resourceKey, "resourceKey can not be null.");
        Assert.hasText(resourceKey.getResourceName(), "resourceKey.resourceName can not be empty.");
        Assert.hasText(resourceKey.getResourceType(), "resourceKey.resourceType can not be empty.");
    }

    public ResourceKey(String resourceType, String resourceName) {
        Assert.hasText(resourceType, "resourceType can not be empty.");
        Assert.hasText(resourceName, "resourceName can not be empty.");
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public ResourceKey() {

    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceType(String resourceType) {
        Assert.hasText(resourceType, "resourceType can not be empty.");
        this.resourceType = resourceType;
    }

    public void setResourceName(String resourceName) {
        Assert.hasText(resourceName, "resourceName can not be empty.");
        this.resourceName = resourceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
        result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceKey other = (ResourceKey) obj;
        if (resourceName == null) {
            if (other.resourceName != null)
                return false;
        } else if (!resourceName.equals(other.resourceName))
            return false;
        if (resourceType == null) {
            if (other.resourceType != null)
                return false;
        } else if (!resourceType.equals(other.resourceType))
            return false;
        return true;
    }

}
