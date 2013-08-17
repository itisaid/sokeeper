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

import java.sql.Timestamp;

import com.sokeeper.util.Assert;

/**
 * The address and parentNodeId will be unique.
 *
 * @author James Fu (fuyinhai@gmail.com)
 */
public class NodeOnlineStatus extends AddressedEntity {

    private static final long serialVersionUID = 5595272779331257504L;
    private Timestamp         gmtExpired;
    private Boolean           isMaster;

    public NodeOnlineStatus() {
        setServerAddress(NO_SERVER_ADDRESS);
    }

    public Timestamp getGmtExpired() {
        return gmtExpired;
    }

    public void setGmtExpired(Timestamp gmtExpired) {
        this.gmtExpired = gmtExpired;
    }

    public boolean isParentNode() {
        return NO_SERVER_ADDRESS.equals(getServerAddress());
    }

    public Boolean getIsMaster() {
        return isMaster;
    }

    public void setIsMaster(Boolean isMaster) {
        this.isMaster = isMaster;
    }

    public void setServerAddress(String serverAddress) {
        Assert.hasText(serverAddress, "serverAddress could not be null.");
        super.setServerAddress(serverAddress);
    }
}
