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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.AssociationChanges;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.AttributeEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.persist.service.ChangesService;
import com.sokeeper.persist.service.ResourceService;
import com.sokeeper.persist.service.SubscriberService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ChangesServiceImplTest extends BaseTestCase {
    @Autowired
    private ChangesService    changesService;

    @Autowired
    private ResourceService   resourceService;

    @Autowired
    private SubscriberService subscriberService;

    public void test_updateStatusToOwnerDied() throws Throwable {
        try {
            changesService.updateStatusToOwnerDied(null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            changesService.updateStatusToOwnerDied("a", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cleanupResourceChangesTable();
        cleanupResourceTypes();
        registerResourceType("scm.configuration", true);

        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app4",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app5",
                "localhost:9020", "localhost:9010");

        changesService.updateStatusToServerDied(Arrays.asList("localhost:9010"));

        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app1",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceUpdatedChanges("scm.configuration", "app2",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceRemovedChanges("scm.configuration", "app3",
                "localhost:9020", "localhost:9010");
        // only when the records' status is CREATED or UPDATED the record will be updated
        assertEquals(changesService.updateStatusToOwnerDied("localhost:9020", "localhost:9010"), 2);
        assertEquals(changesService.updateStatusToOwnerDied("localhost:9020", "localhost:9010"), 0);
        // if the status is DIED, the record will not be updated
        assertEquals(changesService.updateStatusToOwnerDied("localhost:9021", "localhost:9010"), 0);
    }

    public void test_updateAssociationChangesToOwnerDied() throws Throwable {
        cleanup();

        registerResourceType("online", true, false);
        registerResourceType("not-online", false, false);
        ResourceEntity service = new ResourceEntity();
        service.setResourceType("not-online");
        service.setResourceName("service");
        service.setDescription("service");
        resourceService.addOrUpdateResource(service, null, null, "C0", "S0");
        // 1, the resource is online resource, the association will be updated to DIED when owner died
        {
            ResourceEntity provider = new ResourceEntity();
            provider.setResourceType("online");
            provider.setResourceName("service.email");
            provider.setDescription("service.email");
            Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
            associations.put("service", new AssociationEntity());
            resourceService.addOrUpdateResource(provider, "not-online", associations, "C1", "S1");
            changesService.updateStatusToOwnerDied("C0", "S0");
            assertEquals(
                    "if the resource is not online resource,when the owner died, the associations and resources should not be impacted",
                    executeCountSQL("select count(*) from resources where resource_type='not-online' and resource_name='service' "),
                    1);
            assertEquals(
                    "if the resource is not online resource,when the owner died, the associations and resources should not be impacted",
                    executeCountSQL("select count(*) from association"), 1);
            assertEquals(
                    "if the resource is not online resource,when the owner died, the associations and resources should not be impacted",
                    executeCountSQL("select count(*) from association_changes where changes='CREATED'"),
                    1);

            changesService.updateStatusToOwnerDied("C1", "S1");
            assertEquals(
                    "if the resource is online resource,when the owner died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from resources where resource_type='online' and resource_name='service.email' "),
                    0);
            assertEquals(
                    "if the resource is online resource,when the owner died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from association"), 0);
            assertEquals(
                    "if the resource is online resource,when the owner died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from association_changes where changes='DIED'"),
                    1);
        }
        // 2, when server died
        {
            ResourceEntity provider = new ResourceEntity();
            provider.setResourceType("online");
            provider.setResourceName("service.email");
            provider.setDescription("service.email");
            Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
            associations.put("service", new AssociationEntity());
            resourceService.addOrUpdateResource(provider, "not-online", associations, "C1", "S1");

            changesService.updateStatusToServerDied(Arrays.asList("S1"));
            assertEquals(
                    "if the resource is online resource,when the server died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from resources where resource_type='online' and resource_name='service.email' "),
                    0);
            assertEquals(
                    "if the resource is online resource,when the owner died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from association"), 0);
            assertEquals(
                    "if the resource is online resource,when the owner died, the associations and resources should be impacted",
                    executeCountSQL("select count(*) from association_changes where changes='DIED'"),
                    1);
        }
    }

    public void test_updateStatusToServerDied() throws Throwable {
        try {
            changesService.updateStatusToServerDied(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cleanupResourceChangesTable();
        cleanupResourceTypes();
        registerResourceType("scm.configuration", true);

        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app4",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app5",
                "localhost:9021", "localhost:9010");

        changesService.updateStatusToServerDied(Arrays.asList("localhost:9010"));

        changesService.addOrUpdateResourceCreatedChanges("scm.configuration", "app1",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceUpdatedChanges("scm.configuration", "app2",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceRemovedChanges("scm.configuration", "app3",
                "localhost:9020", "localhost:9010");
        changesService.addOrUpdateResourceUpdatedChanges("scm.configuration", "app6",
                "localhost:9020", "localhost:9011");

        Collection<String> servers = new HashSet<String>();
        servers.add("localhost:9010");
        servers.add("localhost:9011");

        assertEquals(changesService.updateStatusToServerDied(servers), 3);
    }

    public void test_addOrUpdateResourceCreatedChanges() throws Throwable {
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();
        changesService.addOrUpdateResourceCreatedChanges("t1", "r1");
        changesService.addOrUpdateResourceCreatedChanges("t2", "r2");
        subscriberService.addResourceChangesSubscriber("t1", "r1", "c1", "s1");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c2", "s2");

        subscriberService.addResourceChangesSubscriber("t1", "r1", "c3", "s3");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c3", "s3");

        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s1")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s2")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s3")).size(),
                2);

        assertEquals(changesService.updateStatusToOwnerDied("c1", "s1"), 0);
        assertEquals(changesService.updateStatusToServerDied(Arrays.asList("s1")), 0);
        assertEquals(changesService.updateStatusToServerDied(Arrays.asList("s2")), 0);
        assertEquals(changesService.updateStatusToServerDied(Arrays.asList("s3")), 0);

    }

    public void test_addOrUpdateResourceRemovedChanges() throws Throwable {
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();
        changesService.addOrUpdateResourceRemovedChanges("t1", "r1");
        changesService.addOrUpdateResourceRemovedChanges("t2", "r2");
        subscriberService.addResourceChangesSubscriber("t1", "r1", "c1", "s1");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c2", "s2");

        subscriberService.addResourceChangesSubscriber("t1", "r1", "c3", "s3");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c3", "s3");

        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s1")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s2")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s3")).size(),
                2);

    }

    public void test_addOrUpdateResourceUpdatedChanges() throws Throwable {
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();
        changesService.addOrUpdateResourceUpdatedChanges("t1", "r1");
        changesService.addOrUpdateResourceUpdatedChanges("t2", "r2");
        subscriberService.addResourceChangesSubscriber("t1", "r1", "c1", "s1");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c2", "s2");

        subscriberService.addResourceChangesSubscriber("t1", "r1", "c3", "s3");
        subscriberService.addResourceChangesSubscriber("t2", "r2", "c3", "s3");

        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s1")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s2")).size(),
                1);
        assertEquals(changesService.listResourceChangesEvent(0L, 100L, Arrays.asList("s3")).size(),
                2);

    }

    public void test_ownerDiedWillNotImpactNoOwnerResourceChanges() throws Throwable {
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();
        cleanupResourceTypes();
        registerResourceType("t1", true);
        registerResourceType("t2", true);
        registerResourceType("t3.1", true);
        registerResourceType("t3.2", true);
        registerResourceType("t4.1", true);
        registerResourceType("t4.2", true);
        registerResourceType("t4.3", true);

        changesService.addOrUpdateResourceUpdatedChanges("t1", "r1");
        changesService.addOrUpdateResourceUpdatedChanges("t2", "r2", "c2", "s2");
        changesService.addOrUpdateResourceUpdatedChanges("t3.1", "r3.1", "c3", "s3");
        changesService.addOrUpdateResourceUpdatedChanges("t3.2", "r3.2", "c3", "s3");
        changesService.addOrUpdateResourceUpdatedChanges("t4.1", "r4.1", "c4", "s4");
        changesService.addOrUpdateResourceUpdatedChanges("t4.2", "r4.2", "c4", "s4");
        changesService.addOrUpdateResourceUpdatedChanges("t4.3", "r4.3", "c4", "s4");

        assertEquals("owner died will only impact the resource changes which has owner",
                changesService.updateStatusToOwnerDied("c2", "s2"), 1);
        assertEquals("owner died will only impact the resource changes which has owner",
                changesService.updateStatusToOwnerDied("c3", "s3"), 2);
        assertEquals("owner died will only impact the resource changes which has owner",
                changesService.updateStatusToServerDied(Arrays.asList("s4")), 3);
    }

    public void test_updateExistedResourceChangesWillImpactOwnerInformation() throws Throwable {
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();
        cleanupResourceTypes();
        registerResourceType("t3.1", true);
        registerResourceType("t3.2", true);

        changesService.addOrUpdateResourceUpdatedChanges("t3.1", "r3.1", "c3", "s3");
        changesService.addOrUpdateResourceUpdatedChanges("t3.2", "r3.2", "c3", "s3");
        changesService.addOrUpdateResourceUpdatedChanges("t3.2", "r3.2", "c4", "s4");
        assertEquals(changesService.updateStatusToServerDied(Arrays.asList("s4")), 1);
    }

    public void test_resourceIsNotOnlineResource_will_not_be_impacted_by_owner_died()
            throws Throwable {
        registerResourceType("online_resource", true);
        registerResourceType("not_online_resource", false);
        changesService.addOrUpdateResourceCreatedChanges("online_resource", "online_resource_1",
                "c1", "s1");
        changesService.addOrUpdateResourceCreatedChanges("online_resource", "online_resource_2",
                "c1", "s1");
        changesService.addOrUpdateResourceCreatedChanges("not_online_resource",
                "not_online_resource_1", "c1", "s1");
        changesService.addOrUpdateResourceCreatedChanges("not_online_resource",
                "not_online_resource_2", "c1", "s1");
        assertEquals(changesService.updateStatusToServerDied(Arrays.asList("s1")), 2);
    }

    public void test_listAssociationChangesEvent() throws Throwable {
        cleanup();
        registerResourceType("t1", true, false);
        // 0, prepare subscriber
        subscriberService.addResourceChangesSubscriber("t1", "service", "C1", "S1");
        // C1 subscribed the service
        subscriberService.addResourceChangesSubscriber("t1", "0", "C0", "S1");
        // C0 subscribed the resource
        ResourceEntity[] providers = new ResourceEntity[2];
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        {
            ResourceEntity service = new ResourceEntity();
            service.setResourceName("service");
            service.setResourceType("t1");
            resourceService.addOrUpdateResource(service, null, null, null, null);

            for (int i = 0; i < providers.length; i++) {
                providers[i] = new ResourceEntity();
                providers[i].setResourceName("" + i);
                providers[i].setResourceType("t1");
                resourceService.addOrUpdateResource(providers[i], "t1", associations, null, null);
            }
        }
        // 1, association created
        associations.put("service", new AssociationEntity());
        resourceService.addOrUpdateResource(providers[0], "t1", associations, null, null);
        Collection<AssociationChangesEvent> changes = changesService.listAssociationChangesEvent(
                0L, 100L, Arrays.asList("S1"));
        assertEquals(changes.size(), 2);
        for (AssociationChangesEvent change : changes) {
            if (change.getSubscriber().equals("C1")) {
                assertEquals(change.getChanges(), AssociationChanges.CHANGES_CREATED);
            } else if (change.getSubscriber().equals("C0")) {
                assertEquals(change.getChanges(), AssociationChanges.CHANGES_CREATED);
            }
        }

        changes = changesService.listAssociationChangesEvent(0L, 100L, "C1", "S1");
        assertEquals(changes.size(), 1);
        for (AssociationChangesEvent change : changes) {
            assertEquals(change.getChanges(), AssociationChanges.CHANGES_CREATED);
        }

        changes = changesService.listAssociationChangesEvent(0L, 100L, "C0", "S1");
        assertEquals(changes.size(), 1);
        for (AssociationChangesEvent change : changes) {
            assertEquals(change.getChanges(), AssociationChanges.CHANGES_CREATED);
        }
        // 2, association's attributes updated
        {
            AttributeEntity attr = new AttributeEntity();
            attr.setKey("association.weight");
            attr.setValue("85%");
            attr.setType("association.attr.type");
            associations.get("service").addAttribute(attr);
            resourceService.addOrUpdateResource(providers[0], "t1", associations, null, null);
            changes = changesService.listAssociationChangesEvent(0L, 100L, Arrays.asList("S1"));
            assertEquals(changes.size(), 2);
            for (AssociationChangesEvent change : changes) {
                if (change.getSubscriber().equals("C1")) {
                    assertEquals(change.getChanges(), AssociationChanges.CHANGES_UPDATED);
                } else if (change.getSubscriber().equals("C0")) {
                    assertEquals(change.getChanges(), AssociationChanges.CHANGES_UPDATED);
                }
            }
        }
        // 3, association removed
        associations.clear();
        resourceService.addOrUpdateResource(providers[0], "t1", associations, null, null);
        changes = changesService.listAssociationChangesEvent(0L, 100L, Arrays.asList("S1"));
        assertEquals(changes.size(), 2);
        for (AssociationChangesEvent change : changes) {
            if (change.getSubscriber().equals("C1")) {
                assertEquals(change.getChanges(), AssociationChanges.CHANGES_DELETED);
            } else if (change.getSubscriber().equals("C0")) {
                assertEquals(change.getChanges(), AssociationChanges.CHANGES_DELETED);
            }
        }
    }

    public void test_listResourceChangesEvent() throws Throwable {
        try {
            changesService.listResourceChangesEvent(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            changesService.listResourceChangesEvent(0L, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            changesService.listResourceChangesEvent(0L, 5L, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cleanupNodeOnlineStatusTable();
        cleanupResourceChangesTable();
        cleanupResourceSubscribeListTable();
        resetResourceChangesSequence();

        // add subscriber
        ChangesSubscriber subscribe = new ChangesSubscriber();
        subscribe.setClientAddress("localhost:8010");
        subscribe.setResourceId("r1");
        subscribe.setResourceType("t1");
        subscribe.setServerAddress("localhost:9010");
        subscriberService.addResourceChangesSubscriber(subscribe);
        assertEquals(subscriberService.getSubscribedResources("localhost:8010", "localhost:9010")
                .size(), 1);
        // add resource changes
        changesService.addOrUpdateResourceCreatedChanges("t1", "r1", "localhost:7010",
                "localhost:9012");

        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).toArray(new ResourceChangesEvent[0])[0]
                .getSubscriber(), "localhost:8010");
        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).toArray(new ResourceChangesEvent[0])[0]
                .getResourceType(), "t1");

        assertEquals(changesService.listResourceChangesEvent(0L, 10L, "localhost:8010",
                "localhost:9010").toArray(new ResourceChangesEvent[0])[0].getSubscriber(),
                "localhost:8010");
        assertEquals(changesService.listResourceChangesEvent(0L, 10L, "localhost:8010",
                "localhost:9010").toArray(new ResourceChangesEvent[0])[0].getResourceType(), "t1");

        assertEquals(changesService.listResourceChangesEvent(0L, 1L,
                Arrays.asList("localhost:9012")).size(), 0);
        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).size(), 1);

        assertEquals(changesService.listResourceChangesEvent(0L, 1L, "localhost:8010",
                "localhost:9012").size(), 0);
        assertEquals(changesService.listResourceChangesEvent(0L, 10L, "localhost:8010",
                "localhost:9010").size(), 1);

        // add more subscribe
        subscribe.setClientAddress("localhost:8012");
        subscriberService.addResourceChangesSubscriber(subscribe);
        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).size(), 2);
        // add more resource changes(no subscriber)
        changesService.addOrUpdateResourceCreatedChanges("t1", "r2", "localhost:7010",
                "localhost:9012");
        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).size(), 2);
        subscribe.setResourceId("r2");
        subscriberService.addResourceChangesSubscriber(subscribe);
        assertEquals(changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).size(), 3);
        ResourceChangesEvent event = changesService.listResourceChangesEvent(0L, 10L,
                Arrays.asList("localhost:9010")).toArray(new ResourceChangesEvent[0])[0];
        assertEquals(event.getChanges(), ResourceChanges.CHANGES_CREATED);
        assertNotNull(event.getResourceId());

        // sequenceFrom > the sequence
        assertEquals(changesService.listResourceChangesEvent(100L, 110L,
                Arrays.asList("localhost:9010")).size(), 0);
        assertEquals(changesService.listResourceChangesEvent(100L, 110L, "localhost:8010",
                "localhost:9010").size(), 0);

    }

    public void test_updateStatusToOwnerDied_resourcesWillBeRemoved() throws Throwable {
        cleanup();
        registerResourceType("t1", true);
        Long[] resourceIds = new Long[3];// rightEntity0.id,rightEntity1.id,leftEntity.id
        // 0, prepare data
        {
            // 0.1 prepare 1 leftEntity with 2 attributes
            ResourceEntity leftEntity = new ResourceEntity();
            {
                leftEntity.setResourceType("t1");
                leftEntity.setResourceName("L1");
                for (int i = 0; i < 2; i++) {
                    AttributeEntity attrOfLeftEntity = new AttributeEntity();
                    attrOfLeftEntity.setKey("L1." + i);
                    attrOfLeftEntity.setValue("L1." + i);
                    leftEntity.addAttribute(attrOfLeftEntity);
                }
            }
            // 0.2 prepare 2 rightEntity with 2 attributes
            for (int i = 0; i < 2; i++) {
                ResourceEntity rightEntity = new ResourceEntity();
                {
                    rightEntity.setResourceType("t1");
                    rightEntity.setResourceName("R" + i);
                    for (int j = 0; j < 2; j++) {
                        AttributeEntity attrOfRightEntity = new AttributeEntity();
                        attrOfRightEntity.setKey(rightEntity.getResourceName() + "." + j);
                        attrOfRightEntity.setValue(rightEntity.getResourceName() + "." + j);
                        rightEntity.addAttribute(attrOfRightEntity);
                    }
                }
                resourceService.addOrUpdateResource(rightEntity, null, null, null, null);
                resourceIds[i] = rightEntity.getId();
            }
            // 0.3 prepare leftEntity association to rightEntity0 and rightEntity1,each association has two attributes
            Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
            for (int i = 0; i < 2; i++) {
                AssociationEntity associationEntity = new AssociationEntity();
                {
                    for (int j = 0; j < 2; j++) {
                        AttributeEntity attrOfAssociationEntity = new AttributeEntity();
                        attrOfAssociationEntity.setKey("A" + i + "." + j);
                        attrOfAssociationEntity.setValue("A" + i + "." + j);
                        associationEntity.addAttribute(attrOfAssociationEntity);
                    }
                }
                associations.put("R" + i, associationEntity);
            }
            // 0.4 save the resourceEntity
            resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
            resourceIds[2] = leftEntity.getId();
            // 0.5 check
            assertEquals(
                    "we should has 3 resources",
                    executeCountSQL("select count(*) from resources where resource_name in ('L1','R0','R1')"),
                    3);
            assertEquals(
                    "we should has 2 associations",
                    executeCountSQL("select count(a.id) from association a,resources r where a.left_id=r.id and r.resource_name in ('L1')"),
                    2);
            assertEquals(
                    "we should has 10 attributes",
                    executeCountSQL("select count(*) from attributes where attr_key in ('L1.0','L1.1','R0.0','R0.1','R1.0','R1.1','A0.0','A0.1','A1.0','A1.1')"),
                    10);
        }
        // 1, prepare the resource changes event
        changesService.addOrUpdateResourceCreatedChanges("t1", "R0", "C0", "S0");
        changesService.addOrUpdateResourceCreatedChanges("t1", "R1", "C1", "S1");
        changesService.addOrUpdateResourceCreatedChanges("t1", "L1", "C2", "S1");
        // 2, let C0,S0 off line
        changesService.updateStatusToOwnerDied("C0", "S0");
        assertEquals(
                "we should has 2 resources",
                executeCountSQL("select count(*) from resources where resource_name in ('L1','R1')"),
                2);
        assertEquals(
                "we should has 1 associations",
                executeCountSQL("select count(a.id) from association a,resources r where a.left_id=r.id and r.resource_name in ('L1')"),
                1);
        assertEquals(
                "we should has 6 attributes",
                executeCountSQL("select count(*) from attributes where attr_key in ('L1.0','L1.1','R1.0','R1.1','A1.0','A1.1')"),
                6);
        // 3, let S1 off line
        changesService.updateStatusToServerDied(Arrays.asList("S1"));
        assertEquals("we should has 0 resources",
                executeCountSQL("select count(*) from resources"), 0);
        assertEquals("we should has 0 associations",
                executeCountSQL("select count(*) from association"), 0);
        assertEquals("we should has 0 attributes",
                executeCountSQL("select count(*) from attributes"), 0);

    }

    public void test_getCurrentResourceChangesSequence() {
        assertNotNull(changesService.getCurrentSequenceOfChanges());
    }
}
