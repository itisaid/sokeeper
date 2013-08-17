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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;

import com.sokeeper.exception.RpcException;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcInvocationHandler implements InvocationHandler {

    final private String          targetGroup;
    final private Set<RpcAddress> targets;
    final private RpcIoHandler    rpcIoHandler;
    final private boolean         argsFilterEnabled;

    public RpcInvocationHandler(Set<RpcAddress> targets, String targetGroup,
                                RpcIoHandler rpcIoHandler, boolean argsFilterEnabled) {
        Assert.notNull(rpcIoHandler, "rpcIoHandler can not be null.");
        this.targets = targets;
        this.targetGroup = targetGroup;
        this.rpcIoHandler = rpcIoHandler;
        this.argsFilterEnabled = argsFilterEnabled;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws RpcException, Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }
        return rpcIoHandler.invoke(method, args, targets, targetGroup, argsFilterEnabled);
    }
}
