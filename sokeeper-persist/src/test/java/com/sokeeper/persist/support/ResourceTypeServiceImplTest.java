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

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.ResourceType;
import com.sokeeper.persist.service.ResourceTypeService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceTypeServiceImplTest extends BaseTestCase {

    @Autowired
    private ResourceTypeService resourceTypeService;

    public void test_flushCachedResourceTypes() throws Throwable {
        cleanupResourceTypes();
        registerResourceType("t1", true);
        // when we call getResourceType should let the persistence layer cache it.
        assertNotNull(resourceTypeService.getResourceType("t1"));
        cleanupResourceTypes();
        assertNotNull(resourceTypeService.getResourceType("t1"));
        // if we call the flushResourceTypes will trigger the cache be flushed
        resourceTypeService.flushCachedResourceTypes();
        assertNull(resourceTypeService.getResourceType("t1"));
    }

    public void test_registerResourceType() throws Throwable {
        cleanupResourceTypes();
        ResourceType resourceType = new ResourceType();
        try {
            resourceTypeService.registerResourceType(resourceType);
            fail();
        } catch (IllegalArgumentException e) {
        }
        resourceType.setTypeName("scm.configuration");
        resourceTypeService.registerResourceType(resourceType);
        assertEquals(resourceTypeService.getResourceType("scm.configuration").getOnlineResource(),
                new Boolean(false));
        // once the resourceType is registered,we can not update it.
        resourceType.setOnlineResource(true);
        resourceTypeService.registerResourceType(resourceType);
        assertEquals(resourceTypeService.getResourceType("scm.configuration").getOnlineResource(),
                new Boolean(false));
        // create a parastical resource type
        ResourceType parasticalType = new ResourceType();
        parasticalType.setTypeName("service.provider");
        parasticalType.setOnlineResource(true);
        resourceTypeService.registerResourceType(parasticalType);
        assertEquals(resourceTypeService.getResourceType("service.provider").getOnlineResource(),
                new Boolean(true));
    }
}
