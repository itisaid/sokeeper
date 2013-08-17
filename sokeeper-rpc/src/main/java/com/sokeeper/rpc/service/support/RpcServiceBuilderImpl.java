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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sokeeper.rpc.service.RpcServiceBuilder;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcServiceBuilderImpl implements RpcServiceBuilder {
    private Map<String, Constructor<?>> constructorOfProxiedClazzes = new HashMap<String, Constructor<?>>();
    private final static Class<?>[]     constructorParams           = { InvocationHandler.class };

    @SuppressWarnings("unchecked")
    public <T> T buildRemoteServiceProxy(Class<? extends T> serviceInterface,
                                         Set<RpcAddress> targets, String targetGroup,
                                         RpcIoHandler rpcIoHandler,
                                         boolean argsFilterEnabled)
            throws IllegalArgumentException {
        Assert.notNull(serviceInterface, "serviceInterface can not be null.");
        Assert
                .isTrue(serviceInterface.isInterface(),
                        "only interface can be registered as service");
        Assert.notNull(rpcIoHandler, "rpcIoHandler can not be null.");

        Constructor constructorOfProxiedClazz = constructorOfProxiedClazzes.get(serviceInterface
                .getName());
        if (constructorOfProxiedClazz == null) {
            Class<?> proxiedClazz = Proxy.getProxyClass(serviceInterface.getClassLoader(),
                    new Class<?>[] { serviceInterface });
            try {
                constructorOfProxiedClazz = proxiedClazz.getConstructor(constructorParams);
            } catch (NoSuchMethodException e) {
                throw new InternalError(e.toString());
            }
            constructorOfProxiedClazzes.put(serviceInterface.getName(), constructorOfProxiedClazz);
        }
        try {
            return (T) constructorOfProxiedClazz
                    .newInstance(new Object[] { new RpcInvocationHandler(targets, targetGroup,
                            rpcIoHandler, argsFilterEnabled) });
        } catch (Throwable e) {
            throw new InternalError(e.toString());
        }
    }
}
