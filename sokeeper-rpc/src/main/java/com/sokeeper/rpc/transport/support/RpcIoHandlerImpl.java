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

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.exception.RpcException;
import com.sokeeper.rpc.exception.RpcLocalException;
import com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected;
import com.sokeeper.rpc.exception.RpcLocalExceptionIoWriteToTargetFailed;
import com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets;
import com.sokeeper.rpc.exception.RpcLocalExceptionTimeout;
import com.sokeeper.rpc.exception.RpcRemoteException;
import com.sokeeper.rpc.message.HandshakeMessage;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.rpc.message.RpcRequest;
import com.sokeeper.rpc.message.RpcResponse;
import com.sokeeper.rpc.transport.RpcConnection;
import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.rpc.transport.RpcIoListener;
import com.sokeeper.util.Assert;
import com.sokeeper.util.NamedThreadFactory;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
abstract public class RpcIoHandlerImpl extends IoHandlerAdapter implements RpcIoHandler {
    final static public String                            HEART_BEAT_MSG         = "hb";
    final static public String                            KEY_SESSION_CONNECTION = "_connection_";

    final protected Logger                                logger                 = LoggerFactory
                                                                                         .getLogger(getClass());
    final private Map<String, BlockingQueue<RpcResponse>> responses              = new HashMap<String, BlockingQueue<RpcResponse>>();
    private ExecutorService                               threadPool;
    final private Map<String, RpcInvoker>                 rpcInvokers            = new HashMap<String, RpcInvoker>();
    final private Map<Method, String>                     serviceNames           = new HashMap<Method, String>();
    final private Set<RpcIoListener>                      ioListeners            = new HashSet<RpcIoListener>();

    final protected RpcConfiguration                      configuration;

    public RpcIoHandlerImpl(RpcConfiguration cfg) {
        Assert.notNull(cfg, "configuration can not be null.");
        configuration = cfg;
        threadPool = Executors.newCachedThreadPool(new NamedThreadFactory(getThreadPoolName()
                + "-reqProcessor-", false));
    }

    protected String getThreadPoolName() {
        return getClass().getSimpleName();
    }

    private RpcRequest prepare(Method method, Object[] arguments) {
        RpcRequest request = new RpcRequest(getServiceName(method), arguments);
        BlockingQueue<RpcResponse> queue = new LinkedBlockingQueue<RpcResponse>();
        responses.put(request.getId(), queue);
        return request;
    }

