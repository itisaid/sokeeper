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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sokeeper.domain.DomainEntity;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class AttributedDomainEntity extends DomainEntity {
    private static final long            serialVersionUID = -1402711058283845765L;
    private Map<String, AttributeEntity> attributes       = new HashMap<String, AttributeEntity>();

    public Set<String> attributeKeys() {
        return attributes.keySet();
    }

    public boolean containsAttribute(String attrKey) {
        Assert.hasText(attrKey, "attrKey can not be empty.");
        return attributes.containsKey(attrKey);
    }

    public AttributeEntity removeAttribute(String attrKey) {
        Assert.hasText(attrKey, "attrKey can not be empty.");
        return attributes.remove(attrKey);
    }

    public void addAttributes(Collection<AttributeEntity> attributes) {
        Assert.notNull(attributes, "attributes can not be null.");
        for (AttributeEntity attr : attributes) {
            Assert.hasText(attr.getKey(), "attribute's key can not be empty.");
            Assert.hasText(attr.getValue(), "attribute's value can not be empty.");
            this.attributes.put(attr.getKey(), attr);
        }
    }

    public void addAttribute(AttributeEntity attrEntity) {
        Assert.notNull(attrEntity, "attrEntity can not be null.");
        Assert.hasText(attrEntity.getKey(), "attrEntity.key can not be empty.");
        attributes.put(attrEntity.getKey(), attrEntity);
    }

    public AttributeEntity getAttribute(String attrKey) {
        Assert.hasText(attrKey, "attrKey can not be empty.");
        return attributes.get(attrKey);
    }

    public Collection<AttributeEntity> getAttributes() {
        return attributes.values();
    }
}
