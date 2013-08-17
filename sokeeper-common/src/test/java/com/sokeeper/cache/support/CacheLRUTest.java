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
package com.sokeeper.cache.support;

import com.sokeeper.cache.support.CacheLRU;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class CacheLRUTest extends TestCase {

    public void test_CacheLRU() throws Throwable {
        CacheLRU<String, String> cacheLRU = new CacheLRU<String, String>(2);
        cacheLRU.put("1", "1");
        cacheLRU.put("2", "2");
        cacheLRU.put("3", "3");
        assertFalse("the eldest entity should be removed", cacheLRU.containsKey("1"));
        assertTrue(cacheLRU.containsKey("2"));
        assertTrue(cacheLRU.containsKey("3"));
        cacheLRU.put("1", "1");
        assertTrue(cacheLRU.containsKey("1"));
        assertTrue(cacheLRU.containsKey("3"));
        assertEquals(cacheLRU.size(), 2);
        cacheLRU.removeElements(new Object[] { "4", "5" });
        assertEquals(cacheLRU.get("1"), "1");
        assertTrue(cacheLRU.containsKey("3"));
        assertEquals(cacheLRU.size(), 2);
        cacheLRU.removeElements(new Object[] { "1", "3" });
        assertFalse(cacheLRU.containsKey("1"));
        assertFalse(cacheLRU.containsKey("3"));
        assertEquals(cacheLRU.size(), 0);

        cacheLRU.put("1", "1");
        cacheLRU.put("2", "2");
        cacheLRU.put("3", "3");
        cacheLRU.clear();
        assertEquals(cacheLRU.size(), 0);
    }
}
