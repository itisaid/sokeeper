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
package com.sokeeper.persist.support;

import java.sql.Timestamp;
import java.util.Map;

import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.stereotype.Component;

import com.sokeeper.domain.PersistedConfiguration;
import com.sokeeper.persist.service.PersistedConfigurationService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("persistedConfigurationService")
public class PersistedConfigurationServiceImpl extends SqlMapClientDaoSupport implements
        PersistedConfigurationService {

    public Timestamp getCurrentTime() {
        Timestamp tstamp = (Timestamp) getSqlMapClientTemplate().queryForObject(
                "common.getCurrentTime");
        return tstamp;
    }

    @SuppressWarnings("unchecked")
    public PersistedConfiguration getPersistedConfiguration() {
        Map map = getSqlMapClientTemplate().queryForMap("common.getPersistedConfiguration", null,
                "configKey", "configValue");
        PersistedConfiguration config = new PersistedConfiguration();
        for (Object key : map.keySet()) {
            config.addParameter(key.toString(), map.get(key).toString());
        }
        return config;
    }
}
