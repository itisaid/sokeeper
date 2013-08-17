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

import java.util.Set;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;

import com.sokeeper.rpc.message.HandshakeMessage;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.transport.support.RpcIoHandlerImpl;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class MockRpcIoHandlerImpl extends RpcIoHandlerImpl {

    public MockRpcIoHandlerImpl(RpcConfiguration configuration) {
        super(configuration);
    }

    public boolean startup() {
        return true;
    }

    @Override
    protected Set<IoSession> getSessions() {
        return null;
    }

    @Override
    protected void onHandshakeMessageGot(IoSession session, HandshakeMessage message) {
    }

    @Override
    protected IoService getIoService() {
        return null;
    }

    @Override
    protected void sendHandshakeMessage(IoSession session) {
    }

    public boolean isAlive() {
        return true;
    }

}
