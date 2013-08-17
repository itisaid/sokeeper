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
package com.sokeeper.persist.service;

import java.util.Collection;

import com.sokeeper.domain.ChangesSubscriber;
import com.sokeeper.domain.resource.ResourceKey;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface SubscriberService {
    /**
     * Remove the subscribers from resource subscriber list by the subscriber's
     * client address and server address.
     *
     * @param clientAddress:the subscriber's client side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @param serverAddress:the subscriber's server side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @return: the removed records number.
     * @throws IllegalArgumentException
     */
    public int removeSubscriber(String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Remove subscribers from resource subscriber list by the subscriber's
     * address and the resource's key.
     *
     * @param resourceKey: can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceType: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceKey.resourceName: could not be empty, otherwise throw
     *            {@link IllegalArgumentException}.
     * @param clientAddress:the subscriber's client side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @param serverAddress:the subscriber's server side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @return: the removed records number.
     * @throws IllegalArgumentException
     */
    public int removeSubscriber(ResourceKey resourceKey, String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Remove the whole server's resource subscriber list, it's useful when the
     * server startup or shutdown, it should call this method to cleanup the
     * subscriber list.Or when server1 crashed, and server2 detect server1's
     * crash event, server2 should cleanup server1's subscriber list. <br>
     *
     * @param serverAddresses:the subscriber's server side addresses.can not be
     *            null otherwise throw {@link IllegalArgumentException},could be
     *            empty,when its empty will do nothing.
     * @return: the impacted records number.
     * @throws IllegalArgumentException
     */
    public int removeSubscribersOfServers(Collection<String> serverAddresses)
            throws IllegalArgumentException;

    /**
     * Add new resourceChangesSubscriber into resource_subscriber table.
     *
     * @param resourceChangesSubscriber:can not be null otherwise throw
     *            {@link IllegalArgumentException}
     * @param resourceChangesSubscriber.resourceType:can not be null otherwise
     *            throw {@link IllegalArgumentException}
     * @param resourceChangesSubscriber.resourceId:can not be null otherwise
     *            throw {@link IllegalArgumentException}
     * @param resourceChangesSubscriber.clientAddress:can not be null otherwise
     *            throw {@link IllegalArgumentException}
     * @param resourceChangesSubscriber.serverAddress:can not be null otherwise
     *            throw {@link IllegalArgumentException}
     * @throws IllegalArgumentException
     */
    public void addResourceChangesSubscriber(ChangesSubscriber resourceChangesSubscriber)
            throws IllegalArgumentException;

    /**
     * Quick method for {@link #addResourceChangesSubscriber(ChangesSubscriber)}
     * .
     *
     * @param resourceType:can not be null otherwise throw
     *            {@link IllegalArgumentException}
     * @param resourceId:can not be null otherwise throw
     *            {@link IllegalArgumentException}
     * @param clientAddress:can not be null otherwise throw
     *            {@link IllegalArgumentException}
     * @param serverAddress:can not be null otherwise throw
     *            {@link IllegalArgumentException}
     * @throws IllegalArgumentException
     */
    public void addResourceChangesSubscriber(String resourceType, String resourceId,
                                             String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Query given clientAddress and serverAddress's subscriber list.
     *
     * @param clientAddress:the subscriber's client side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @param serverAddress:the subscriber's server side address.can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @return: the subscriber list records.
     * @throws IllegalArgumentException
     */
    public Collection<ChangesSubscriber> getSubscribedResources(String clientAddress,
                                                                String serverAddress)
            throws IllegalArgumentException;
}
