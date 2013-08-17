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
package com.sokeeper.util;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Collection;

import com.sokeeper.util.Assert;
import com.sokeeper.util.NetUtils;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcSocketAddress extends InetSocketAddress implements RpcAddress {

    private static final long serialVersionUID = -8461880182814914148L;

    public static RpcSocketAddress fromFullAddress(String hostnameAndPort) {
        Assert.hasText(hostnameAndPort, "hostnameAndPort can not be empty");
        String[] tokens = hostnameAndPort.split(":");
        Assert.isTrue(tokens.length == 2, "illegal address:" + hostnameAndPort);
        return new RpcSocketAddress(tokens[0], Integer.valueOf(tokens[1]).intValue());
    }

    public RpcSocketAddress(String hostname, int port) {
        super(hostname, port);
    }

    public RpcSocketAddress(InetSocketAddress addr) {
        super(addr.getHostName(), addr.getPort());
    }

    public Collection<String> getFullAddresses(boolean withLocalHost) {
        Collection<String> ips = getIpAddresses(withLocalHost);
        Collection<String> addresses = new HashSet<String>();
        for (String ip : ips) {
            addresses.add(ip + ":" + getPort());
        }
        return addresses;
    }

    public String getIpAddress() {
        return getAddress().getHostAddress();
    }

    public String getFullAddress() {
        return getAddress().getHostAddress() + ":" + this.getPort();
    }

    public Collection<String> getIpAddresses(boolean withLocalHost) {
        Collection<String> addresses = new HashSet<String>();
        if (getAddress().getHostAddress().equals(ALL_ZERO_ADDRESS)) {
            addresses.addAll(NetUtils.getAllLocalAddresses());
        } else {
            addresses.add(getAddress().getHostAddress());
        }
        if (!withLocalHost) {
            addresses.remove(LOCAL_HOST_ADDRESS);
        }
        return addresses;
    }

    public boolean ipEquals(RpcAddress otherAddress) {
        boolean sameIP = false;
        String hostname = ((RpcSocketAddress) otherAddress).getHostName();
        if (hostname != null && getHostName() != null) {
            sameIP = new RpcSocketAddress(hostname, getPort()).equals(this);
        }
        return sameIP;
    }

}
