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
package com.sokeeper.rpc.service;

import java.util.Set;

import com.sokeeper.rpc.transport.RpcIoHandler;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface RpcServiceBuilder {

    /**
     * This method is used to build the remote service's wrapper instance.
     *
     * @param serviceInterface: could not be null and must be an interface.
     * @param targets: the target peers' address set,could be null, if it's null
     *            all targets will be called.
     * @param targetGroup: the target peers' group, if the peers in the group it
     *            will be invoked.
     * @param rpcIoHandler: could not be null.
     * @param argsFilterEnabled:
     *            {@link RpcIoHandler#invoke(java.lang.reflect.Method, Object[], Set, String, boolean)}
     * @return: the remote services' delegated instance on local which support
     *          serviceInterface.
     * @throws IllegalArgumentException
     */
    public abstract <T> T buildRemoteServiceProxy(Class<? extends T> serviceInterface,
                                                  Set<RpcAddress> targets, String targetGroup,
                                                  RpcIoHandler rpcIoHandler,
                                                  boolean argsFilterEnabled)
            throws IllegalArgumentException;
}
