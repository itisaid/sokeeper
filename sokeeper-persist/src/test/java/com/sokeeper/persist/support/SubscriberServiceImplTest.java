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

import org.springframework.beans.factory.annotation.Autowired;

import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.persist.service.SubscriberService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class SubscriberServiceImplTest extends BaseTestCase {

    @Autowired
    private SubscriberService subscriberService;

    public void test_removeSubscriber() throws Throwable {
        assertNotNull(subscriberService);
        try {
            subscriberService.removeSubscriber(null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            subscriberService.removeSubscriber("a", null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        cleanupResourceSubscribeListTable();

        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9020','localhost:9010')");
        assertEquals(subscriberService.removeSubscriber("localhost:9020", "localhost:9010"), 1);
    }

    public void test_removeSubscriberOfGivenResource() throws Throwable {
        assertNotNull(subscriberService);
        try {
            subscriberService.removeSubscriber(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            subscriberService.removeSubscriber(new ResourceKey("t1", "r1"), "a", null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cleanupResourceSubscribeListTable();

        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'t1','r1','localhost:9020','localhost:9010')");
        assertEquals(subscriberService.removeSubscriber(new ResourceKey("t1", "r1"),
                "localhost:9020", "localhost:9010"), 1);
    }

    public void test_getSubscribedResources() throws Throwable {
        assertNotNull(subscriberService);
        try {
            subscriberService.getSubscribedResources(null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            subscriberService.getSubscribedResources("a", null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        cleanupResourceSubscribeListTable();

        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9020','localhost:9010')");
        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9021','localhost:9010')");

        assertEquals(subscriberService.getSubscribedResources("localhost:9020", "localhost:9010")
                .size(), 1);
        assertEquals(subscriberService.getSubscribedResources("localhost:9021", "localhost:9010")
                .size(), 1);
        assertEquals(subscriberService.removeSubscriber("localhost:9020", "localhost:9010"), 1);
        assertEquals(subscriberService.removeSubscriber("localhost:9021", "localhost:9010"), 1);

    }

    public void test_removeSubscribersOfServer() throws Throwable {
        assertNotNull(subscriberService);
        try {
            subscriberService.removeSubscribersOfServers(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cleanupResourceSubscribeListTable();

        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9020','localhost:9010')");
        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9021','localhost:9010')");
        executeUpdateSQL("insert into resource_subscribe (gmt_create, gmt_modified, resource_type,resource_id,client_address,server_address)values "
                + "(now(),now(),'scm.configuration','app1','localhost:9021','localhost:9011')");

        assertEquals(subscriberService.removeSubscribersOfServers(Arrays.asList("localhost:9010")),
                2);
        assertEquals(subscriberService.getSubscribedResources("localhost:9021", "localhost:9011")
                .size(), 1);
        ChangesSubscriber rsl = subscriberService.getSubscribedResources("localhost:9021",
                "localhost:9011").iterator().next();
        assertEquals(rsl.getClientAddress(), "localhost:9021");
        assertEquals(rsl.getServerAddress(), "localhost:9011");
        assertNotNull(rsl.getId());
        assertNotNull(rsl.getGmtCreated());
        assertNotNull(rsl.getGmtModified());
        assertEquals(rsl.getResourceType(), "scm.configuration");
        assertEquals(rsl.getResourceId(), "app1");

    }

    public void test_addResourceChangesSubscriber() throws Throwable {
        cleanupResourceSubscribeListTable();
        ChangesSubscriber record = null;
        try {
            subscriberService.addResourceChangesSubscriber(record);
            fail();
        } catch (IllegalArgumentException e) {
        }
        record = new ChangesSubscriber();
        record.setResourceType("scm:configuration");
        try {
            subscriberService.addResourceChangesSubscriber(record);
            fail();
        } catch (IllegalArgumentException e) {
        }
        record.setResourceId("test1");
        try {
            subscriberService.addResourceChangesSubscriber(record);
            fail();
        } catch (IllegalArgumentException e) {
        }
        record.setClientAddress("localhost:8010");
        try {
            subscriberService.addResourceChangesSubscriber(record);
            fail();
        } catch (IllegalArgumentException e) {
        }
        record.setServerAddress("localhost:9010");
        subscriberService.addResourceChangesSubscriber(record);
        assertEquals(subscriberService.getSubscribedResources("localhost:8010", "localhost:9010")
                .size(), 1);
        subscriberService.addResourceChangesSubscriber(record);
        assertEquals(subscriberService.getSubscribedResources("localhost:8010", "localhost:9010")
                .size(), 1);

    }
}
