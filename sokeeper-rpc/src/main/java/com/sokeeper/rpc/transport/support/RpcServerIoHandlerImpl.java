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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketAcceptor;

import com.sokeeper.rpc.message.HandshakeMessage;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.util.Assert;
import com.sokeeper.util.NamedThreadFactory;
import com.sokeeper.util.NetUtils;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcServerIoHandlerImpl extends RpcIoHandlerImpl {
    private SocketAcceptor  acceptor = null;
    private ExecutorService acceptorThreadPool;

    public RpcServerIoHandlerImpl(RpcConfiguration configuration) {
        super(configuration);
    }

    public boolean startup() {
        boolean succeed = true;
        if (acceptor == null) {
            try {
                acceptorThreadPool = Executors.newCachedThreadPool(new NamedThreadFactory(
                        getThreadPoolName() + "-acceptor-", true));
                acceptor = new SocketAcceptor(Runtime.getRuntime().availableProcessors() + 1,
                        acceptorThreadPool);
                acceptor.getDefaultConfig().getSessionConfig().setReuseAddress(true);
                acceptor.getDefaultConfig().getSessionConfig().setTcpNoDelay(true);
                acceptor.getDefaultConfig().setReuseAddress(true);
                acceptor.getDefaultConfig().setThreadModel(
                        ExecutorThreadModel.getInstance(getThreadPoolName() + "-acceptor", true));
                acceptor.getFilterChain().addLast("codec", getCodecFactory());
                if (configuration.getMainAddress().getPort() > 0
                        && NetUtils.selectAvailablePort(configuration.getMainAddress().getPort()) == configuration
                                .getMainAddress().getPort()) {
                    acceptor.bind((SocketAddress) configuration.getMainAddress(), this);
                    logger.info(getThreadPoolName() + " started at:"
                            + configuration.getMainAddress().toString());
                } else {
                    logger.error("start server:" + configuration.getMainAddress()
                            + " failed due to port:" + configuration.getMainAddress().getPort()
                            + " occupied.");
                    succeed = false;
                }
            } catch (IOException e) {
                logger.error("start server:" + configuration.getMainAddress() + " failed.");
                succeed = false;
            }
        } else {
            logger.warn("please shutdown first:" + getThreadPoolName());
        }
        return succeed;
    }

    public void shutdown() {
        if (acceptor != null) {
            Set<IoSession> sessions = getSessions();
            try {
                acceptor.unbindAll();
            } catch (Throwable e) {
            }
            for (IoSession session : sessions) {
                try {
                    session.close().join();
                } catch (Throwable e) {
                }
            }
            try {
                acceptorThreadPool.shutdown();
            } catch (Throwable e) {
            }
            acceptor = null;
        }
        super.shutdown();
    }

    @Override
    protected IoService getIoService() {
        return acceptor;
    }

    public void publishNewAddedServers(Set<RpcAddress> servers) throws IllegalArgumentException {
        Assert.notNull(servers, "servers can not be null.");
        if (servers.size() > 0) {
            configuration.addServers(servers);
            HandshakeMessage message = new HandshakeMessage();
            message.setServers(servers);
            for (IoSession session : getSessions()) {
                write(message, session);
            }
        }
    }

    @Override
    protected void sendHandshakeMessage(IoSession session) {
        Assert.notNull(session, "session can not be null.");
        HandshakeMessage message = new HandshakeMessage();
        // 1, server tell the client heart beat parameters
        message.addParameter(RpcConfiguration.KEY_RPC_URL_PARAM_HEART_BEAT_SEC, ""
                + configuration.getSecondsOfHb());
        // 2, server tell the client other servers addresses
        message.setServers(configuration.getServers());
        write(message, session);
    }

    protected void onHandshakeMessageGot(IoSession session, HandshakeMessage message) {
        if (message != null && acceptor != null) {
            if (logger.isInfoEnabled()) {
                logger.info("handshakeMessageGot:" + message + " from " + session.toString());
            }
            // 1, server add the client to the groups the client wants join in
            String groups = message.getParameter(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS, "");
            addClientToGroups(session, groups);
        }
    }

    public boolean isAlive() {
        return acceptor != null && acceptor.getManagedServiceAddresses().size() > 0;
    }
}
