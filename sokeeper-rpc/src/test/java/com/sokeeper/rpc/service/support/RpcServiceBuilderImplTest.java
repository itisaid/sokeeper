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
package com.sokeeper.rpc.service.support;

import java.lang.reflect.Method;
import java.util.Set;

import com.sokeeper.exception.RpcException;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.service.support.RpcServiceBuilderImpl;
import com.sokeeper.rpc.transport.support.MockRpcIoHandlerImpl;
import com.sokeeper.util.RpcAddress;

import junit.framework.TestCase;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcServiceBuilderImplTest extends TestCase {

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

    public void test_buildRemoteServiceProxy() throws Throwable {
        RpcServiceBuilderImpl builder = new RpcServiceBuilderImpl();
        try {
            builder.buildRemoteServiceProxy(null, null, null, null, false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            builder.buildRemoteServiceProxy(RpcServiceBuilderImplTestInterface.class, null, null,
                    null, false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            builder.buildRemoteServiceProxy(RpcServiceBuilderImplTest.class, null, null,
                    new MockRpcIoHandlerImpl(new RpcConfiguration("tcp://localhost:9010/server")) {

                        public Object invoke(Method method, Object[] args, Set<RpcAddress> targets,
                                             String targetGroup, boolean argsFilterEnabled)
                                throws RpcException, Throwable {
                            return null;
                        }
                    }, false);
            fail("only interface can be registered as service");
        } catch (IllegalArgumentException e) {
        }

        RpcServiceBuilderImplTestInterface service = builder.buildRemoteServiceProxy(
                RpcServiceBuilderImplTestInterface.class, null, null, new MockRpcIoHandlerImpl(
                        new RpcConfiguration("tcp://localhost:9010/server")) {
                    public Object invoke(Method method, Object[] args, Set<RpcAddress> targets,
                                         String targetGroup, boolean argsFilterEnabled)
                            throws RpcException, Throwable {
                        return null;
                    }
                }, false);
        assertNotNull(service);
        assertNull(service.sayHello());
        assertNull(service.toString());

    }
}
