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
package com.sokeeper.rpc.transport;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import com.sokeeper.exception.RpcException;
import com.sokeeper.rpc.message.RpcConfiguration;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface RpcIoHandler {
    /**
     * startup the ioHandler, includes initialize the related resources and
     * thread pool.
     *
     * @return: if the ioHandler started successfully return true otherwise
     *          false.
     */
    public boolean startup();

    /**
     * shutdown the ioHandler,also release the resources and thread pool.
     */
    public void shutdown();

    /**
     * check whether the ioHandler is running or not.
     *
     * @return: true if the ioHandler is running otherwise return false.
     */
    public boolean isAlive();

    /**
     * register the remote request's handler, when the remote peer send requests
     * to the ioHandler,the ioHandler will forward the requests to the
     * registered service.
     *
     * @param <T>:the service's interface type.
     * @param serviceInterface:the service interface class.
     * @param service: the implementation for the interface.
     */
    public <T> void registerRequestHandler(Class<? super T> serviceInterface, T service);

    /**
     * register the listener to the IO event listen queue.
     *
     * @param ioListener
     */
    public void registerIoListener(RpcIoListener ioListener);

    /**
     * unregister the listener from the IO event listen queue.
     *
     * @param ioListener
     */
    public void unregisterIoListener(RpcIoListener ioListener);

    /**
     * check whether the IO has connections connected.
     *
     * @param target: the remote peer's address could not be null otherwise
     *            throw {@link IllegalArgumentException}
     * @return: true if there has connection connected.
     */
    public boolean isConnected(RpcAddress target);

    /**
     * close the connection through the remote peer's address.
     *
     * @param target not be null otherwise throw
     *            {@link IllegalArgumentException}
     */
    public void disconnect(RpcAddress target);

    /**
     * Disconnect all connected connections.
     */
    public void disconnectAll();

    /**
     * Get the configuration.
     *
     * @return: the configuration.
     */
    public RpcConfiguration getConfiguration();

    /**
     * Get all the connected connections.
     *
     * @return: the connected connections, could be empty, but impossible be
     *          null.
     */
    public Collection<RpcConnection> getConnections();

    /**
     * Let the remote peer know new servers added into the service network.
     *
     * @param servers:can not be null, otherwise throw
     *            {@link IllegalArgumentException}
     * @throws IllegalArgumentException
     */
    public void publishNewAddedServers(Set<RpcAddress> servers) throws IllegalArgumentException;

    /**
     * This method will call the remote peers whose address in the targets set
     * or the remote peer in the given group. <li>targets is empty and
     * groupName==null: all the activated peers will be invoked</li> <li>targets
     * is empty and groupName!=null: the peers in the given group will be
     * invoked</li> <li>targets not empty and groupName==null: the peers in the
     * targets will be invoked</li> <li>targets not empty and groupName!=null:
     * the peers in the targets or in the group will be invoked</li>
     *
     * @param method: the remote call method information,can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @param args: the arguments for the method can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param targets: the remote peers addresses set, could be null or
     *            empty,when it's null or empty and the groupName is null,all
     *            the activated remote peers will be invoked,otherwise the peer
     *            whose address included in the targets will be invoked.
     * @param groupName: when call multiple peers, we can just send the call to
     *            the given group's peers.
     * @return: when the whole remote finished without exception will return the
     *          last remote peer's returned result,the result could be null or
     *          desired type.
     * @throws RpcException:
     *             <ul>
     *             <li>throw
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionIoTargetIsNotConnected}
     *             if the remote peer can not be connected.</li>
     *             <li>throw
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionIoWriteToTargetFailed}
     *             if the remote peer is connected but when we write message to
     *             it failed.</li>
     *             <li>throw
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionTimeout}
     *             after the message sent to remote peer but can not receive
     *             response within given timeout time.</li>
     *             <li>throw
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets}
     *             if the request sent to multiple remote peers and any of them
     *             has exception happened,all the exceptions from each remote
     *             peer will be collected and wrapped to this exception,the
     *             caller can call the
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets#getFailedTargets()}
     *             to know which remote peer failed also can get the detailed
     *             failure reason by call
     *             {@link com.sokeeper.rpc.exception.RpcLocalExceptionMultipleTargets#getFailedMessage(RpcAddress)}
     *             .</li>
     *             <li>throw {@link com.sokeeper.rpc.exception.RpcRemoteException}
     *             when the response from remote peer is
     *             {@link com.sokeeper.rpc.exception.RpcLocalException} but this
     *             is the remote peer's local RPC exception and it's meaningless
     *             for the current local peer,so,we need wrapper it.</li>
     *             </ul>
     * @param argsFilterEnabled: whether re-organize the arguments according to
     *            remote target's address.When it's true the targets and
     *            targetGroup parameters will be ignored,instead the targets
     *            will be extracted from arggs[0].keySet().To help understanding
     *            this parameter,let's take this scenario: one server has
     *            multiple clients,the server wants to deliver messages to each
     *            client,but different client should receive different
     *            message(s),from the server perspective,the code looks like:
     *            <code><pre>
     *                Map&lt;RpcAddress,Message&gt; messages=new HashMap&lt;RpcAddress,Message&gt;();
     *                messages.put(client1RpcAddress,messageToClient1);
     *                messages.put(client2RpcAddress,messageToClient2);
     *                serviceHandler.deliver(messages);
     *                //the underling call will be
     *                invoke( method, new Object[]{messages},null,null,true);
     *            </pre></code><br/>
     *            At this point, the server don't want deliver all message to
     *            all clients, instead it want client1 receive messageToClient1,
     *            client2 want to receive messageToClient2,if we set
     *            argsFilterEnabled to true, the back end RpcFramework will
     *            re-organize the parameter(s) according to each client<br/>
     *            <code><pre>
     *                for( client: clients ){
     *                    Map&lt;RpcAddress,Message&gt; messageToClient=new HashMap&lt;RpcAddress,Message&gt;();
     *                    messageToClient.put(client.getRpcAddress(),messages.get(client));
     *                    sendToClient(messageToClient);
     *                }
     *            </pre></code>
     * @throws Throwable:when any of the real business(non {@link RpcException})
     *             exception happened will throw it.
     */
    public Object invoke(Method method, Object[] args, Set<RpcAddress> targets, String targetGroup,
                         boolean argsFilterEnabled) throws RpcException, Throwable;
}
