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

import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.RpcIoListener;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcIoListenerExecutor implements Runnable {
    private final RpcConnection connection;
    private final int           eventType;
    private final RpcIoListener ioListener;
    private final RpcIoHandler  ioHandler;

    public RpcIoListenerExecutor(final RpcConnection connection, final int eventType,
                                 final RpcIoListener ioListener, final RpcIoHandler ioHandler) {
        this.connection = connection;
        this.eventType = eventType;
        this.ioListener = ioListener;
        this.ioHandler = ioHandler;
    }

    public void run() {
        switch (eventType) {
            case RpcIoListener.EVENT_CONNECTION_CREATED: {
                ioListener.onConnectionCreated(connection, ioHandler);
            }
                break;
            case RpcIoListener.EVENT_CONNECTION_CLOSED: {
                ioListener.onConnectionClosed(connection, ioHandler);
            }
                break;
        }
    }
}
