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
package com.sokeeper.util;

import java.net.InetSocketAddress;

import com.sokeeper.util.RpcSocketAddress;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcSocketAddressTest extends TestCase {

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_others() throws Throwable {
        assertEquals(new RpcSocketAddress(new InetSocketAddress(9010)).getIpAddress(), "0.0.0.0");
        assertEquals(new RpcSocketAddress(new InetSocketAddress(9010)).getFullAddress(),
                "0.0.0.0:9010");
        assertTrue(new RpcSocketAddress(new InetSocketAddress(9010)).getIpAddresses(true).contains(
                "127.0.0.1"));
    }

    public void test_getIpAddresses() throws Throwable {
        assertTrue(new RpcSocketAddress("localhost", 5005).getIpAddresses(true).size() == 1);
        assertTrue(new RpcSocketAddress("localhost", 5005).getIpAddresses(true).contains(
                "127.0.0.1"));
        assertTrue(new RpcSocketAddress("localhost", 5005).getIpAddresses(false).size() == 0);

        assertTrue(new RpcSocketAddress("localhost", 5005).getFullAddresses(true).size() == 1);
        assertTrue(new RpcSocketAddress("localhost", 5006).getFullAddresses(true).contains(
                "127.0.0.1:5006"));
        assertTrue(new RpcSocketAddress("localhost", 5005).getFullAddresses(false).size() == 0);

    }

    public void test_equals() throws Throwable {
        assertEquals(new InetSocketAddress("localhost", 5005), new RpcSocketAddress("localhost",
                5005));

    }

    public void test_fromFullAddress() throws Throwable {
        try {
            RpcSocketAddress.fromFullAddress(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            RpcSocketAddress.fromFullAddress("");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            RpcSocketAddress.fromFullAddress("a");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            RpcSocketAddress.fromFullAddress("a:b");
            fail();
        } catch (IllegalArgumentException e) {
        }
        assertEquals(RpcSocketAddress.fromFullAddress("localhost:9010"), new RpcSocketAddress(
                "localhost", 9010));

    }

    public void test_ipEquals() throws Throwable {
        assertTrue(new RpcSocketAddress("localhost", 6005).ipEquals(new RpcSocketAddress(
                "127.0.0.1", 5005)));
        assertTrue(new RpcSocketAddress("localhost", 6005).ipEquals(new RpcSocketAddress(
                "localhost", 5005)));
        assertFalse(new RpcSocketAddress("others_does_not_exist", 5005)
                .ipEquals(new RpcSocketAddress("localhost", 5005)));
    }
}
