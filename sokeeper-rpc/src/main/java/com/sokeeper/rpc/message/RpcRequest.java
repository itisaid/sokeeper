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

import java.io.Serializable;
import java.util.UUID;

import com.sokeeper.util.Assert;


/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 3287028427386134696L;

    private String            id;
    private String            serviceName;
    private Object[]          arguments;

    public RpcRequest(String serviceName, Object[] arguments) {
        Assert.notNull(serviceName, "serviceName can not be null.");
        Assert.notNull(arguments, "arguments can not be null.");
        this.id = UUID.randomUUID().toString();
        this.serviceName = serviceName;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Object[] getArguments() {
        return arguments;
    }

}