    private RpcResponse get(RpcRequest request, int timeout) {
        String requestId = request.getId();
        BlockingQueue<RpcResponse> queue = responses.get(requestId);
        RpcResponse response = null;
        if (queue != null) {
            try {
                response = queue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
        }
        return response;
    }

    public RpcConfiguration getConfiguration() {
        return configuration;
    }

    public ProtocolCodecFilter getCodecFactory() {
        return new ProtocolCodecFilter(new ObjectSerializationCodecFactory());
    }

    public Object publicToClients(final Method method, Map<RpcAddress, Object> messages)
            throws RpcException, Throwable {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(final Method method,  Object[] args, Set<RpcAddress> targets,
                         String groupName, boolean argsFilterEnabled) throws RpcException,
            Throwable {
        Assert.notNull(method, "method can not be null.");
        if(args==null){
            args=new Object[0];
        }
        if (argsFilterEnabled) {
            Assert.isTrue(args.length == 1 && (args[0] instanceof Map),
                    "when argsFilterEnabled the argument must be Map<RpcAddress,?>");
            targets = (Set<RpcAddress>) ((Map) args[0]).keySet();
            Assert.notEmpty(targets, "the targets can not be null.");
            groupName = null;
        }
        if (targets == null) {
            targets = new HashSet<RpcAddress>();
        }
        String serviceName = getServiceName(method);
        Map<RpcAddress, Object> results = new HashMap<RpcAddress, Object>();
        Map<SocketAddress, IoSession> undelivered = new HashMap<SocketAddress, IoSession>();
        Set<IoSession> sessions = getSessions();
        boolean toAllEnabled = (targets.size() == 0) && (groupName == null);
        for (IoSession session : sessions) {
            if (toAllEnabled) {
                undelivered.put(session.getRemoteAddress(), session);
            } else if (targets.contains(session.getRemoteAddress())) {
                undelivered.put(session.getRemoteAddress(), session);
            } else if (groupName != null) {
                if (isClientInGroup(session, groupName)) {
                    undelivered.put(session.getRemoteAddress(), session);
                }
            }
        }
        for (RpcAddress target : targets) {
            if (!undelivered.containsKey(target)) {
                results.put(target, new RpcLocalExceptionIoTargetIsNotConnected(
                        "target_not_connected[" + target + "] call " + serviceName));
            }
        }
        for (Entry<SocketAddress, IoSession> pair : undelivered.entrySet()) {
            RpcAddress addr = new RpcSocketAddress((InetSocketAddress) pair.getKey());
            Object[] rargs = null;
            if (argsFilterEnabled) {
                Map<RpcAddress, Object> map = new HashMap<RpcAddress, Object>();
                map.put(addr, ((Map<RpcAddress, Object>) args[0]).get(addr));
                rargs = new Object[] { map };
            } else {
                rargs = args;
            }
            RpcRequest request = prepare(method, rargs);
            try {
                write(request, pair.getValue());
                results.put(addr, request);
            } catch (Throwable e) {
                responses.remove(request.getId());
                results.put(addr, new RpcLocalExceptionIoWriteToTargetFailed("write_target_failed["
                        + pair.getKey() + "] call " + serviceName));
            }
        }

        Object result = null;
        RpcLocalExceptionMultipleTargets failures = null;
        boolean timeoutHappened = false;
        for (Entry<RpcAddress, Object> pair : results.entrySet()) {
            if (pair.getValue() instanceof RpcRequest) {
                RpcRequest request = (RpcRequest) pair.getValue();
                try {
                    RpcResponse response = get(request, (timeoutHappened ? 1 : configuration
                            .getTimeout()));
                    if (response == null) {
                        timeoutHappened = true;
                        pair.setValue(new RpcLocalExceptionTimeout("call_target_timeout["
                                + pair.getKey() + "]in[" + configuration.getTimeout() + "ms] call "
                                + serviceName));
                    } else {
                        result = response.getResult();
                        // the remote peer return their own RpcLocalException to me,
                        // but this is meaningless for me so I need wrapper it to RpcRemoteException
                        if ((result != null) && (result instanceof RpcLocalException)) {
                            result = new RpcRemoteException(((RpcLocalException) result)
                                    .getMessage());
                        }
                        pair.setValue(result);
                    }
                } finally {
                    responses.remove(request.getId());
                }
            }
            result = pair.getValue();
            if ((result != null) && (result instanceof Throwable)) {
                if (failures == null) {
                    failures = new RpcLocalExceptionMultipleTargets(serviceName);
                }
                failures.addFailedTarget(pair.getKey(), ((Throwable) result).getMessage());
            }
        }
        if (failures != null && failures.hasFailedTargets()) {
            if (results.size() == 1) {
                throw (Throwable) result;
            } else {
                logger.error(failures.getMessage());
                throw failures;
            }
        }
        invokeFinished(results);
        return result;
    }

    protected void invokeFinished(Map<RpcAddress, Object> results) throws Throwable {
    }

    public boolean isConnected(RpcAddress target) {
        Assert.notNull(target, "target address can not be null.");
        Set<IoSession> sessions = getSessions();
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(target)) {
                return session.isConnected();
            }
        }
        return false;
    }

    public void disconnect(RpcAddress target) {
        Assert.notNull(target, "target can not be null.");
        Set<IoSession> sessions = getSessions();
        for (IoSession session : sessions) {
            if (target.equals(session.getRemoteAddress())) {
                session.close().join();
                break;
            }
        }
    }

    public void disconnectAll() {
        Set<IoSession> sessions = getSessions();
        for (IoSession session : sessions) {
            try {
                session.close().join();
            } catch (Throwable e) {
            }
        }
    }

    abstract protected IoService getIoService();

