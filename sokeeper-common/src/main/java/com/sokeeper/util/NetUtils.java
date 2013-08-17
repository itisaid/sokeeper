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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Collection;
import java.util.Enumeration;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class NetUtils {
    private static Collection<String> localAddresses = null;

    public static Collection<String> getAllLocalAddresses() {
        if (localAddresses == null) {
            Collection<String> all = new HashSet<String>();
            Enumeration<NetworkInterface> interfaces = null;
            try {
                interfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface netI = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = netI.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        all.add(address.getHostAddress());
                    }
                }
            }
            localAddresses = all;
        }
        return localAddresses;
    }

    public static int selectAvailablePort(int preferedPort) {
        int port = -1;
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(preferedPort));
            port = ss.getLocalPort();
        } catch (IOException e) {
            try {
                ss = new ServerSocket();
                ss.bind(null);
                port = ss.getLocalPort();
            } catch (IOException ee) {
            }
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
        return port;
    }
}
