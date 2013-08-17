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

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.exception.RpcException;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.service.RpcServiceBuilder;
import com.sokeeper.rpc.service.support.RpcServiceBuilderImpl;
import com.sokeeper.rpc.transport.support.RpcServerIoHandlerImpl;
import com.sokeeper.server.ResourceChangesDispatcher;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceChangesDispatcherTest extends BaseTestCase {

    @Autowired
    private ChangesService    changesService;

    @Autowired
    private ResourceService   resourceService;

    @Autowired
    private SubscriberService subscriberService;
    private RpcServiceBuilder serviceBuilder = new RpcServiceBuilderImpl();

    private Vector<Object>    args           = null;
    private Exception         throwException = null;

    public void test_startup() throws Throwable {
        ResourceChangesDispatcher dispatcher = new ResourceChangesDispatcher();
        initializeDispatcher(dispatcher, Arrays.asList("localhost:9010"));
        dispatcher.startup();
        dispatcher.shutdown();
    }

    private void initializeDispatcher(ResourceChangesDispatcher dispatcher,
                                      Collection<String> localServers) throws SQLException {
        cleanup();
        args = new Vector<Object>();
        throwException = null;
        dispatcher.setServiceBuilder(serviceBuilder);
        dispatcher.setChangesService(changesService);
        dispatcher.setLocalServers(localServers);
        dispatcher.setPersistedConfiguration(new PersistedConfiguration());
        dispatcher.setNotifierIoHandler(new RpcServerIoHandlerImpl(new RpcConfiguration(
                "tcp://localhost:9010")) {
            public Object invoke(final Method method, final Object[] args, Set<RpcAddress> targets,
                                 String groupName, boolean argsFilterEnabled) throws RpcException,
                    Throwable {
                if (ResourceChangesDispatcherTest.this.throwException != null) {
                    throw new RpcException(ResourceChangesDispatcherTest.this.throwException
                            .getMessage());
                }
                ResourceChangesDispatcherTest.this.args.add(args[0]);
                return null;
            }
        });
    }

    public void test_dispatchToClientsThrowException() throws Throwable {
        ResourceChangesDispatcher dispatcher = new ResourceChangesDispatcher();
        initializeDispatcher(dispatcher, Arrays.asList("localhost:9010"));
        dispatcher.startup();
        throwException = new Exception("test_dispatchToClientsThrowException");
        registerResourceType("online", true);
        registerResourceType("not-online", false);

        subscriberService.addResourceChangesSubscriber("not-online", "service", "localhost:8010",
                "localhost:9010");
        subscriberService.addResourceChangesSubscriber("online", "service.email", "localhost:8010",
                "localhost:9010");

        ResourceEntity service = new ResourceEntity();
        service.setResourceType("not-online");
        service.setResourceName("service");
        service.setDescription("service");
        resourceService.addOrUpdateResource(service, null, null, "C0", "S0");
        dispatcher.shutdown();
    }

    @SuppressWarnings("unchecked")
    public void test_on_association_changes_happened() throws Throwable {
        ResourceChangesDispatcher dispatcher = new ResourceChangesDispatcher();
        initializeDispatcher(dispatcher, Arrays.asList("localhost:9010"));
        dispatcher.startup();

        // at this point the cached sequence in dispatcher should be 0,now we're going to prepare association changes
        registerResourceType("online", true);
        registerResourceType("not-online", false);

        subscriberService.addResourceChangesSubscriber("not-online", "service", "localhost:8010",
                "localhost:9010");
        subscriberService.addResourceChangesSubscriber("online", "service.email", "localhost:8010",
                "localhost:9010");

        ResourceEntity service = new ResourceEntity();
        service.setResourceType("not-online");
        service.setResourceName("service");
        service.setDescription("service");
        resourceService.addOrUpdateResource(service, null, null, "C0", "S0");
        //Map<RpcAddress, Collection<ResourceChangesEvent>> resourceChangesToClients should be push to args[0]
        assertTrue("service created should increment the sequence", changesService
                .getCurrentSequenceOfChanges().intValue() > 0);
        ResourceEntity provider = new ResourceEntity();
        provider.setResourceType("online");
        provider.setResourceName("service.email");
        provider.setDescription("service.email");
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        associations.put("service", new AssociationEntity());
        resourceService.addOrUpdateResource(provider, "not-online", associations, "localhost:8010",
                "localhost:9010");
        //Map<RpcAddress, Collection<ResourceChangesEvent>> resourceChangesToClients should be push to args[1]
        //Map<RpcAddress, Collection<AssociationChangesEvent>> associationChangesToClients should be push to args[2]
        while (args.size() < 2) {
            Thread.sleep(100);
        }
        // args[0]
        {
            Map<RpcAddress, Collection<ResourceChangesEvent>> resourceChangesToClients = (Map<RpcAddress, Collection<ResourceChangesEvent>>) args
                    .get(0);
            assertEquals(resourceChangesToClients.size(), 1);
            assertTrue(resourceChangesToClients.containsKey(RpcSocketAddress
                    .fromFullAddress("localhost:8010")));
            Collection<ResourceChangesEvent> resourceChanges = resourceChangesToClients
                    .get(RpcSocketAddress.fromFullAddress("localhost:8010"));
            assertEquals(resourceChanges.size(), 1);
            ResourceChangesEvent event = resourceChanges.toArray(new ResourceChangesEvent[0])[0];
            assertEquals(event.getResourceId(), "service");
            assertEquals(event.getResourceType(), "not-online");
            assertEquals(event.getChanges(), ResourceChanges.CHANGES_CREATED);
        }
        // args[1]
        {
            Map<RpcAddress, Collection<ChangesEvent>> resourceChangesToClients = (Map<RpcAddress, Collection<ChangesEvent>>) args
                    .get(1);
            assertEquals(resourceChangesToClients.size(), 1);
            assertTrue(resourceChangesToClients.containsKey(RpcSocketAddress
                    .fromFullAddress("localhost:8010")));
            Collection<ChangesEvent> resourceChanges = resourceChangesToClients
                    .get(RpcSocketAddress.fromFullAddress("localhost:8010"));
            assertEquals(resourceChanges.size(), 2);
            for (ChangesEvent change : resourceChanges) {
                if (change instanceof ResourceChangesEvent) {
                    ResourceChangesEvent event = (ResourceChangesEvent) change;
                    assertEquals(event.getResourceId(), "service.email");
                    assertEquals(event.getResourceType(), "online");
                    assertEquals(event.getChanges(), ResourceChanges.CHANGES_CREATED);
                } else {
                    AssociationChangesEvent event = (AssociationChangesEvent) change;
                    assertEquals(event.getLeftId(), "service.email");
                    assertEquals(event.getRightId(), "service");
                    assertEquals(event.getChanges(), ResourceChanges.CHANGES_CREATED);
                }
            }
        }
        dispatcher.shutdown();
    }
}
