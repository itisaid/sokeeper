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

import java.util.concurrent.atomic.AtomicLong;

import com.sokeeper.rpc.message.RpcRequest;


import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcRequestTest extends TestCase {

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

    public void test_newRpcRequest() throws Throwable {
        long startTime = System.nanoTime();
        Object[] args = new Object[] {};
        for (int i = 0; i < 10; i++) {
            new RpcRequest("serviceName", args);
        }
        long endTime = System.nanoTime();
        System.out.println("cost:" + (endTime - startTime) / 1000000 + " ms");
        AtomicLong aLong=new AtomicLong(Long.MAX_VALUE);
        System.out.println(aLong.get());
        System.out.println(aLong.incrementAndGet());
        System.out.println(aLong.incrementAndGet());
    }

}