    public void publishNewAddedServers(Set<RpcAddress> servers) throws IllegalArgumentException {
    }

    /**
     * @return: all the activated sessions.
     */
    protected Set<IoSession> getSessions() {
        Set<IoSession> sessions = Collections.emptySet();
        IoService service = getIoService();
        if (service != null) {
            Set<SocketAddress> targets = service.getManagedServiceAddresses();
            if (targets.size() > 0) {
                sessions = new HashSet<IoSession>();
                for (SocketAddress target : targets) {
                    sessions.addAll(service.getManagedSessions(target));
                }
            }
        }
        return sessions;
    }

    public Collection<RpcConnection> getConnections() {
        Collection<RpcConnection> connections = new HashSet<RpcConnection>();
        for (IoSession session : getSessions()) {
            connections.add(getConnection(session));
        }
        return connections;
    }

    /**
     * The developer can override this method to mock the real remote
     * invocation.
     *
     * @param message: message object can not be null otherwise throw
     *            IllegalArgumentException.
     * @param session:the session object can not be null otherwise throw
     *            IllegalArgumentException.
     */
    protected WriteFuture write(Object message, IoSession session) {
        return session.write(message);
    }

    public String getServiceName(Method method) {
        String serviceName = serviceNames.get(method);
        if (serviceName == null) {
            StringBuffer sb = new StringBuffer();
            sb.append(method.getDeclaringClass().getName() + ".");
            sb.append(method.getName() + "(");
            Class<?>[] params = method.getParameterTypes();
            for (int j = 0; j < params.length; j++) {
                sb.append(params[j].getName());
                if (j < (params.length - 1))
                    sb.append(",");
            }
            sb.append(")");
            serviceName = sb.toString();
            serviceNames.put(method, serviceName);
        }
        return serviceName;
    }

    public int getResponsesSize() {
        return responses.size();
    }

