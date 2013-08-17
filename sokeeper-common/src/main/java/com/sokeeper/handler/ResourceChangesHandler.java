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
package com.sokeeper.handler;

import java.util.Collection;
import java.util.Map;

import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.ResourceChangesEvent;
import com.sokeeper.exception.RpcException;
import com.sokeeper.util.RpcAddress;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface ResourceChangesHandler {
    /**
     * This is the resource / association changes call back handler.
     *
     * @param changes:the changes which contains {@link ResourceChangesEvent} or
     *            {@link AssociationChangesEvent} or both.
     * @throws RpcException
     */
    public void onResourcesChanged(Map<RpcAddress, Collection<ChangesEvent>> changes)
            throws RpcException;
}
