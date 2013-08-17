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
package com.sokeeper.server;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.cache.support.CacheLRU;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.server.ResourceAccessHandlerImpl;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceAccessHandlerImplTest extends BaseTestCase {

    @Autowired
    private ChangesService    changesService;

    @Autowired
    private ResourceService   resourceService;

    @Autowired
    private SubscriberService subscriberService;

    public void test_addOrUpdateResource() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();

        try {
            impl.addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}), null,
                    null);
            fail("the rpc connection could not be null.");
        } catch (IllegalArgumentException e) {
        }
        initializeRpcConnection();
        impl
                .addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}), null,
                        null);
        assertNotNull(impl.getResourceEntity(new ResourceKey("t1", "r1")));
        assertNotNull(impl.subscribe(new ResourceKey("t1", "r1")));
        assertNotNull(impl.getResourceService());
        assertEquals(impl.getCurrentSequenceOfChanges(), new Long(1));
        impl.removeResource(new ResourceKey("t1", "r1"));
        impl.getResourcesCache().clear();
        assertNull(impl.getResourceEntity(new ResourceKey("t1", "r1")));
        destroyRpcConnection();
    }

    public void test_addOrUpdateAssociation() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();
        initializeRpcConnection();
        impl
                .addOrUpdateResource(resource("t1", "l1", new String[] {}, new String[] {}), null,
                        null);
        impl
                .addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}), null,
                        null);
        assertNotNull(impl.addOrUpdateAssociation(new ResourceKey("t1", "l1"), new ResourceKey(
                "t1", "r1"), null));
        assertNotNull(impl.getAssociationEntity(new ResourceKey("t1", "l1"), new ResourceKey("t1",
                "r1")));
        destroyRpcConnection();
    }

    public void test_removeAssociation() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();
        initializeRpcConnection();
        impl
                .addOrUpdateResource(resource("t1", "l1", new String[] {}, new String[] {}), null,
                        null);
        impl
                .addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}), null,
                        null);
        assertNotNull(impl.addOrUpdateAssociation(new ResourceKey("t1", "l1"), new ResourceKey(
                "t1", "r1"), null));
        impl.removeAssociation(new ResourceKey("t1", "l1"), new ResourceKey("t1", "r1"));
        assertNull(impl.getAssociationEntity(new ResourceKey("t1", "l1"), new ResourceKey("t1",
                "r1")));
        destroyRpcConnection();
    }

    private void destroyRpcConnection() {
        RpcConnection.setCurrentRpcConnection(null);
    }

    private void initializeRpcConnection() {
        RpcConnection.setCurrentRpcConnection(new RpcConnection(RpcSocketAddress
                .fromFullAddress("127.0.0.1:8010"), RpcSocketAddress
                .fromFullAddress("127.0.0.1:9010")));
    }

    private ResourceAccessHandlerImpl initializeHandler() {
        ResourceAccessHandlerImpl impl = new ResourceAccessHandlerImpl();
        impl.setChangesService(changesService);
        impl.setResourceService(resourceService);
        impl.setSubscriberService(subscriberService);
        impl.setResourcesCache(new CacheLRU<ResourceKey, ResourceEntity>(100));
        registerResourceType("t1", true);
        return impl;
    }

    public void test_unsubscribe() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();
        try {
            impl.subscribe(new ResourceKey("t1", "r1"));
            fail("the rpc connection could not be null.");
        } catch (IllegalArgumentException e) {
        }
        initializeRpcConnection();
        assertNull(impl.subscribe(new ResourceKey("t1", "r1")));
        assertEquals(subscriberService.getSubscribedResources("127.0.0.1:9010", "127.0.0.1:8010")
                .size(), 1);
        try {
            impl.unsubscribe(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            impl.unsubscribe(new ResourceKey("t1", null));
            fail();
        } catch (IllegalArgumentException e) {
        }
        impl.unsubscribe(new ResourceKey("t1", "r1"));
        assertEquals(subscriberService.getSubscribedResources("127.0.0.1:9010", "127.0.0.1:8010")
                .size(), 0);
        destroyRpcConnection();
    }

    public void test_subscribe_in_batch() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();
        initializeRpcConnection();
        impl.subscribe(Arrays.asList(new ResourceKey("t1", "r1"), new ResourceKey("t1", "r2")));
        assertEquals(subscriberService.getSubscribedResources("127.0.0.1:9010", "127.0.0.1:8010")
                .size(), 2);
        destroyRpcConnection();
    }

    public void test_subscribeLostEvents() throws Throwable {
        ResourceAccessHandlerImpl impl = initializeHandler();
        initializeRpcConnection();

        try {
            impl.getLostEvents(null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        impl
                .addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}), null,
                        null);
        impl
                .addOrUpdateResource(resource("t1", "r2", new String[] {}, new String[] {}), null,
                        null);
        assertEquals(impl.getLostEvents(100L).size(), 0);
        assertEquals(impl.getLostEvents(0L).size(), 2);
        destroyRpcConnection();
    }
}
