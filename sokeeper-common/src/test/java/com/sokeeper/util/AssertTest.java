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

import java.util.Arrays;
import java.util.HashMap;

import com.sokeeper.util.Assert;


import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class AssertTest extends TestCase {

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

    public void test_assert() throws Throwable {

        try {
            Assert.hasLength(null, "hasLength");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "hasLength");
        }
        try {
            Assert.hasText(null, "hasText");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "hasText");
        }

        try {
            Assert.doesNotContain("abc", "a", "doesNotContain");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "doesNotContain");
        }

        try {
            Assert.notEmpty(Arrays.asList(), "notEmpty");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "notEmpty");
        }

        try {
            Assert.notEmpty(new Object[] {}, "notEmpty");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "notEmpty");
        }

        try {
            Assert.noNullElements(new Object[] { "a", null }, "noNullElements");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "noNullElements");
        }

        try {
            Assert.notEmpty(new HashMap<String, String>(), "notEmpty");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "notEmpty");
        }

        try {
            Assert.isInstanceOf(Integer.class, "abc", "isInstanceOf");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            Assert.isAssignable(Integer.class, Object.class, "isAssignable");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            Assert.isNull("a", "isNull");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            Assert.state(false, "state");
            fail();
        } catch (IllegalStateException e) {
        }
    }

}
