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

import java.sql.Timestamp;

import com.sokeeper.domain.PersistedConfiguration;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface PersistedConfigurationService {

    /**
     * Get the persistence layer's current time, this time will be used to
     * distinguish the nodes' online status changed time.
     *
     * @return: current persistence layer's time,normally it's the database
     *          current time.
     */
    public Timestamp getCurrentTime();

    /**
     * Get the unified configuration from persistence layer.
     *
     * @return: the configuration.
     */
    public abstract PersistedConfiguration getPersistedConfiguration();

}
