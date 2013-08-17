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

import com.sokeeper.domain.ResourceType;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface ResourceTypeService {

    /**
     * Create new resourceType into database.
     *
     * @param resourceType:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param resourceType.name:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @throws IllegalArgumentException
     */
    public void registerResourceType(ResourceType resourceType) throws IllegalArgumentException;

    /**
     * Query the ResourceType from database.
     *
     * @param typeName:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @return: given resource type or null if it does not exist.
     * @throws IllegalArgumentException
     */
    public ResourceType getResourceType(String typeName) throws IllegalArgumentException;

    public void flushCachedResourceTypes();

}
