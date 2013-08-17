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

import java.io.Serializable;

import com.sokeeper.util.Assert;
import com.sokeeper.util.RpcAddress;


/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcConnection implements Serializable {
    private static final long                       serialVersionUID = -4329959200461064131L;

    private RpcAddress                              localAddress;
    private RpcAddress                              remoteAddress;
    private static final ThreadLocal<RpcConnection> CURRENT          = new ThreadLocal<RpcConnection>();

    public RpcConnection(RpcAddress localAddress, RpcAddress remoteAddress) {
        Assert.notNull(localAddress, "localAddress can not be null");
        Assert.notNull(remoteAddress, "remoteAddress can not be null");

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public static RpcConnection getCurrentRpcConnection() {
        return CURRENT.get();
    }

    public static void setCurrentRpcConnection(RpcConnection connection) {
        CURRENT.set(connection);
    }

    public RpcAddress getLocalAddress() {
        return localAddress;
    }

    public RpcAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localAddress == null) ? 0 : localAddress.hashCode());
        result = prime * result + ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
        return result;
    }

    public String toString() {
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("L[").append(localAddress.toString()).append("]->R[").append(
                remoteAddress.toString()).append("]");
        return strBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RpcConnection other = (RpcConnection) obj;
        if (localAddress == null) {
            if (other.localAddress != null)
                return false;
        } else if (!localAddress.equals(other.localAddress))
            return false;
        if (remoteAddress == null) {
            if (other.remoteAddress != null)
                return false;
        } else if (!remoteAddress.equals(other.remoteAddress))
            return false;
        return true;
    }
}
