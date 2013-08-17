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

import com.sokeeper.util.UrlParser;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class UrlParserTest extends TestCase {

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

    public void test_parse() throws Throwable {
        try {
            new UrlParser(null);
            fail("should report null pointer exception.");
        } catch (IllegalArgumentException e) {
        }
        // 1, should be able to parse the protocol
        String url = "tcp://0.0.0.0:8080/client?hb_sec=5000&capacity=500&illegal=abc&boolean=true";
        UrlParser rpcUrl = new UrlParser(url);
        assertEquals(rpcUrl.getProtocol(), "tcp");
        assertEquals(rpcUrl.getHost(), "0.0.0.0");
        assertEquals(rpcUrl.getPort(), 8080);
        assertEquals(rpcUrl.getParameters().getParameter("hb_sec", 0), 5000);
        assertEquals(rpcUrl.getParameters().getParameter("capacity", ""), "500");

        assertEquals(rpcUrl.getParameters().getParameter("default", "default"), "default");
        assertEquals(rpcUrl.getParameters().getParameter("illegal", 8), 8);

        assertEquals(rpcUrl.getParameters().getParameter("enabled", false), false);
        assertEquals(rpcUrl.getParameters().getParameter("boolean", false), true);
        assertTrue(rpcUrl.getParameters().hasParameter("capacity"));
        assertEquals(rpcUrl.getParameters().getIndexedParameter("illegal",
                new String[] { "bbb", "abc" }, 5, true), 1);
        rpcUrl.getParameters().toString();
        try {
            rpcUrl.openConnection(rpcUrl.getUrl());
            fail("should unsupported operation.");
        } catch (UnsupportedOperationException e) {
        }

        try {
            new UrlParser("");
            fail("should report illegal argument exception");
        } catch (IllegalArgumentException e) {
        }
        try {
            new UrlParser("tcp://localhost");
            fail("should report illegal argument exception");
        } catch (IllegalArgumentException e) {
        }

    }

}
