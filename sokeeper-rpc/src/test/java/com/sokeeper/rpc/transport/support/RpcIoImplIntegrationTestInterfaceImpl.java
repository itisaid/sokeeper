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
package com.sokeeper.rpc.transport.support;

import java.util.Map;

import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcIoImplIntegrationTestInterfaceImpl implements RpcIoImplIntegrationTestInterface {
    public Object        msg;

    public RpcConnection connection;

    public RpcIoImplIntegrationTestInterfaceImpl(Object msg) {
        this.msg = msg;
    }

    public String sayHello(String msg) throws Throwable {
        if (this.msg != null && this.msg instanceof Throwable) {
            throw (Throwable) this.msg;
        }
        connection = RpcConnection.getCurrentRpcConnection();
        return msg;
    }

    public String sayHello(Map<RpcAddress, String> messages) throws Throwable {
        return null;
    }

}
