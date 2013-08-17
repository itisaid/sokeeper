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
package com.sokeeper.domain;

import java.sql.Timestamp;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import com.sokeeper.domain.AssociationChanges;
import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.NodeOnlineStatus;
import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.domain.ResourceType;
import com.sokeeper.domain.resource.AssociationAttributeDTO;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.AttributeEntity;
import com.sokeeper.domain.resource.ResourceAttributeDTO;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.PersistLayerException;

/**
 * The address and parentNodeId will be unique.
 *
 * @author James Fu (fuyinhai@gmail.com)
 */
public class DomainTest extends TestCase {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    public void test_ResourceKey() throws Throwable {
        try {
            new ResourceKey(null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            new ResourceKey("resourceType", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        assertEquals(new ResourceKey("resourceType", "resourceName").getResourceName(),
                "resourceName");
        assertEquals(new ResourceKey("resourceType", "resourceName").getResourceType(),
                "resourceType");
        assertEquals(new ResourceKey("resourceType", "resourceName"), new ResourceKey(
                "resourceType", "resourceName"));
        assertEquals(new ResourceKey("resourceType", "resourceName").hashCode(), new ResourceKey(
                "resourceType", "resourceName").hashCode());
        assertFalse(new ResourceKey("resourceType", "resourceName").equals(null));
    }

    public void test_ResourceType() throws Throwable {
        ResourceType rt = new ResourceType();
        rt.setOnlineResource(true);
        rt.setKeepHistoric(false);
        rt.setTypeName("T1");
        assertEquals(rt.getOnlineResource(), Boolean.TRUE);
        assertEquals(rt.getKeepHistoric(), Boolean.FALSE);
        assertEquals(rt.getTypeName(), "T1");

    }

    public void test_NodeOnlineStatus() throws Throwable {
        NodeOnlineStatus nos = new NodeOnlineStatus();
        try {
            nos.setServerAddress(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        nos.setIsMaster(true);
        assertEquals(nos.getIsMaster(), Boolean.TRUE);
        nos.setServerAddress(NodeOnlineStatus.NO_SERVER_ADDRESS);
        assertTrue(nos.isParentNode());
        nos.setGmtExpired(new Timestamp(System.currentTimeMillis()));
        assertNotNull(nos.getGmtExpired());

    }

    public void test_ChangesSubscriber() throws Throwable {
        ChangesSubscriber cc = new ChangesSubscriber();
        cc.setResourceId("R1");
        cc.setResourceType("T1");
        assertEquals(cc.getResourceId(), "R1");
        assertEquals(cc.getResourceType(), "T1");

    }

    public void test_ChangesEntity() throws Throwable {
        ResourceChanges rc = new ResourceChanges();
        rc.setId(5L);
        rc.setAcceptOwnerDiedEvent(true);
        rc.setChanges(ResourceChanges.CHANGES_CREATED);
        rc.setClientAddress("C1");
        rc.setServerAddress("S1");
        rc.setResourceType("T1");
        rc.setResourceId("R1");
        rc.setGmtCreated(new Timestamp(System.currentTimeMillis()));
        rc.setGmtModified(new Timestamp(System.currentTimeMillis()));
        rc.setSequence(5L);
        assertEquals(rc.getChanges(), ResourceChanges.CHANGES_CREATED);
        assertEquals(rc.getClientAddress(), "C1");
        assertEquals(rc.getServerAddress(), "S1");
        assertEquals(rc.getResourceId(), "R1");
        assertEquals(rc.getResourceType(), "T1");
        assertEquals(rc.getAcceptOwnerDiedEvent(), Boolean.TRUE);
        assertEquals(rc.getSequence(), new Long(5));
        assertNotNull(rc.getId());
        assertNotNull(rc.getGmtCreated());
        assertNotNull(rc.getGmtModified());

        AssociationChanges ac = new AssociationChanges();
        ac.setLeftId("L1");
        ac.setLeftType("LT1");
        ac.setRightId("R1");
        ac.setRightType("RT1");
        ac.setAssociationId(5L);
        assertEquals(ac.getLeftId(), "L1");
        assertEquals(ac.getLeftType(), "LT1");
        assertEquals(ac.getRightId(), "R1");
        assertEquals(ac.getRightType(), "RT1");
        assertEquals(ac.getAssociationId(), new Long(5));

    }

    public void test_PersistedConfiguration() throws Throwable {
        assertTrue(new PersistedConfiguration().getSecondsOfResourceChangesWatcherTimer() > 0);
        assertNotNull(new PersistedConfiguration().getSuffixOfServer());

        PersistedConfiguration config = new PersistedConfiguration();
        int sop = config.getSecondsOfPresenceTimer();
        int son = config.getSecondsOfNodeKeepAlive();
        assertTrue(config.getSecondsOfPresenceTimer() > 0);
        assertTrue(config.getSecondsOfNodeKeepAlive() > 0);
        config.setSecondsOfNodeKeepAlive(son * 2);
        config.setSecondsOfPresenceTimer(sop * 2);
        assertEquals(sop * 2, config.getSecondsOfPresenceTimer());
        assertEquals(son * 2, config.getSecondsOfNodeKeepAlive());
        assertEquals(config.getMaxCachedEntities(),
                PersistedConfiguration.DEFAULT_MAX_CACHED_ENTITIES);

        assertNotNull(config.getSuffixOfServer());
        assertNotNull(config.getSecondsOfResourceChangesWatcherTimer() > 0);

    }

    public void test_resource_domains() throws Throwable {
        ResourceEntity rc = new ResourceEntity();
        rc.setResourceType("T1");
        rc.setResourceName("R1");
        rc.setDescription("D1");
        rc.setVersion(1L);
        assertEquals(rc.getResourceType(), "T1");
        assertEquals(rc.getResourceName(), "R1");
        assertEquals(rc.getDescription(), "D1");
        assertEquals(rc.getVersion(), new Long(1));

        try {
            rc.addAttribute(new AttributeEntity());
            fail("attribute's name can not be null.");
        } catch (IllegalArgumentException e) {
        }

        AttributeEntity ae = new AttributeEntity();
        ae.setOwnerId(5L);
        ae.setKey("K1");
        ae.setValue("V1");
        ae.setType("T1");
        ae.setDescription("D1");
        assertEquals(ae.getOwnerId(), new Long(5));
        assertEquals(ae.getKey(), "K1");
        assertEquals(ae.getValue(), "V1");
        assertEquals(ae.getType(), "T1");
        assertEquals(ae.getDescription(), "D1");
        assertNotNull(ae.toString());

        rc.addAttribute(ae);
        assertTrue(rc.containsAttribute("K1"));
        assertEquals(rc.getAttributes().size(), 1);
        assertEquals(rc.getAttribute("K1").getDescription(), "D1");
        rc.addAttributes(Arrays.asList(ae));
        assertEquals(rc.getAttributes().size(), 1);
        assertEquals(rc.attributeKeys().size(), 1);
        rc.removeAttribute("K1");
        assertEquals(rc.attributeKeys().size(), 0);

        AssociationEntity ass = new AssociationEntity();
        ass.setLeftId(5L);
        ass.setRightId(6L);
        ass.setChanged(true);
        assertEquals(ass.getLeftId(), new Long(5));
        assertEquals(ass.getRightId(), new Long(6));
        assertTrue(ass.isChanged());

        AssociationAttributeDTO aaDTO = new AssociationAttributeDTO();
        assertNotNull(aaDTO.getAssociation());
        assertNotNull(aaDTO.getAttribute());

        ResourceAttributeDTO raDTO = new ResourceAttributeDTO();
        assertNotNull(raDTO.getResource());
        assertNotNull(raDTO.getAttribute());

    }
    public void test_AssociationChangesEvent() throws Throwable {
        AssociationChangesEvent rce = new AssociationChangesEvent();
        rce.setChanges(AssociationChanges.CHANGES_CREATED);
        rce.setLeftId("L1");
        rce.setLeftType("T1");
        rce.setRightId("R1");
        rce.setRightType("T1");
        rce.setSubscriber("S1");
        rce.setSequence(1L);
        assertEquals(rce.getChanges(), ResourceChanges.CHANGES_CREATED);
        assertEquals(rce.getRightId(), "R1");
        assertEquals(rce.getRightType(), "T1");
        assertEquals(rce.getLeftId(), "L1");
        assertEquals(rce.getLeftType(), "T1");
        assertEquals(rce.getSubscriber(), "S1");
        assertEquals(rce.getSequence(),new Long(1));

    }
    public void test_ResourceChangesEvent() throws Throwable {
        ResourceChangesEvent rce = new ResourceChangesEvent();
        rce.setChanges(ResourceChanges.CHANGES_CREATED);
        rce.setResourceId("R1");
        rce.setResourceType("T1");
        rce.setSubscriber("S1");
        rce.setSequence(1L);
        assertEquals(rce.getChanges(), ResourceChanges.CHANGES_CREATED);
        assertEquals(rce.getResourceId(), "R1");
        assertEquals(rce.getResourceType(), "T1");
        assertEquals(rce.getSubscriber(), "S1");
        assertEquals(rce.getSequence(),new Long(1));

    }

    public void test_PersistLayerException() throws Throwable {
        new PersistLayerException("msg");
        new PersistLayerException("msg", new Exception(""));
    }
}
