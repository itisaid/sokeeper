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
package com.sokeeper;

//import java.io.File;
//import java.util.Properties;

//import com.sun.enterprise.ee.cms.core.GMSFactory;
//import com.sun.enterprise.ee.cms.core.GroupManagementService;

//import com.sleepycat.je.Environment;
//import com.sleepycat.je.EnvironmentConfig;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class OthersTest extends TestCase {
    //private String     groupName = "slave";
    //private GroupManagementService.MemberType memberType = GroupManagementService.MemberType.CORE;
    //private Properties props     = new Properties();

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test() {
        //EnvironmentConfig cfgOfEnv=new EnvironmentConfig();
        //cfgOfEnv.setAllowCreate(true);
        //Environment env = new Environment(new File("target"),cfgOfEnv);
        //assertNotNull(env);
    }

    public void test_shoal() throws Throwable {
        //        String serverNamePrefix = "node";//UUID.randomUUID().toString();
        //        String server1Name = serverNamePrefix + "1";
        //        String server2Name = serverNamePrefix + "2";
        //
        //        // 1, start node1
        //        GroupManagementService gms1 = (GroupManagementService) GMSFactory.startGMSModule(
        //                server1Name, groupName, memberType, props);
        //        gms1.join();
        //        assertNotNull(gms1);
        //
        //        // 2, start node2
        //        GroupManagementService gms2 = (GroupManagementService) GMSFactory.startGMSModule(
        //                server2Name, groupName, memberType, props);
        //        gms2.join();
        //        assertNotNull(gms2);
        //
        //        // 3, let node1 send message
        //        gms1.getGroupHandle().sendMessage(null, "sayHello".getBytes());
        //
        //        // 4, node2 should able receive the message
        //        Thread.sleep(1000);
    }
}
