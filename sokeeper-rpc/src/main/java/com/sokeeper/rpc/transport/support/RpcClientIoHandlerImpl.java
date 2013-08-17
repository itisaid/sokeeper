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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketConnector;

import com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected;
import com.sokeeper.rpc.message.HandshakeMessage;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.util.Assert;
import com.sokeeper.util.MapParameters;
import com.sokeeper.util.NamedThreadFactory;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcClientIoHandlerImpl extends RpcIoHandlerImpl implements Runnable {
    private SocketConnector connector       = null;
    private volatile Thread reconnectThread = null;
    private ExecutorService connectorThreadPool;

    public RpcClientIoHandlerImpl(RpcConfiguration configuration) {
        super(configuration);
    }

    public boolean startup() {
        boolean succeed = true;
        if (connector == null) {
            connectorThreadPool = Executors.newCachedThreadPool(new NamedThreadFactory(
                    getThreadPoolName() + "-connector-", true));
            connector = new SocketConnector(Runtime.getRuntime().availableProcessors() + 1,
                    connectorThreadPool);
            connector.getDefaultConfig().getSessionConfig().setTcpNoDelay(true);
            connector.getDefaultConfig().getSessionConfig().setReuseAddress(true);
            connector.getDefaultConfig().setThreadModel(
                    ExecutorThreadModel.getInstance(getThreadPoolName() + "-connector", true));
            connector.getFilterChain().addLast("codec", getCodecFactory());
            for (int triedTimes = 0; triedTimes < configuration.getStartupRetryTimes(); triedTimes++) {
                succeed = connectServers();
                if (!succeed && (configuration.getStartupRetryTimes() > 1)) {
                    try {
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                } else {
                    break;
                }
            }
            if (configuration.getAutoReconnectInMs() > 0) {
                startConnectThread();
            }
        } else {
            logger.warn("please shutdown first:" + getThreadPoolName());
        }
        return succeed;
    }

    public void shutdown() {
        if (connector != null) {
            stopConnectThread();

            Set<IoSession> sessions = getSessions();
            for (IoSession session : sessions) {
                try {
                    session.close().join();
                } catch (Throwable e) {
                }
            }

            try {
                connectorThreadPool.shutdown();
            } catch (Throwable e) {
            }
            connector.setWorkerTimeout(0);
            connector = null;
        }
        super.shutdown();
    }

    public void run() {
        while (Thread.currentThread() == reconnectThread) {
            try {
                connectServers();
                Thread.sleep(configuration.getAutoReconnectInMs());
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private boolean connectServers() {
        boolean connected = true;
        switch (configuration.getConnectPolicy()) {
            case RpcConfiguration.CONNECT_POLICY_ALL: {
                for (RpcAddress address : configuration.getServers()) {
                    if (!connect((RpcSocketAddress) address)) {
                        connected = false;
                    }
                }
            }
                break;
            case RpcConfiguration.CONNECT_POLICY_MAIN: {
                connected = connect((RpcSocketAddress) configuration.getMainAddress());
            }
                break;
            case RpcConfiguration.CONNECT_POLICY_ANYONE: {
                if (getSessions().size() == 0) {
                    connected = false;
                    RpcAddress[] sroted = configuration.getOrderedServers();
                    for (RpcAddress address : sroted) {
                        if (connect((RpcSocketAddress) address)) {
                            connected = true;
                            configuration.recordAccessedTime(address);
                            break;
                        }
                    }
                }
            }
        }
        return connected;
    }

    private void startConnectThread() {
        reconnectThread = new Thread(this);
        reconnectThread.setDaemon(true);
        reconnectThread.setName(getThreadPoolName() + "-reconnect");
        reconnectThread.start();
    }

    private void stopConnectThread() {
        if (reconnectThread != null) {
            Thread oldThread = reconnectThread;
            try {
                reconnectThread.interrupt();
            } catch (Throwable e) {
            }
            reconnectThread = null;
            try {
                oldThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private boolean connect(RpcSocketAddress address) {
        boolean connected = true;
        if (!isConnected(address)) {
            connected = false;
            try {
                Assert.state(connector != null, "the connector has been terminated.");
                ConnectFuture future = connector.connect(address, this);
                future.join();
                IoSession session = future.getSession();
                if (session != null && logger.isInfoEnabled()) {
                    logger.info("connect:" + address + " succeed.");
                    connected = true;
                }
            } catch (Throwable e) {
                logger.error("connect:" + address + " failed.");
            }
        }
        return connected;
    }

    protected void invokeFinished(Map<RpcAddress, Object> results) throws Throwable {
        if (results.size() == 0) {
            throw new RpcLocalExceptionIoTargetIsNotConnected("target_not_connected["
                    + configuration.getMainAddress() + "]");
        }
    }

    @Override
    protected IoService getIoService() {
        return connector;
    }

    @Override
    protected void sendHandshakeMessage(IoSession session) {
        Assert.notNull(session, "session can not be null.");
        HandshakeMessage message = new HandshakeMessage();
        MapParameters parameters = configuration.getParameters();
        // 1, client tell the server the groups the client want to join in
        if (parameters.hasParameter(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS)) {
            message.addParameter(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS, configuration
                    .getParameters().getParameter(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS, ""));
        }
        write(message, session);
    }

    protected void onHandshakeMessageGot(IoSession session, HandshakeMessage message) {
        if (message != null) {
            if (logger.isInfoEnabled()) {
                logger.info("handshakeMessageGot:" + message + " from " + session.toString());
            }
            // 1, set the heart beat by using the server side's configuration for heart beat
            if (message.hasParameter(RpcConfiguration.KEY_RPC_URL_PARAM_HEART_BEAT_SEC)) {
                setHeartBeat(session, message.getParameter(
                        RpcConfiguration.KEY_RPC_URL_PARAM_HEART_BEAT_SEC, configuration
                                .getSecondsOfHb()));
            }
            if (configuration.isAcceptServersThroughHandshake()) {
                // 2, import the server side's servers
                //    if the servers contains 127.0.0.1 and the current session's remote address is not 127.0.0.1 we should filter it out
                //    if the server address is 0.0.0.0 we should filter it out
                Set<RpcAddress> servers = new HashSet<RpcAddress>();
                if (message.getServers() != null) {
                    boolean localhostConnected = getConnection(session).getRemoteAddress()
                            .getIpAddress().equals(RpcAddress.LOCAL_HOST_ADDRESS);
                    for (RpcAddress server : message.getServers()) {
                        String ip = server.getIpAddress();
                        if (!ip.equals(RpcAddress.ALL_ZERO_ADDRESS)) {
                            if (localhostConnected || !ip.equals(RpcAddress.LOCAL_HOST_ADDRESS)) {
                                servers.add(server);
                            }
                        }
                    }
                }
                configuration.addServers(servers);
            }
        }
    }

    public boolean isAlive() {
        return getSessions().size() > 0;
    }
}
