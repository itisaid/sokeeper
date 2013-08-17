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
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.AttributeEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.PersistLayerException;
import com.sokeeper.persist.service.ResourceService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceServiceImplTest extends BaseTestCase {

    @Autowired
    private ResourceService resourceService;

    public void test_addOrUpdateResource_noAssociations_noAttributes() throws Throwable {
        cleanup();
        registerResourceType("t1", false);

        // 1, first time create
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType("t1");
        resourceEntity.setResourceName("r1");
        assertEquals(resourceService.addOrUpdateResource(resourceEntity, null, null, null, null).getVersion(),
                     ResourceEntity.INITIAL_VERSION);
        assertEquals(executeUpdateSQL("delete from resource_changes where resource_id='r1' and resource_type='t1'"), 1);
        // 2, second time update(but no changes should not generate changes event)
        Long resourceId = resourceService.getResourceEntity("t1", "r1", false).getId();
        resourceService.addOrUpdateResource(resourceEntity, null, null, null, null);
        assertEquals(resourceId, resourceService.getResourceEntity("t1", "r1", false).getId());
        assertEquals(
                     "update(but no changes should not generate changes event)",
                     executeCountSQL("select count(*) from resource_changes where resource_id='r1' and resource_type='t1'"),
                     0);
    }

    public void test_addOrUpdateResource_noAssociations_hasAttributes() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
        // 1, first time create
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType("t1");
        resourceEntity.setResourceName("r1");
        AttributeEntity[] attrs = new AttributeEntity[3];
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = new AttributeEntity();
            attrs[i].setKey("attr" + i);
            attrs[i].setValue("attrVal" + i);
            resourceEntity.addAttribute(attrs[i]);
        }
        assertEquals(resourceService.addOrUpdateResource(resourceEntity, null, null, null, null).getVersion(),
                     ResourceEntity.INITIAL_VERSION);
        cleanupResourceChangesTable();

        ResourceEntity record = resourceService.getResourceEntity("t1", "r1", true);
        assertTrue(record.containsAttribute("attr0"));
        assertTrue(record.containsAttribute("attr1"));
        assertTrue(record.containsAttribute("attr2"));
        assertEquals(record.getAttribute("attr1").getValue(), "attrVal1");
        // 2, update, but all the attributes keep unchanged
        {
            assertTrue(resourceService.addOrUpdateResource(resourceEntity, null, null, null, null).getVersion() > ResourceEntity.INITIAL_VERSION);
            assertEquals("the version number should be incremented when perform update", resourceEntity.getVersion(),
                         new Long(2));
            ResourceEntity recordUpdated = resourceService.getResourceEntity("t1", "r1", true);
            assertEquals(record.getId(), recordUpdated.getId());
            assertEquals(record.getAttributes().size(), recordUpdated.getAttributes().size());
            assertEquals("update(but no changes should not generate changes event)",
                         executeCountSQL("select count(*) from resource_changes"), 0);
        }
        // 3, update, 1 attribute changed
        {
            record.getAttribute("attr1").setValue("attrVal1changed");
            resourceService.addOrUpdateResource(record, null, null, null, null);
            ResourceEntity recordUpdated = resourceService.getResourceEntity("t1", "r1", true);
            assertEquals(recordUpdated.getAttribute("attr1").getValue(), "attrVal1changed");
            assertEquals(
                         "update",
                         executeCountSQL("select count(*) from resource_changes where resource_id='r1' and resource_type='t1'"),
                         1);
            cleanupResourceChangesTable();
        }
        // 4, update, added one attribute
        {
            AttributeEntity attr3 = new AttributeEntity();
            attr3.setKey("attr3");
            attr3.setValue("attr3Val");
            record.addAttribute(attr3);
            resourceService.addOrUpdateResource(record, null, null, null, null);
            ResourceEntity recordUpdated = resourceService.getResourceEntity("t1", "r1", true);
            assertEquals(recordUpdated.getAttribute("attr3").getValue(), "attr3Val");
            assertEquals(
                         "update",
                         executeCountSQL("select count(*) from resource_changes where resource_id='r1' and resource_type='t1'"),
                         1);
            cleanupResourceChangesTable();
        }
        // 5, update, removed one attribute
        {
            record.removeAttribute("attr3");
            resourceService.addOrUpdateResource(record, null, null, null, null);
            ResourceEntity recordUpdated = resourceService.getResourceEntity("t1", "r1", true);
            assertNull(recordUpdated.getAttribute("attr3"));
            assertNotNull(recordUpdated.getAttribute("attr0"));
            assertNotNull(recordUpdated.getAttribute("attr1"));
            assertNotNull(recordUpdated.getAttribute("attr2"));
            assertEquals(
                         "update",
                         executeCountSQL("select count(*) from resource_changes where resource_id='r1' and resource_type='t1'"),
                         1);
            cleanupResourceChangesTable();
        }
        // 6, update, added one attribute , removed one attribute and updated one attribute
        {
            AttributeEntity attr3 = new AttributeEntity();
            attr3.setKey("attr3");
            attr3.setValue("attr3Val");
            record.addAttribute(attr3);
            record.removeAttribute("attr2");
            record.getAttribute("attr1").setValue("newValue");
            resourceService.addOrUpdateResource(record, null, null, null, null);
            ResourceEntity recordUpdated = resourceService.getResourceEntity("t1", "r1", true);
            assertNotNull(recordUpdated.getAttribute("attr3"));
            assertNull(recordUpdated.getAttribute("attr2"));
            assertEquals(recordUpdated.getAttribute("attr1").getValue(), "newValue");
            assertEquals(
                         "update",
                         executeCountSQL("select count(*) from resource_changes where resource_id='r1' and resource_type='t1'"),
                         1);
            cleanupResourceChangesTable();
        }
    }

    public void test_getResourceIdsByTypeAndNames() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
        Collection<String> names = new HashSet<String>();
        for (int i = 0; i < 5; i++) {
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType("t1");
            resourceEntity.setResourceName("r" + i);
            resourceService.addOrUpdateResource(resourceEntity, null, null, null, null);
            names.add("r" + i);
        }
        assertEquals(resourceService.getResourceIdsByTypeAndNames("t1", names).size(), 5);
    }

    public void test_leftResourceChanged_associationChanges_should_be_generated() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
        // 1, prepare rightEntity"
        ResourceEntity rightEntity = new ResourceEntity();
        rightEntity.setResourceType("t1");
        rightEntity.setResourceName("r1");
        resourceService.addOrUpdateResource(rightEntity, null, null, null, null);
        // 2, prepare leftEntity and let it associated with rightEntity
        ResourceEntity leftEntity = new ResourceEntity();
        leftEntity.setResourceType("t1");
        leftEntity.setResourceName("l1");
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        associations.put("r1", new AssociationEntity());
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        // 3, update leftEntities' attributes
        AttributeEntity leftAttr = new AttributeEntity();
        leftAttr.setKey("leftAttr");
        leftAttr.setValue("leftAttr");
        leftEntity.addAttribute(leftAttr);
        cleanupAssociationChanges();
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        // 4, at this point should generated the association changes event
        assertEquals(
                     executeCountSQL("select count(*) from association_changes where changes='UPDATED' and left_id='l1' "),
                     1);

    }

    public void test_addOrUpdateResource_hasAssociations_noAttributes() throws Throwable {
        cleanup();
        registerResourceType("t1", false);

        Map<String, ResourceEntity> resources = new HashMap<String, ResourceEntity>();
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        for (int i = 0; i < 5; i++) {
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType("t1");
            resourceEntity.setResourceName("right" + i);
            resourceService.addOrUpdateResource(resourceEntity, null, null, null, null);
            associations.put("right" + i, new AssociationEntity());
            resources.put("right" + i, resourceEntity);
        }
        ResourceEntity leftEntity = new ResourceEntity();
        leftEntity.setResourceType("t1");
        leftEntity.setResourceName("left");
        // 1, create normal association
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        for (Entry<String, AssociationEntity> pair : associations.entrySet()) {
            assertEquals(pair.getValue().getLeftId(), leftEntity.getId());
            assertEquals(resources.get(pair.getKey()).getId(), pair.getValue().getRightId());
        }
        assertEquals(
                     "create resource who has associations should generate association_changes event",
                     executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and changes='CREATED'"),
                     5);
        cleanupResourceChangesTable();
        // 2, remove an association
        associations.remove("right4");
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        assertEquals(resourceService.getAssociationWithAttributesByLeftId(leftEntity.getId()).size(), 4);
        assertEquals(
                     executeCountSQL("select count(*) from association a,resources r "
                                     + "where r.resource_name in('right0','right1','right2','right3') and r.id=a.right_id"),
                     4);
        assertEquals(executeCountSQL("select count(*) from association"), 4);
        assertEquals(
                     "remove association should generate association_changes event",
                     executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and changes='DELETED'"),
                     1);
        assertEquals(
                     "remove association should generate association_changes event,but the un changed association changes should not be impacted",
                     executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and changes='CREATED'"),
                     4);
        cleanupResourceChangesTable();
        // add an association
        associations.put("right4", new AssociationEntity());
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        assertEquals(resourceService.getAssociationWithAttributesByLeftId(leftEntity.getId()).size(), 5);
        assertEquals(
                     executeCountSQL("select count(*) from association a,resources r "
                                     + "where r.resource_name in('right0','right1','right2','right3','right4') and r.id=a.right_id"),
                     5);
        assertEquals(executeCountSQL("select count(*) from association"), 5);
        assertEquals(
                     "add association should generate association changes event",
                     executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and changes='CREATED'"),
                     5);
        assertEquals("add association should not generate resource changes event",
                     executeCountSQL("select count(*) from resource_changes"), 0);
        cleanupResourceChangesTable();
        // remove one association and add another association
        {
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType("t1");
            resourceEntity.setResourceName("right5");
            resourceService.addOrUpdateResource(resourceEntity, null, null, null, null);

            associations.remove("right4");
            associations.put("right5", new AssociationEntity());
            associations.put("right6", new AssociationEntity());
            try {
                resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
                fail("when the right side resource does not exist,should throw PersistLayerException");
            } catch (PersistLayerException e) {
            }
            associations.remove("right6");

            resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
            assertEquals(resourceService.getAssociationWithAttributesByLeftId(leftEntity.getId()).size(), 5);
            assertEquals(
                         executeCountSQL("select count(*) from association a,resources r "
                                         + "where r.resource_name in('right0','right1','right2','right3','right5') and r.id=a.right_id"),
                         5);
            assertEquals(executeCountSQL("select count(*) from association"), 5);
            assertEquals(
                         "remove and add association should generate association changes event",
                         executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and right_id='right4' and changes='DELETED'"),
                         1);
            assertEquals(
                         "remove and add association should generate association changes event",
                         executeCountSQL("select count(*) from association_changes where left_id='left' and left_type='t1' and right_id='right5' and changes='CREATED'"),
                         1);
            cleanupResourceChangesTable();
        }
    }

    public void test_addOrUpdateResource_hasAssociations_hasAttributes() throws Throwable {
        cleanup();
        registerResourceType("t1", false);

        Map<String, ResourceEntity> resources = new HashMap<String, ResourceEntity>();
        Map<String, AssociationEntity> associations = new HashMap<String, AssociationEntity>();
        for (int i = 0; i < 5; i++) {
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType("t1");
            resourceEntity.setResourceName("right" + i);
            resourceService.addOrUpdateResource(resourceEntity, null, null, null, null);

            AttributeEntity attr = new AttributeEntity();
            attr.setKey("attr" + i);
            attr.setValue("attr" + i + "value");
            AssociationEntity association = new AssociationEntity();
            association.addAttribute(attr);
            associations.put("right" + i, association);
            resources.put("right" + i, resourceEntity);
        }
        ResourceEntity leftEntity = new ResourceEntity();
        leftEntity.setResourceType("t1");
        leftEntity.setResourceName("left");
        // 1, create normal association
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        for (Entry<String, AssociationEntity> pair : associations.entrySet()) {
            assertEquals(pair.getValue().getLeftId(), leftEntity.getId());
            assertEquals(resources.get(pair.getKey()).getId(), pair.getValue().getRightId());
        }
        assertEquals(executeCountSQL("select count(*) from attributes r,association a where a.id=r.owner_id "), 5);
        // 3, drop one association
        AssociationEntity ass4 = associations.remove("right4");
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        assertEquals(executeCountSQL("select count(*) from attributes r,association a where a.id=r.owner_id "), 4);
        // 4, add one association
        associations.put("right4", ass4);
        resourceService.addOrUpdateResource(leftEntity, "t1", associations, null, null);
        assertEquals(executeCountSQL("select count(*) from attributes r,association a where a.id=r.owner_id "), 5);
    }

    public void test_addOrUpdateAssociation() throws Throwable {
        cleanup();
        registerResourceType("t1", true);

        ResourceEntity leftEntity = new ResourceEntity();
        leftEntity.setResourceType("t1");
        leftEntity.setResourceName("L1");
        ResourceEntity rightEntity = new ResourceEntity();
        rightEntity.setResourceType("t1");
        rightEntity.setResourceName("R1");
        // 1, leftEntity does not exist
        try {
            resourceService.addOrUpdateAssociation(leftEntity.getResourceKey(), rightEntity.getResourceKey(), null,
                                                   null, null);
            fail();
        } catch (IllegalStateException e) {
        }
        resourceService.addOrUpdateResource(leftEntity, null, null, null, null);
        // 2, rightEntity does not exist
        try {
            resourceService.addOrUpdateAssociation(leftEntity.getResourceKey(), rightEntity.getResourceKey(), null,
                                                   null, null);
            fail();
        } catch (IllegalStateException e) {
        }
        resourceService.addOrUpdateResource(rightEntity, null, null, null, null);
        // 3, create association succeed
        AssociationEntity createdEntity = null;
        {
            createdEntity = resourceService.addOrUpdateAssociation(leftEntity.getResourceKey(),
                                                                   rightEntity.getResourceKey(), null, null, null);
            assertNotNull(createdEntity);
            assertNotNull(resourceService.getAssociationWithAttributesByKeys(leftEntity.getResourceKey(),
                                                                             rightEntity.getResourceKey()));
            assertEquals(executeCountSQL("select count(*) from association_changes where changes='CREATED'"), 1);
        }
        // 4, update the association by adding attributes
        AssociationEntity updatedEntity = null;
        {
            Map<String, String> attrs = new HashMap<String, String>();
            attrs.put("added", "added");
            updatedEntity = resourceService.addOrUpdateAssociation(leftEntity.getResourceKey(),
                                                                   rightEntity.getResourceKey(), attrs, null, null);
            assertNotNull(updatedEntity);
            assertNotNull(resourceService.getAssociationWithAttributesByKeys(leftEntity.getResourceKey(),
                                                                             rightEntity.getResourceKey()));
            assertEquals(createdEntity.getId(), updatedEntity.getId());
            assertEquals(
                         executeCountSQL("select count(*) from association_changes where changes='UPDATED' and accept_died_evt='1'"),
                         1);
        }
    }

    public void test_getAssociationWithAttributesByLeftId() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
        ResourceEntity leftEntity = new ResourceEntity();
        leftEntity.setResourceType("t1");
        leftEntity.setResourceName("left");
        Map<String, AssociationEntity> assEntities = new HashMap<String, AssociationEntity>();
        // prepare 5 rightEntity
        for (int i = 0; i < 5; i++) {
            ResourceEntity rightEntity = new ResourceEntity();
            rightEntity.setResourceType("t1");
            rightEntity.setResourceName("right" + i);
            resourceService.addOrUpdateResource(rightEntity, null, null, null, null);
            assEntities.put(rightEntity.getResourceName(), new AssociationEntity());
        }
        // association don't have attributes
        resourceService.addOrUpdateResource(leftEntity, "t1", assEntities, null, null);
        AssociationEntity[] associations = resourceService.getAssociationWithAttributesByLeftId(leftEntity.getId()).toArray(
                                                                                                                            new AssociationEntity[0]);
        assertEquals(associations.length, 5);
        for (AssociationEntity association : associations) {
            assertNotNull(association.getId());
        }
        // prepare 1 attribute for each association
        for (int i = 0; i < 5; i++) {
            AssociationEntity association = new AssociationEntity();
            AttributeEntity attr = new AttributeEntity();
            attr.setKey("asso_right" + i);
            attr.setValue("asso_right" + i);
            association.addAttribute(attr);
            assEntities.put("right" + i, association);
        }
        resourceService.addOrUpdateResource(leftEntity, "t1", assEntities, null, null);
        associations = resourceService.getAssociationWithAttributesByLeftId(leftEntity.getId()).toArray(
                                                                                                        new AssociationEntity[0]);
        assertEquals(associations.length, 5);

        for (AssociationEntity association : associations) {
            assertNotNull(association.getId());
            assertEquals(association.getAttributes().size(), 1);
        }
        for (int i = 0; i < 5; i++) {
            ResourceKey leftKey = leftEntity.getResourceKey();
            ResourceKey rightKey = new ResourceKey("t1", "right" + i);
            assertNotNull(resourceService.geAssociationIdByKeys(leftKey, rightKey));
            assertNotNull(resourceService.getAssociationWithAttributesByKeys(leftKey, rightKey));
            resourceService.removeAssociation(leftKey, rightKey);
            assertNull(resourceService.geAssociationIdByKeys(leftKey, rightKey));
            assertNull(resourceService.getAssociationWithAttributesByKeys(leftKey, rightKey));
            assertEquals(
                         executeCountSQL("select count(*) from association_changes where left_type='t1' and right_type='t1' and left_id='"
                                         + leftKey.getResourceName()
                                         + "' and right_id='"
                                         + rightKey.getResourceName()
                                         + "'"), 1);

        }
    }

    public void test_getAssociationWithAttributesByRightId() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
        ResourceEntity rightEntity = new ResourceEntity();
        rightEntity.setResourceType("t1");
        rightEntity.setResourceName("right");
        Map<String, AssociationEntity> assEntities = new HashMap<String, AssociationEntity>();
        // prepare 1 rightEntity
        resourceService.addOrUpdateResource(rightEntity, "t1", assEntities, null, null);
        // prepare 5 leftEntity
        for (int i = 0; i < 5; i++) {
            assEntities.clear();
            ResourceEntity leftEntity = new ResourceEntity();
            leftEntity.setResourceType("t1");
            leftEntity.setResourceName("left" + i);
            assEntities.put("right", new AssociationEntity());
            resourceService.addOrUpdateResource(leftEntity, "t1", assEntities, null, null);
        }
        AssociationEntity[] associations = resourceService.getAssociationWithAttributesByRightId(rightEntity.getId()).toArray(
                                                                                                                              new AssociationEntity[0]);
        assertEquals(associations.length, 5);
        // prepare 1 attribute for each association
        for (int i = 0; i < 5; i++) {
            assEntities.clear();
            ResourceEntity leftEntity = new ResourceEntity();
            leftEntity.setResourceType("t1");
            leftEntity.setResourceName("left" + i);
            assEntities.put("right", new AssociationEntity());
            AttributeEntity attr = new AttributeEntity();
            attr.setKey("left" + i);
            attr.setValue("left" + i);
            assEntities.get("right").addAttribute(attr);
            resourceService.addOrUpdateResource(leftEntity, "t1", assEntities, null, null);
        }
        associations = resourceService.getAssociationWithAttributesByRightId(rightEntity.getId()).toArray(
                                                                                                          new AssociationEntity[0]);
        assertEquals(associations.length, 5);
        for (AssociationEntity association : associations) {
            assertNotNull(association.getId());
            assertEquals(association.getAttributes().size(), 1);
        }
        for (int i = 0; i < 5; i++) {
            ResourceKey leftKey = new ResourceKey("t1", "left" + i);
            ResourceKey rightKey = rightEntity.getResourceKey();
            assertNotNull(resourceService.geAssociationIdByKeys(leftKey, rightKey));
            assertNotNull(resourceService.getAssociationWithAttributesByKeys(leftKey, rightKey));
            resourceService.removeAssociation(leftKey, rightKey);
            assertNull(resourceService.geAssociationIdByKeys(leftKey, rightKey));
            assertNull(resourceService.getAssociationWithAttributesByKeys(leftKey, rightKey));
            assertEquals(
                         executeCountSQL("select count(*) from association_changes where left_type='t1' and right_type='t1' and left_id='"
                                         + leftKey.getResourceName()
                                         + "' and right_id='"
                                         + rightKey.getResourceName()
                                         + "'"), 1);
        }
    }

    public void test_removeNonHistoricResourcesByNames() throws Throwable {
        cleanup();
        registerResourceType("t1", false);
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
            assertEquals("we should has 3 resources",
                         executeCountSQL("select count(*) from resources where resource_name in ('L1','R0','R1')"), 3);
            assertEquals(
                         "we should has 2 associations",
                         executeCountSQL("select count(a.id) from association a,resources r where a.left_id=r.id and r.resource_name in ('L1')"),
                         2);
            assertEquals(
                         "we should has 10 attributes",
                         executeCountSQL("select count(*) from attributes where attr_key in ('L1.0','L1.1','R0.0','R0.1','R1.0','R1.1','A0.0','A0.1','A1.0','A1.1')"),
                         10);

        }
        // 1, remove rightEntity0
        resourceService.removeNonHistoricResourcesByNames("t1", Arrays.asList("R0"), null, null);
        assertEquals("we should has 2 resources",
                     executeCountSQL("select count(*) from resources where resource_name in ('L1','R1')"), 2);
        assertEquals(
                     "we should has 1 associations",
                     executeCountSQL("select count(a.id) from association a,resources r where a.left_id=r.id and r.resource_name in ('L1')"),
                     1);
        assertEquals(
                     "we should has 6 attributes",
                     executeCountSQL("select count(*) from attributes where attr_key in ('L1.0','L1.1','R1.0','R1.1','A1.0','A1.1')"),
                     6);
        assertEquals(
                     "we should has 1 association deleted event",
                     executeCountSQL("select count(*) from association_changes where left_id='L1' and right_id='R0' and changes='DELETED'"),
                     1);
        // 2, remove leftEntity
        cleanupResourceChangesTable();
        resourceService.removeNonHistoricResourcesByNames("t1", Arrays.asList("L1"), null, null);
        assertEquals("we should has 1 resources",
                     executeCountSQL("select count(*) from resources where resource_name in ('R1')"), 1);
        assertEquals(
                     "we should has 0 associations",
                     executeCountSQL("select count(a.id) from association a,resources r where a.left_id=r.id and r.resource_name in ('L1')"),
                     0);
        assertEquals("we should has 2 attributes",
                     executeCountSQL("select count(*) from attributes where attr_key in ('R1.0','R1.1')"), 2);
        assertEquals("we should has 1 resource changes event",
                     executeCountSQL("select count(*) from resource_changes where resource_id='L1'"), 1);
        assertEquals(
                     "we should has 1 association deleted event",
                     executeCountSQL("select count(*) from association_changes where left_id='L1' and right_id='R1' and changes='DELETED'"),
                     1);

    }

    public void test_removeNonHistoricResourcesByNames_throwExceptionForKeepHistoricResource() throws Throwable {
        cleanup();
        registerResourceType("t1", true, true);
        try {
            resourceService.removeNonHistoricResourcesByNames("t1", Arrays.asList("L1"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }
}
