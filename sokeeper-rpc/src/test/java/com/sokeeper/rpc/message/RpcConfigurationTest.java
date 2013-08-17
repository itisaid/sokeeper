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
package com.sokeeper.rpc.message;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcConfigurationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_getLastAccessedTimeAscSortedPeers() throws Throwable {
        RpcAddress addr_9010 = new RpcSocketAddress("localhost", 9010);
        RpcAddress addr_9011 = new RpcSocketAddress("localhost", 9011);
        RpcAddress addr_9012 = new RpcSocketAddress("localhost", 9012);

        RpcConfiguration config = new RpcConfiguration("tcp://localhost:9010/client?"
                + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS + "=localhost:9011,localhost:9012");
        assertTrue(config.getServers().contains(addr_9010));
        assertTrue(config.getServers().contains(addr_9011));
        assertTrue(config.getServers().contains(addr_9012));

        config.recordAccessedTime(addr_9012);
        Thread.sleep(1);
        config.recordAccessedTime(addr_9010);
        Thread.sleep(1);
        config.recordAccessedTime(addr_9011);
        Thread.sleep(1);

        RpcAddress[] sorted = config.getOrderedServers();
        assertEquals(sorted[0], addr_9012);
        assertEquals(sorted[1], addr_9010);
        assertEquals(sorted[2], addr_9011);

        config.recordAccessedTime(addr_9011);
        Thread.sleep(1);
        config.recordAccessedTime(addr_9012);
        Thread.sleep(1);
        config.recordAccessedTime(addr_9010);
        Thread.sleep(1);
        sorted = config.getOrderedServers();
        assertEquals(sorted[0], addr_9011);
        assertEquals(sorted[1], addr_9012);
        assertEquals(sorted[2], addr_9010);

    }

    @Test
    public void test_whiteList() throws Throwable {
        RpcConfiguration cfg = new RpcConfiguration("tcp://localhost:9090/client?a=a");
        try {
            cfg.setWhiteIpList(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        cfg.setWhiteIpList(Arrays.asList("127.0.0.1", "192.168.0.1"));
        assertTrue(cfg.isIpInWhiteList("127.0.0.1"));
    }

    @Test
    public void test_setServers() throws Throwable {
        RpcConfiguration cfg = new RpcConfiguration("tcp://localhost:9090/client?a=a");
        Set<RpcAddress> servers = new HashSet<RpcAddress>();
        servers.add(RpcSocketAddress.fromFullAddress("localhost:9010"));
        servers.add(RpcSocketAddress.fromFullAddress("localhost:9020"));
        cfg.setServers(servers.iterator().next(), servers);
        assertEquals(cfg.getMainAddress(), RpcSocketAddress.fromFullAddress("localhost:9010"));
        assertTrue(cfg.getServers().contains(RpcSocketAddress.fromFullAddress("localhost:9020")));
    }

    @Test
    public void test_parameters() throws Throwable {
        RpcConfiguration cfg = new RpcConfiguration("tcp://localhost:9090/client?a=a");
        assertTrue(cfg.getAutoReconnectInMs() < 0);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?"
                + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS + "=a:98").getServers().size(), 2);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?"
                + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS + "=a:a").getServers().size(), 1);

        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?"
                + RpcConfiguration.KEY_RPC_URL_PARAM_SERVERS + "=aa").getServers().size(), 1);

        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=all")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_ALL);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=main")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_MAIN);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=anyone")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_ANYONE);

        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=aLl")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_ALL);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=maiN")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_MAIN);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=anyOne")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_ANYONE);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client").getConnectPolicy(),
                RpcConfiguration.CONNECT_POLICY_ALL);
        assertEquals(new RpcConfiguration("tcp://localhost:9090/client?connect_policy=aaa")
                .getConnectPolicy(), RpcConfiguration.CONNECT_POLICY_ALL);

    }

}