    final public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (status == IdleStatus.WRITER_IDLE) {
            write(HEART_BEAT_MSG, session);
        } else if (status == IdleStatus.READER_IDLE) {
            logger.warn("remote_no_hb_close_session:" + session.toString());
            session.close();
        }
    }

    public void sessionOpened(final IoSession session) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("sessionOpened:" + session.toString());
        }
        sendHandshakeMessage(session);
        if ((getSessions().size() <= configuration.getMaxConnections())
                || configuration.isIpInWhiteList(getConnection(session).getRemoteAddress()
                        .getIpAddress())) {
            setHeartBeat(session, configuration.getSecondsOfHb());
            processIoEvent(session, RpcIoListener.EVENT_CONNECTION_CREATED);
        } else {
            logger.error("max_connection_reached:" + configuration.getMaxConnections()
                    + " close the connection:" + session.toString());
            session.close();
        }
    }

    /**
     * when the session created between the two peers, each peer will send
     * handshake message to remote peer,the handshake message could be some
     * settings information,so the remote peer can do some adjustment according
     * the other peer's settings.
     */
    abstract protected void sendHandshakeMessage(IoSession session);

    /**
     * when the peer got remote peer's handshake message this method will be
     * invoked.
     *
     * @param session:the apache mina io session
     * @param message:the handshake message.
     */
    abstract protected void onHandshakeMessageGot(IoSession session, HandshakeMessage message);

    @SuppressWarnings("unchecked")
    public boolean isClientInGroup(IoSession session, String groupName) {
        if (session != null && groupName != null) {
            Set<String> groups = (Set<String>) session
                    .getAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS);
            if (groups != null && groups.contains(groupName)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void addClientToGroups(IoSession session, String groups) {
        if (groups != null && !"".equals(groups)) {
            String[] groupNames = groups.split(RpcConfiguration.KEY_RPC_URL_PARAM_SEP_TOKEN);
            synchronized (session) {
                Set<String> sets = (Set<String>) session
                        .getAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS);
                if (sets == null) {
                    sets = new HashSet<String>();
                    session.setAttribute(RpcConfiguration.KEY_RPC_URL_PARAM_GROUPS, sets);
                }
                sets.addAll(Arrays.asList(groupNames));
            }
        }
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error("close_session_on_mina_io_exception", cause);
        session.close();
    }

    public void setHeartBeat(IoSession session, int idleTime) {
        Assert.notNull(session, "session can not be null.");
        synchronized (session) {
            idleTime = idleTime < 0 ? 0 : idleTime;
            session.setIdleTime(IdleStatus.WRITER_IDLE, idleTime);
            session.setIdleTime(IdleStatus.READER_IDLE, idleTime * 2);
        }
    }

    protected RpcConnection getConnection(IoSession session) {
        RpcConnection connection = (RpcConnection) session.getAttribute(KEY_SESSION_CONNECTION);
        if (connection == null) {
            connection = new RpcConnection(new RpcSocketAddress((InetSocketAddress) session
                    .getLocalAddress()), new RpcSocketAddress((InetSocketAddress) session
                    .getRemoteAddress()));
            session.setAttribute(KEY_SESSION_CONNECTION, connection);
        }
        return connection;
    }

    private void processIoEvent(IoSession session, int eventType) {
        for (RpcIoListener ioListener : ioListeners) {
            threadPool.execute(new RpcIoListenerExecutor(getConnection(session), eventType,
                    ioListener, this));
        }
    }

    public void sessionClosed(IoSession session) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("sessionClosed:" + session.toString());
        }
        processIoEvent(session, RpcIoListener.EVENT_CONNECTION_CLOSED);
    }

    public void messageReceived(final IoSession session, final Object message) throws Exception {
        if (message != null && session != null) {
            if (message instanceof RpcResponse) {
                RpcResponse response = (RpcResponse) message;
                BlockingQueue<RpcResponse> queue = responses.get(response.getId());
                if (queue != null) {
                    queue.offer(response);
                }
            } else if (message instanceof RpcRequest) {
                RpcRequest request = (RpcRequest) message;
                RpcInvoker invoker = rpcInvokers.get(request.getServiceName());
                if (invoker != null) {
                    threadPool.execute(new RpcExecutor(getConnection(session), session, request,
                            invoker));
                } else {
                    logger.error("no invoker registered for call:" + request.getServiceName());
                    RpcResponse response = new RpcResponse(request.getId(), new RpcRemoteException(
                            "service_not_registered:" + request.getServiceName()));
                    write(response, session);
                }
            } else if (message instanceof HandshakeMessage) {
                threadPool.execute(new Runnable() {
                    public void run() {
                        RpcIoHandlerImpl.this.onHandshakeMessageGot(session,
                                (HandshakeMessage) message);
                    }
                });
            } else if (message instanceof String) {
                if (logger.isInfoEnabled()) {
                    logger.info(message + " " + session.toString());
                }
            } else {
                logger.error("unsupported message:" + message.getClass().getName() + "["
                        + message.toString() + "]");
            }
        }
    }

    public <T> void registerRequestHandler(Class<? super T> serviceInterface, T service) {
        Assert.notNull(serviceInterface, "serviceInterface can not be null.");
        Assert.notNull(service, "service can not be null.");
        for (Method method : serviceInterface.getMethods()) {
            if (method.getDeclaringClass() != Object.class) {
                method.setAccessible(true);
                rpcInvokers.put(getServiceName(method), new RpcInvoker(method, service));
            }
        }
    }

    public void registerIoListener(RpcIoListener ioListener) {
        Assert.notNull(ioListener, "ioListener can not be null.");
        ioListeners.add(ioListener);
    }

    public void unregisterIoListener(RpcIoListener ioListener) {
        Assert.notNull(ioListener, "ioListener can not be null.");
        ioListeners.remove(ioListener);
    }

    public RpcInvoker getRpcInvoker(String serviceName) {
        Assert.notNull(serviceName, "serviceName can not be null.");
        return rpcInvokers.get(serviceName);
    }

    public void shutdown() {
        try {
            threadPool.shutdown();
        } catch (Throwable e) {
        }
        try {
            threadPool = Executors.newCachedThreadPool(new NamedThreadFactory(getThreadPoolName()
                    + "-reqProcessor-", false));
            responses.clear();
        } catch (Throwable e) {
        }
    }
}
