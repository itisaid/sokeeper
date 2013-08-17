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
package com.sokeeper.client;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sokeeper.client.Client;
import com.sokeeper.client.listener.ResourceEventListener;
import com.sokeeper.client.listener.RpcIoEventListener;
import com.sokeeper.domain.AssociationChanges;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.RpcException;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ClientImplTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_startup() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        assertNotNull(client.getResourceHandler());
        assertNotNull(client.getClientIoHandler());
        client.shutdown();
    }

    private Client client(int serverPort, boolean autoReconnect) {
        Client client = new Client();
        client.setResourceAccessHandler(new MockResourceAccessHandler());
        client.setServerUrl("tcp://localhost:" + serverPort + "/client"
                + (autoReconnect ? "?auto_reconnect_ms=1000" : ""));
        return client;
    }

    private Map<RpcAddress, Collection<ChangesEvent>> prepareAssociationChanges(
                                                                                String clientAddress,
                                                                                String[] leftTypes,
                                                                                String[] leftIds,
                                                                                String[] rightTypes,
                                                                                String[] rightIds,
                                                                                String[] changes,
                                                                                Long sequence) {
        Map<RpcAddress, Collection<ChangesEvent>> map = new HashMap<RpcAddress, Collection<ChangesEvent>>();
        Collection<ChangesEvent> resourceChanges = new HashSet<ChangesEvent>();
        RpcAddress addr = RpcSocketAddress.fromFullAddress(clientAddress);
        for (int i = 0; i < rightTypes.length; i++) {
            AssociationChangesEvent event = new AssociationChangesEvent();
            event.setChanges(changes[i]);
            event.setLeftId(leftIds[i]);
            event.setLeftType(leftTypes[i]);
            event.setRightId(rightIds[i]);
            event.setRightType(rightTypes[i]);
            event.setSequence(sequence);
            resourceChanges.add(event);
        }
        map.put(addr, resourceChanges);
        return map;
    }

    private Map<RpcAddress, Collection<ChangesEvent>> prepareResourceChanges(
                                                                             String clientAddress,
                                                                             String[] resourceTypes,
                                                                             String[] resourceIds,
                                                                             String[] changes,
                                                                             Long sequence) {
        Map<RpcAddress, Collection<ChangesEvent>> map = new HashMap<RpcAddress, Collection<ChangesEvent>>();
        Collection<ChangesEvent> resourceChanges = new HashSet<ChangesEvent>();
        RpcAddress addr = RpcSocketAddress.fromFullAddress(clientAddress);
        for (int i = 0; i < resourceTypes.length; i++) {
            ResourceChangesEvent event = new ResourceChangesEvent();
            event.setChanges(changes[i]);
            event.setResourceId(resourceIds[i]);
            event.setResourceType(resourceTypes[i]);
            event.setSequence(sequence);
            resourceChanges.add(event);
        }
        map.put(addr, resourceChanges);
        return map;
    }

    @Test
    public void test_onResourcesChanged_cacheLRU_will_be_cleanup() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();

        ResourceKey key = new ResourceKey("t1", "r1");
        client.getResourcesCache().put(key, new ResourceEntity());
        assertTrue(client.getResourcesCache().containsKey(key));
        client.onResourcesChanged(prepareResourceChanges("localhost:8010", new String[] { "t1" },
                new String[] { "r1" }, new String[] { ResourceChanges.CHANGES_DELETED }, 8L));
        assertFalse(client.getResourcesCache().containsKey(key));
        assertEquals(client.getVisitedSequenceOfChanges(), new Long(8));
        client.shutdown();
    }

    @Test
    public void test_onAssociationChanged() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        Map<RpcAddress, Collection<ChangesEvent>> changes = prepareAssociationChanges(
                "localhost:8010", new String[] { "t1" }, new String[] { "L1" },
                new String[] { "t1" }, new String[] { "R1" },
                new String[] { AssociationChanges.CHANGES_CREATED }, 5L);
        client.onResourcesChanged(changes);
        assertEquals(client.getVisitedSequenceOfChanges(), new Long(5));
        client.shutdown();
    }

    @Test
    public void test_getCurrentSequenceOfChanges() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        client.setResourceAccessHandler(new MockResourceAccessHandler() {
            public Long getCurrentSequenceOfChanges() {
                return 5L;
            }
        });
        assertEquals(client.getResourceHandler().getCurrentSequenceOfChanges(), 5L);
        client.shutdown();
    }

    @Test
    public void test_subscribe() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        client.setResourceAccessHandler(new MockResourceAccessHandler() {
            public ResourceEntity subscribe(ResourceKey resourceKey) throws RpcException {
                ResourceEntity resourceEntity = new ResourceEntity();
                resourceEntity.setResourceName(resourceKey.getResourceName());
                resourceEntity.setResourceType(resourceKey.getResourceType());
                return resourceEntity;
            }
        });
        assertNotNull(client.subscribe(new ResourceKey("t1", "r1"), null));
        assertTrue(client.getResourcesCache().containsKey(new ResourceKey("t1", "r1")));
        client.shutdown();
    }

    @Test
    public void test_unsubscribe() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        ResourceKey resourceKey = new ResourceKey("t1", "r1");
        ResourceEventListener listener1 = new ResourceEventListener() {

            public void onAssociationChanged(AssociationChangesEvent event) {
            }

            public void onResourceChanged(ResourceChangesEvent event) {
            }
        };
        ResourceEventListener listener2 = new ResourceEventListener() {

            public void onAssociationChanged(AssociationChangesEvent event) {
            }

            public void onResourceChanged(ResourceChangesEvent event) {
            }
        };

        final AtomicBoolean unsubscribeCalled = new AtomicBoolean(false);
        client.setResourceAccessHandler(new MockResourceAccessHandler() {
            public void unsubscribe(ResourceKey resourceKey) throws RpcException {
                unsubscribeCalled.set(true);
            }
        });
        // 1, no listener subscribed the resource
        client.unsubscribe(resourceKey, listener1);
        assertFalse(
                "if no listener subscribed the resources, should not trigger call server's unsubscribe",
                unsubscribeCalled.get());
        // 2, more than one subscribers subscribed same resource,when unsubscribe listener 1 should not call remote un-subscribe
        client.subscribe(resourceKey, listener1);
        client.subscribe(resourceKey, listener2);
        client.unsubscribe(resourceKey, listener2);
        assertFalse(
                "if more than listeners subscribed the resources, when unsubscribe one listener should not trigger call server's unsubscribe",
                unsubscribeCalled.get());
        client.unsubscribe(resourceKey, listener1);
        assertTrue("if no listners subscribed the resource, should call server's unsubscribe",
                unsubscribeCalled.get());

        client.shutdown();

    }

    @Test
    public void test_addOrUpdateResource() throws Throwable {
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        client.startup();
        client.setResourceAccessHandler(new MockResourceAccessHandler() {
            public ResourceEntity addOrUpdateResource(
                                                      ResourceEntity resourceEntity,
                                                      String rightResourceType,
                                                      Map<String, AssociationEntity> rightAssociations)
                    throws RpcException {
                return resourceEntity;
            }
        });
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceName("r1");
        resourceEntity.setResourceType("t1");
        assertNotNull(client.addOrUpdateResource(resourceEntity, null, null));
        client.shutdown();
    }

    @Test
    public void test_usablity() throws Throwable {
        // step 1: initialize client with serverUrl
        Client client = client(NetUtils.selectAvailablePort(9010), false);
        // step 2: register the resource listeners, this step can be done before the client started or after client started
        client.subscribe(new ResourceKey("t1", "l1"), new ResourceEventListener() {
            public void onAssociationChanged(AssociationChangesEvent event) {
            }

            public void onResourceChanged(ResourceChangesEvent event) {
            }
        });
        client.subscribe(new ResourceKey("t1", "r1"), new ResourceEventListener() {
            public void onAssociationChanged(AssociationChangesEvent event) {
            }

            public void onResourceChanged(ResourceChangesEvent event) {
            }
        });
        // step 3: register IO event listeners:this step can be done before the client started or after the client started
        client.registIoListener(new RpcIoEventListener() {
            public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
            }

            public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
            }
        });
        // step 4: startup the client
        client.startup();
        // step 5: the user can perform the operations they want
        client.getResourceEntity(new ResourceKey("t1", "r1"));
        // step 6: during the runtime, the user will be notified by IO events
        RpcConnection connection = new RpcConnection(RpcSocketAddress
                .fromFullAddress("localhost:8010"), RpcSocketAddress
                .fromFullAddress("localhost:9010"));
        client.onConnectionCreated(connection, client.getClientIoHandler());
        client.onConnectionClosed(connection, client.getClientIoHandler());
        // step 7: during the runtime the user will be notified by the resource events
        client.onResourcesChanged(prepareResourceChanges("localhost:8010", new String[] { "t1" },
                new String[] { "r1" }, new String[] { ResourceChanges.CHANGES_DELETED }, 8L));
        // step 8: during the runtime the user will be notified by the association events
        Map<RpcAddress, Collection<ChangesEvent>> changes = prepareAssociationChanges(
                "localhost:8010", new String[] { "t1" }, new String[] { "l1" },
                new String[] { "t1" }, new String[] { "r1" },
                new String[] { AssociationChanges.CHANGES_CREATED }, 5L);
        client.onResourcesChanged(changes);
        client.removeResource(new ResourceKey("t1", "r1"));
        client.addOrUpdateAssociation(new ResourceKey("t1", "l1"), new ResourceKey("t1", "r1"),
                null);
        client.removeAssociation(new ResourceKey("t1", "l1"), new ResourceKey("t1", "r1"));
        client.getAssociationEntity(new ResourceKey("t1", "l1"), new ResourceKey("t1", "r1"));

        // step 9: the user can shutdown the client
        client.shutdown();
        // step 10: if we startup client again, the threadPool will be re-initialized
        client.startup();
        client.shutdown();

    }

}
