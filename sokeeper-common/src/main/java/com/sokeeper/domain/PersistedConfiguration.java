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
package com.sokeeper.domain;

import com.sokeeper.util.MapParameters;

/**
 * The address and parentNodeId will be unique.
 *
 * @author James Fu (fuyinhai@gmail.com)
 */
public class PersistedConfiguration extends MapParameters {
    private static final long   serialVersionUID                              = -8470261819746028399L;

    public static final String  KEY_SECONDS_OF_RESOURCE_CHANGES_WATCHER_TIMER = "secondsOfResourceChangesWatcherTimer";
    public static final String  KEY_SECONDS_OF_PRESENCE_TIMER                 = "secondsOfPresenceTimer";
    public static final String  KEY_SECONDS_OF_NODE_KEEP_ALIVE                = "secondsOfNodeKeepAlive";
    public static final String  KEY_SUFFIX_OF_SERVER                          = "suffixOfServer";
    public static final String  KEY_MAX_CACHED_ENTITIES                       = "maxCachedEntities";

    public static final int     DEFAULT_MAX_CACHED_ENTITIES                   = 100000;
    private static final String DEFAULT_SUFFIX_OF_SERVER                      = "/server?timeout_ms=5000&max_connections=500&hb_sec=10";

    private int                 secondsOfPresenceTimer                        = -1;
    private int                 secondsOfNodeKeepAlive                        = -1;

    public int getSecondsOfPresenceTimer() {
        if (secondsOfPresenceTimer < 0) {
            secondsOfPresenceTimer = getParameter(KEY_SECONDS_OF_PRESENCE_TIMER, 5);
        }
        return secondsOfPresenceTimer;
    }

    public int getSecondsOfNodeKeepAlive() {
        if (secondsOfNodeKeepAlive < 0) {
            secondsOfNodeKeepAlive = getParameter(KEY_SECONDS_OF_NODE_KEEP_ALIVE, 20);
        }
        return secondsOfNodeKeepAlive;
    }

    public void setSecondsOfPresenceTimer(int secondsOfTimerPeriod) {
        this.secondsOfPresenceTimer = secondsOfTimerPeriod;
    }

    public void setSecondsOfNodeKeepAlive(int secondsOfNodeKeepAlive) {
        this.secondsOfNodeKeepAlive = secondsOfNodeKeepAlive;
    }

    public String getSuffixOfServer() {
        return this.getParameter(KEY_SUFFIX_OF_SERVER, DEFAULT_SUFFIX_OF_SERVER);
    }

    public int getSecondsOfResourceChangesWatcherTimer() {
        return this.getParameter(KEY_SECONDS_OF_RESOURCE_CHANGES_WATCHER_TIMER, 1);
    }

    public int getMaxCachedEntities() {
        return this.getParameter(KEY_MAX_CACHED_ENTITIES, DEFAULT_MAX_CACHED_ENTITIES);
    }
}
