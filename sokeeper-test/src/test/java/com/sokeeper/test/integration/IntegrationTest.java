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
package com.sokeeper.test.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.sokeeper.client.Client;
import com.sokeeper.client.listener.ResourceEventListener;
import com.sokeeper.client.listener.RpcIoEventListener;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.AttributeEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.server.ResourceAccessHandlerImpl;
import com.sokeeper.server.Server;
import com.sokeeper.util.NetUtils;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class IntegrationTest extends BaseTestCase {

    public void test_onAssociationChanged() throws Throwable {
        cleanup();
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare resource type
        registerResourceType("service", false);
        registerResourceType("service.provider", true);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        // 3, prepare provider
        Client provider = client(serverPort, true, 1000 * 1000);
        provider.startup();
        // 4, prepare consumer
        Client consumer = client(serverPort, true, 1000 * 1000);
        consumer.startup();
        // 5, prepare service definition
        ResourceKey serviceKey = new ResourceKey("service", "service.mail");
        // define the mail service
        provider.addOrUpdateResource(resource("service", "service.mail", new String[] {},
                new String[] {}), null, null);
        // consumer the mail service
        assertEquals(consumer.subscribe(serviceKey, null).getVersion(), new Long(1));
        assertEquals(new Long(1), consumer.getResourceHandler().getCurrentSequenceOfChanges());
        // register the mail service provider
        provider.addOrUpdateResource(resource("service.provider", "provider[V1.0]://localhost:110",
                new String[] {}, new String[] {}), "service", associate("service.mail",
                new String[] {}, new String[] {}));
        // at this point the consumer should receive the association changed events
        assertTrue(
                "since the service definition not changed so the service definition still in the cache",
                consumer.getResourcesCache().containsKey(serviceKey));
        Collection<AssociationChangesEvent> associationChanges = changesService
                .listAssociationChangesEvent(0L, 100L, addresses(server, true));
        assertEquals(
                "since both the provider and consumer subscribed the association changes,so,we shold got 2 association changes events",
                associationChanges.size(), 2);
        Long associationChangesSequence = associationChanges.iterator().next().getSequence();
        while (consumer.getVisitedSequenceOfChanges().longValue() != associationChangesSequence
                .longValue()) {
            // the consumer should got the association changes events also update the rcsVisited variable
            Thread.sleep(100);
        }
        // 6, shutdown provider, the association should be removed,the consumer should got this message
        provider.shutdown();
        while (consumer.getVisitedSequenceOfChanges().longValue() == associationChangesSequence
                .longValue()) {
            Thread.sleep(100);
        }
        associationChanges = changesService.listAssociationChangesEvent(0L, 100L, addresses(server,
                true));
        assertEquals(
                "since the provider stopped,so,we just has the consumer care about the association changes",
                associationChanges.size(), 1);
        associationChangesSequence = associationChanges.iterator().next().getSequence();
        while (consumer.getVisitedSequenceOfChanges().longValue() != associationChangesSequence
                .longValue()) {
            // the consumer should got the association changes events also update the rcsVisited variable
            Thread.sleep(100);
        }
        // 7, shutdown the consumer and server
        consumer.shutdown();
        server.shutdown();

    }

    public void test_addOrUpdateResource() throws Throwable {
        cleanup();
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        // 3, startup server and client
        server.startup();
        client.startup();
        RpcConnection connection = client.getClientIoHandler().getConnections().iterator().next();
        String clientAddress = connection.getLocalAddress().getFullAddress();
        String serverAddress = connection.getRemoteAddress().getFullAddress();
        // 4, now create resource
        registerResourceType("t1", true);
        ResourceEntity resourceEntity = resource("t1", "r1", new String[] {}, new String[] {});
        resourceService.addOrUpdateResource(resourceEntity, null, null, clientAddress,
                serverAddress);
        // 5, now subscribe the resources
        ResourceKey resourceKey = new ResourceKey("t1", "r1");
        assertNotNull(client.subscribe(resourceKey, null));
        // 6, at this point the server side should cached the resourceEntity
        assertTrue("the server should cache the resourceEntity", server.getResourcesCache()
                .containsKey(resourceKey));
        assertTrue("after got the resourceEntity from server,the client should cache it.", client
                .getResourcesCache().containsKey(resourceKey));
        // at this point, the cached entity will be used
        {
            ResourceAccessHandlerImpl resourceHandlerImpl = (ResourceAccessHandlerImpl) server
                    .getResourceHandler();
            ResourceService oldService = resourceHandlerImpl.getResourceService();
            resourceHandlerImpl.setResourceService(null);
            assertNotNull("at this point, the cached entity will be used", client.subscribe(
                    resourceKey, null));
            resourceHandlerImpl.setResourceService(oldService);
        }
        // 7, now update the resourceEntity
        {
            AttributeEntity attr = new AttributeEntity();
            attr.setKey("added");
            attr.setValue("added");
            resourceEntity.addAttribute(attr);
        }
        client.addOrUpdateResource(resourceEntity, null, null);
        assertEquals("the creator will be added into the subscribe list of the created resources",
                subscriberService.getSubscribedResources(clientAddress, serverAddress).size(), 1);
        assertEquals(resourceService.getResourceEntity("t1", "r1", true).getVersion(), new Long(2));
        // the client should able to got the notification on resource changes, at that moment will cleanup the cache
        while (client.getResourcesCache().containsKey(resourceKey)) {
            Thread.sleep(100);
        }
        // the server should able to got the notification on resource changes, at that moment the cache will be cleanup
        assertFalse("once server received resource changes event, should cleanup the cache", server
                .getResourcesCache().containsKey(resourceKey));
        // the client side's visited sequence should be latest one
        assertEquals("the client side's visited sequence should be updated", client
                .getVisitedSequenceOfChanges(), client.getResourceHandler()
                .getCurrentSequenceOfChanges());
        // 8, shutdown client(when the client stopped, the resources should be cleanup)
        client.shutdown();
        while (resourceService.getResourceEntity("t1", "r1", true) != null) {
            Thread.sleep(100);
        }
        assertEquals(subscriberService.getSubscribedResources(clientAddress, serverAddress).size(),
                0);
        // 9, shutdown server
        server.shutdown();
    }

    @Test
    public void test_ioListener_registered_before_client_started() throws Throwable {
        cleanup();
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        // 3, register ioListener before we startup client
        final AtomicBoolean onConnectionCreatedCalled = new AtomicBoolean(false);
        final AtomicBoolean onConnectionClosedCalled = new AtomicBoolean(false);

        client.registIoListener(new RpcIoEventListener() {
            public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
                onConnectionClosedCalled.set(true);
            }

            public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
                onConnectionCreatedCalled.set(true);
            }
        });
        // 4, startup server
        server.startup();
        // 5, startup client
        client.startup();
        while (!onConnectionCreatedCalled.get()) {
            Thread.sleep(100);
        }
        // 6, shutdown server
        server.shutdown();
        while (!onConnectionClosedCalled.get()) {
            Thread.sleep(100);
        }

        // 7, shutdown client
        client.shutdown();
    }

    @Test
    public void test_ioListener_registered_after_client_started() throws Throwable {
        cleanup();
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);

        // 3, startup server
        server.startup();
        // 4, startup client
        client.startup();

        // 5, register ioListener after we started client
        final AtomicBoolean onConnectionCreatedCalled = new AtomicBoolean(false);
        final AtomicBoolean onConnectionClosedCalled = new AtomicBoolean(false);

        client.registIoListener(new RpcIoEventListener() {
            public void onConnectionClosed(RpcConnection connection, RpcIoHandler ioHandler) {
                onConnectionClosedCalled.set(true);
            }

            public void onConnectionCreated(RpcConnection connection, RpcIoHandler ioHandler) {
                onConnectionCreatedCalled.set(true);
            }
        });
        // 6, shutdown server
        server.shutdown();
        while (!onConnectionClosedCalled.get()) {
            Thread.sleep(100);
        }
        // 7, shutdown client
        client.shutdown();
    }

    @Test
    public void test_resource_changes_listener_can_got_notification() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        // 3, register resource listener before we start the client
        final AtomicBoolean onBeforeCreatedResourceChanged = new AtomicBoolean(false);
        final AtomicBoolean onAfterCreatedResourceChanged = new AtomicBoolean(false);
        ResourceKey resourceCreatedBeforeClientStart = new ResourceKey("t1", "before");
        ResourceKey resourceCreatedAfterClientStart = new ResourceKey("t1", "after");
        resourceService.addOrUpdateResource(resource("t1", "before", new String[] {},
                new String[] {}), null, null, null, null);
        client.subscribe(resourceCreatedBeforeClientStart, new ResourceEventListener() {
            public void onAssociationChanged(AssociationChangesEvent event) {
                onBeforeCreatedResourceChanged.set(true);
            }

            public void onResourceChanged(ResourceChangesEvent event) {
                onBeforeCreatedResourceChanged.set(true);
            }
        });
        // 4, once client started, the pre-subscribed resources will be automatically re subscribed
        client.startup();
        RpcConnection newConnection = client.getClientIoHandler().getConnections().iterator()
                .next();
        while (subscriberService.getSubscribedResources(
                newConnection.getLocalAddress().getFullAddress(),
                newConnection.getRemoteAddress().getFullAddress()).size() == 0) {
            Thread.sleep(100);
        }
        // 5, register listener after client started, the client should able receive the notification
        {
            client.subscribe(resourceCreatedAfterClientStart, new ResourceEventListener() {
                public void onAssociationChanged(AssociationChangesEvent event) {
                    onAfterCreatedResourceChanged.set(true);
                }

                public void onResourceChanged(ResourceChangesEvent event) {
                    onAfterCreatedResourceChanged.set(true);
                }
            });
            resourceService.addOrUpdateResource(resource("t1", "after", new String[] {},
                    new String[] {}), null, null, null, null);
            while (!onAfterCreatedResourceChanged.get()) {
                Thread.sleep(100);
            }
        }
        // 6, if we update the pre created resource, the subscriber should able got the notification
        {
            resourceService.addOrUpdateResource(resource("t1", "before", new String[] { "added" },
                    new String[] { "added" }), null, null, null, null);
            while (!onBeforeCreatedResourceChanged.get()) {
                Thread.sleep(100);
            }
        }
        // 7, if we associate before with after, the two subscribers should got notification
        //    because "before" subscriber subscribed on left side
        //            "after" subscriber subscribed on right side
        {
            onBeforeCreatedResourceChanged.set(false);
            onAfterCreatedResourceChanged.set(false);
            resourceService.addOrUpdateResource(resource("t1", "before", new String[] { "added" },
                    new String[] { "added" }), "t1", associate("after", new String[] {},
                    new String[] {}), null, null);
            while (!onBeforeCreatedResourceChanged.get()) {
                Thread.sleep(100);
            }
            while (!onAfterCreatedResourceChanged.get()) {
                Thread.sleep(100);
            }
        }
        // 8, when call get method, the cache will be updated
        {
            client.getResourcesCache().clear();
            client.getResourceEntity(resourceCreatedBeforeClientStart);
            assertTrue(client.getResourcesCache().containsKey(resourceCreatedBeforeClientStart));
        }
        // 9, shutdown client
        client.shutdown();
        // 10, shutdown server
        server.shutdown();
    }

    @Test
    public void test_removeResource() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();

        resourceService.addOrUpdateResource(resource("t1", "before", new String[] {},
                new String[] {}), null, null, null, null);
        // 3, startup client and remove the resource
        client.startup();
        client.removeResource(new ResourceKey("t1", "before"));
        assertNull(resourceService.getResourceEntity("t1", "before", false));
        client.shutdown();
        server.shutdown();
    }

    @Test
    public void test_when_reconnect_with_server_should_re_subscribe() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        client.startup();
        resourceService.addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}),
                null, null, null, null);
        // 3, let client subscribe
        client.subscribe(new ResourceKey("t1", "r1"), new ResourceEventListener() {
            public void onAssociationChanged(AssociationChangesEvent event) {
            }

            public void onResourceChanged(ResourceChangesEvent event) {
            }
        });
        assertEquals(client.getResourcesCache().size(), 1);
        server.shutdown();
        while (client.getClientIoHandler().isAlive()) {
            Thread.sleep(100);
        }
        resourceService.addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}),
                null, null, null, null);
        assertEquals(resourceService.getResourceEntity("t1", "r1", false).getVersion(), new Long(2));
        server.startup();
        while (!client.getClientIoHandler().isAlive()) {
            Thread.sleep(100);
        }
        RpcConnection newConnection = client.getClientIoHandler().getConnections().iterator()
                .next();
        while (subscriberService.getSubscribedResources(
                newConnection.getLocalAddress().getFullAddress(),
                newConnection.getRemoteAddress().getFullAddress()).size() == 0) {
            Thread.sleep(100);
        }
        // the client will clear the cache
        assertEquals(client.getResourcesCache().size(), 0);
        client.shutdown();
        server.shutdown();
    }

    @Test
    public void test_client_can_receive_lost_events_during_client_offline() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        resourceService.addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}),
                null, null, null, null);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        client.startup();
        while (client.getVisitedSequenceOfChanges().longValue() != changesService
                .getCurrentSequenceOfChanges()) {
            Thread.sleep(100);
        }
        // 3, let client subscribe changes
        final Collection<ChangesEvent> changes = new HashSet<ChangesEvent>();
        client.subscribe(new ResourceKey("t1", "r1"), new ResourceEventListener() {
            public void onAssociationChanged(AssociationChangesEvent event) {
                changes.add(event);
            }

            public void onResourceChanged(ResourceChangesEvent event) {
                changes.add(event);
            }
        });
        // 4, at this point,the client should cached the resource and the version should be 1
        assertEquals(client.getResourcesCache().get(new ResourceKey("t1", "r1")).getVersion(),
                new Long(1));
        // 5, now force the server shutdown,so, the client will be dropped from server
        server.shutdown();
        // 6, during the server off line time, we'll update the resource and create associations
        resourceService.addOrUpdateResource(resource("t1", "r2", new String[] {}, new String[] {}),
                null, null, null, null);// create resource r2
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        associations.put("r2", new AssociationEntity());
        resourceService.addOrUpdateResource(resource("t1", "r1", new String[] { "added" },
                new String[] { "added" }), "t1", associations, null, null);// updated r1, add association r1--r2
        assertEquals(client.getResourcesCache().size(), 1);
        // 7, now, startup server, the client will connect with server
        server.startup();
        // 8, wait until client got all events
        while (changes.size() < 2) {
            Thread.sleep(100);
        }
        //  9, at this point, client should cleared the cache but got the notification
        assertEquals(client.getResourcesCache().size(), 0);
        assertEquals(changes.size(), 2);
        for (ChangesEvent change : changes) {
            if (change instanceof ResourceChangesEvent) {
                assertEquals(((ResourceChangesEvent) change).getResourceId(), "r1");
            } else {
                assertEquals(((AssociationChangesEvent) change).getLeftId(), "r1");
                assertEquals(((AssociationChangesEvent) change).getRightId(), "r2");
            }
        }
        // 10, if the sequence is smaller than client cached value, client will ignore the events
        changes.clear();
        resetResourceChangesSequence();
        resourceService.removeNonHistoricResourcesByNames("t1", Arrays.asList("r1", "r2"), null,
                null);
        Thread.sleep(2000);
        assertEquals(
                "if the sequence is smaller than client cached value, client will ignore the events",
                changes.size(), 0);

        client.shutdown();
        server.shutdown();
    }

    public void test_unsubscribe() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        client.startup();
        // 3, register two subscribers
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

        client.subscribe(resourceKey, listener1);
        client.subscribe(resourceKey, listener2);
        client.unsubscribe(resourceKey, listener2);
        RpcConnection connection = client.getClientIoHandler().getConnections().iterator().next();
        assertEquals(
                "because we still has listener1 subscribed,so,the server should still keep the subscribe information from client",
                subscriberService.getSubscribedResources(
                        connection.getLocalAddress().getFullAddress(),
                        connection.getRemoteAddress().getFullAddress()).size(), 1);
        client.unsubscribe(resourceKey, listener1);
        assertEquals(
                "because nobody subscribed,so,the server should not keep the subscribe information from client",
                subscriberService.getSubscribedResources(
                        connection.getLocalAddress().getFullAddress(),
                        connection.getRemoteAddress().getFullAddress()).size(), 0);

        client.shutdown();
        server.shutdown();
    }

    @Test
    public void test_association_maintain() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        int serverPort = NetUtils.selectAvailablePort(9010);
        // 1, prepare client
        Client client = client(serverPort, true, 1000 * 1000);
        Client subscriber = client(serverPort, true, 1000 * 1000);
        // 2, prepare server
        Server server = server(serverPort, true, 5, 10);
        server.startup();
        client.startup();
        subscriber.startup();

        final Collection<ChangesEvent> resourceEvents = new HashSet<ChangesEvent>();
        final Collection<ChangesEvent> associationCreatedEvents = new HashSet<ChangesEvent>();
        final Collection<ChangesEvent> associationRemovedEvents = new HashSet<ChangesEvent>();

        subscriber.subscribe(new ResourceKey("t1", "r1"), new ResourceEventListener() {

            public void onAssociationChanged(AssociationChangesEvent event) {
                if (event.getChanges().equals(ResourceChanges.CHANGES_CREATED)) {
                    associationCreatedEvents.add(event);
                } else if (event.getChanges().equals(ResourceChanges.CHANGES_DELETED)) {
                    associationRemovedEvents.add(event);
                }
            }

            public void onResourceChanged(ResourceChangesEvent event) {
                resourceEvents.add(event);
            }
        });
        // 3, prepare association
        resourceService.addOrUpdateResource(resource("t1", "l1", new String[] {}, new String[] {}),
                null, null, null, null);
        resourceService.addOrUpdateResource(resource("t1", "r1", new String[] {}, new String[] {}),
                null, null, null, null);
        assertNotNull(client.addOrUpdateAssociation(new ResourceKey("t1", "l1"), new ResourceKey(
                "t1", "r1"), null));
        assertNotNull(client.getAssociationEntity(new ResourceKey("t1", "l1"), new ResourceKey(
                "t1", "r1")));
        client.removeAssociation(new ResourceKey("t1", "l1"), new ResourceKey("t1", "r1"));
        assertNull(client.getAssociationEntity(new ResourceKey("t1", "l1"), new ResourceKey("t1",
                "r1")));
        // 4, at this point, there should have 1 resource created event, 1 association created event and 1 association removed event
        while (associationCreatedEvents.size() < 1) {
            Thread.sleep(100);
        }
        while (associationRemovedEvents.size() < 1) {
            Thread.sleep(100);
        }

        while (resourceEvents.size() < 1) {
            Thread.sleep(100);
        }
        assertEquals(resourceEvents.iterator().next().getChanges(), ResourceChanges.CHANGES_CREATED);
        assertEquals(associationRemovedEvents.iterator().next().getChanges(),
                ResourceChanges.CHANGES_DELETED);
        assertEquals(associationCreatedEvents.iterator().next().getChanges(),
                ResourceChanges.CHANGES_CREATED);

        subscriber.shutdown();
        client.shutdown();
        server.shutdown();
    }
}
