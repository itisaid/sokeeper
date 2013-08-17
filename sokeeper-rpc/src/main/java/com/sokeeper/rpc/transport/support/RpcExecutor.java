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

import java.lang.reflect.InvocationTargetException;


import org.apache.mina.common.IoSession;

import com.sokeeper.rpc.message.RpcRequest;
import com.sokeeper.rpc.message.RpcResponse;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcExecutor implements Runnable {
    final private RpcConnection connection;
    final private IoSession     session;
    final private RpcRequest    request;
    final private RpcInvoker    invoker;

    public RpcExecutor(final RpcConnection connection, final IoSession session,
                       final RpcRequest request, final RpcInvoker invoker) {
        Assert.notNull(connection, "connection can not be null.");
        Assert.notNull(session, "session can not be null.");
        Assert.notNull(request, "request can not be null.");
        Assert.notNull(invoker, "invoker can not be null.");

        this.session = session;
        this.request = request;
        this.invoker = invoker;
        this.connection = connection;
    }

    public void run() {
        Object result = null;
        try {
            RpcConnection.setCurrentRpcConnection(connection);
            result = invoker.invoke(request.getArguments());
        } catch (InvocationTargetException e) {
            result = e.getTargetException();
        } catch (Throwable e) {
            result = e;
        }
        RpcResponse response = new RpcResponse(request.getId(), result);
        session.write(response);
    }
}
