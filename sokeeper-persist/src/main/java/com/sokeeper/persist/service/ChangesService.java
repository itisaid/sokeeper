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

import com.sokeeper.domain.AssociationChangesEvent;
import com.sokeeper.domain.ResourceChanges;
import com.sokeeper.domain.ResourceChangesEvent;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface ChangesService {
    /**
     * Record resource change events with owner died also remove the died
     * resource from resources table. Given server1 and client1, client1
     * connected with server1.At the beginning client1 created a resource
     * through server1, at this point the {@link ChangesService} will create a
     * resource changes record.Later on when client1 disconnected or client1
     * crashed whatever server1 detected client1 dropped from server1, server1
     * will responsible for update all the resource changes records to status
     * {@link #OWNER_DIED}. During the update operation: if the record's status
     * is {@link #CHANGES_DELETED} or {@link #OWNER_DIED},the record will not be
     * updated again;if the record's status is {@link #CHANGES_CREATED} or
     * {@link #CHANGES_UPDATED}, the record's gmtModified with be updated with
     * persistenceLayer'sCurrentTime,the changes field will be updated to
     * {@link #OWNER_DIED},the sequence field will be updated to the next value
     * in the sequence table.
     *
     * @param clientAddress: the client node's address, can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @param serverAddress: the server node's address, can not be null
     *            otherwise throw {@link IllegalArgumentException}.
     * @return: how many records are updated.
     * @throws IllegalArgumentException
     */
    public int updateStatusToOwnerDied(String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Record resource change events with owner died also remove the died
     * resource from resources table. Regarding
     * {@link #updateStatusToOwnerDied(String, String)}, in case of server1
     * crashed,so server1 don't have ability to perform those updating, if other
     * servers e.g.:server2 is alive, server2 can detect server1's crash
     * event,when server2 detected server1's crash event, server2 should take
     * responsibility to update all the server1's resource changes records'
     * status to {@link #OWNER_DIED}.Same rule with
     * {@link #updateStatusToOwnerDied(String, String)} will be followed when
     * perform the updating for each qualified record.
     *
     * @param diedServerAddresses:the crashed server's addresses can not be null
     *            but could be empty,when it's empty will do nothing.
     * @return: how many records are updated.
     * @throws IllegalArgumentException
     */
    public int updateStatusToServerDied(Collection<String> diedServerAddresses)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'CREATED', the
     * clientAddress will be filled with
     * {@link ResourceChanges#NO_CLIENT_ADDRESS},the serverAddress will be
     * filled with {@link ResourceChanges#NO_SERVER_ADDRESS}.So later on,when
     * the owner off line this changes will not be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceCreatedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'CREATED', the
     * clientAddress will be filled with passed in clientAddress,the
     * serverAddress will be filled with passed in serverAddress.So later
     * on,when the owner off line this changes will be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param clientAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param serverAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceCreatedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'UPDATED', the
     * clientAddress will be filled with
     * {@link ResourceChanges#NO_CLIENT_ADDRESS},the serverAddress will be
     * filled with {@link ResourceChanges#NO_SERVER_ADDRESS}.So later on,when
     * the owner off line this changes will not be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceUpdatedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'UPDATED', the
     * clientAddress will be filled with passed in clientAddress,the
     * serverAddress will be filled with passed in serverAddress.So later
     * on,when the owner off line this changes will be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param clientAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param serverAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceUpdatedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'DELETED', the
     * clientAddress will be filled with
     * {@link ResourceChanges#NO_CLIENT_ADDRESS},the serverAddress will be
     * filled with {@link ResourceChanges#NO_SERVER_ADDRESS}.So later on,when
     * the owner off line this changes will not be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceRemovedChanges(String resourceType, String resourceId)
            throws IllegalArgumentException;

    /**
     * Add resource changes record into persistenceLayer or update it when the
     * record already there.The changes field will be filled with 'DELETED', the
     * clientAddress will be filled with passed in clientAddress,the
     * serverAddress will be filled with passed in serverAddress.So later
     * on,when the owner off line this changes will be updated to
     * {@link ResourceChanges#OWNER_DIED}.No matter add or update the sequence
     * field will be updated with the next value of sequence.
     *
     * @param resourceType:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceId:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param clientAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param serverAddress:can not be null or empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: impacted records number(0 or 1).
     * @throws IllegalArgumentException
     */
    public int addOrUpdateResourceRemovedChanges(String resourceType, String resourceId,
                                                 String clientAddress, String serverAddress)
            throws IllegalArgumentException;

    /**
     * Query given servers(should comes from one server node) subscribed
     * resource changes and the subscribers information.With this method call,
     * we can know the given resource changes has been subscribed by which
     * client(s),so, later on we can publish the changes to given client(s).
     *
     * @param sequenceFrom: should be >= 0 otherwise throw
     *            {@link IllegalArgumentException}.
     * @param sequenceTo: should be >= sequenceFrom otherwise throw
     *            {@link IllegalArgumentException}.
     * @param subscribeServers:can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the records of BaseResourceChanges,each record taken:the
     *          subscriber client address information,changed resource's
     *          type,changed resource's id and the changes.
     * @throws IllegalArgumentException
     */
    public Collection<ResourceChangesEvent> listResourceChangesEvent(
                                                                     Long sequenceFrom,
                                                                     Long sequenceTo,
                                                                     Collection<String> subscribeServers)
            throws IllegalArgumentException;

    /**
     * Query given client subscribed resource changes.
     *
     * @param sequenceFrom: refer to {
     *            {@link #listResourceChangesEvent(Long, Long, Collection)}
     * @param sequenceTo: refer to {
     *            {@link #listResourceChangesEvent(Long, Long, Collection)}
     * @param clientAddress: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param serverAddress: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the resource changes events.
     * @throws IllegalArgumentException
     */
    public Collection<ResourceChangesEvent> listResourceChangesEvent(Long sequenceFrom,
                                                                     Long sequenceTo,
                                                                     String clientAddress,
                                                                     String serverAddress)
            throws IllegalArgumentException;

    /**
     * Query given servers(should comes from one server node) subscribed
     * association changes and the subscribers information.With this method
     * call, we can know the given association changes has been subscribed by
     * which client(s),so, later on we can publish the changes to given
     * client(s).
     *
     * @param sequenceFrom: should be >= 0 otherwise throw
     *            {@link IllegalArgumentException}.
     * @param sequenceTo: should be >= sequenceFrom otherwise throw
     *            {@link IllegalArgumentException}.
     * @param subscribeServers:can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the records of BaseResourceChanges,each record taken:the
     *          subscriber client address information,changed resource's
     *          type,changed resource's id and the changes.
     * @throws IllegalArgumentException
     */
    public Collection<AssociationChangesEvent> listAssociationChangesEvent(
                                                                           Long sequenceFrom,
                                                                           Long sequenceTo,
                                                                           Collection<String> servers)
            throws IllegalArgumentException;

    /**
     * Query given client subscribed association changes.
     *
     * @param sequenceFrom: refer to {
     *            {@link #listAssociationChangesEvent(Long, Long, Collection)}.
     * @param sequenceTo: refer to {
     *            {@link #listAssociationChangesEvent(Long, Long, Collection)}.
     * @param clientAddress: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @param serverAddress: can not be empty otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: the association changes events.
     * @throws IllegalArgumentException
     */
    public Collection<AssociationChangesEvent> listAssociationChangesEvent(Long sequenceFrom,
                                                                           Long sequenceTo,
                                                                           String clientAddress,
                                                                           String serverAddress)
            throws IllegalArgumentException;

    /**
     * Get the current resource changes' sequence from persistence layer.
     */
    public Long getCurrentSequenceOfChanges();
}
