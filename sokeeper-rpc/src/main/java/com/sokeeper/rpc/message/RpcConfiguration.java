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
package com.sokeeper.rpc.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sokeeper.util.Assert;
import com.sokeeper.util.MapParameters;
import com.sokeeper.util.RpcAddress;
import com.sokeeper.util.RpcSocketAddress;
import com.sokeeper.util.UrlParser;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcConfiguration implements Serializable {
    final protected static Logger               logger                                = LoggerFactory
                                                                                              .getLogger(RpcConfiguration.class);

    private static final long                   serialVersionUID                      = 7103304504478215402L;
    final static public String                  KEY_RPC_URL_PARAM_TIMEOUT_MS          = "timeout_ms";
    final static public String                  KEY_RPC_URL_PARAM_MAX_CONNECTIONS     = "max_connections";
    final static public String                  KEY_RPC_URL_PARAM_AUTO_RECONNECT_MS   = "auto_reconnect_ms";
    final static public String                  KEY_RPC_URL_PARAM_HEART_BEAT_SEC      = "hb_sec";
    final static public String                  KEY_RPC_URL_PARAM_SERVERS             = "servers";
    final static public String                  KEY_RPC_URL_PARAM_STARTUP_RETRY_TIMES = "startup_retry_times";

    final static public String                  KEY_RPC_URL_PARAM_GROUPS              = "groups";
    final static public String                  KEY_RPC_URL_PARAM_SEP_TOKEN           = ",";

    final static public String                  KEY_RPC_URL_PARAM_CONNECT_POLICY      = "connect_policy";

    final static public int                     CONNECT_POLICY_ALL                    = 0;
    final static public int                     CONNECT_POLICY_MAIN                   = 1;
    final static public int                     CONNECT_POLICY_ANYONE                 = 2;
    final static public String[]                CONNECT_POLICIES                      = new String[] {
            "all", "main", "anyone"                                                  };
    final private int                           timeout;
    final private int                           startupRetryTimes;
    final private int                           maxConnections;
    final private int                           autoReconnectInMs;
    private ConcurrentHashMap<RpcAddress, Long> serversWithAccessedTime;
    private RpcAddress                          mainServerAddress;
    private boolean                             acceptServersThroughHandshake         = true;
    final private int                           secondsOfHb;
    private int                                 connectPolicy;
    private MapParameters                       parameters;
    private Collection<String>                  whiteIpList                           = new HashSet<String>();

    /**
     * @param url: can not be null, and the format like this:
     *            <ul>
     *            client:
     *            <li>
     *            tcp://server_ip_address:port/client?timeout_ms=5000&auto_reconnect_ms
     *            =5000&servers=server1:port,server2:port,server3:port&
     *            connect_policy=all&groups=group1,group2
     *            <li>
     *            tcp://server_ip_address:port/client?timeout_ms=5000&auto_reconnect_ms
     *            =5000&servers=server1:port,server2:port,server3:port&
     *            connect_policy=main&groups=group1,group2
     *            <li>
     *            tcp://server_ip_address:port/client?timeout_ms=5000&auto_reconnect_ms
     *            =5000&servers=server1:port,server2:port,server3:port&
     *            connect_policy=anyone&groups=group1,group2
     *            </ul>
     *            <ul>
     *            server:
     *            <li>
     *            tcp://server_ip_address:port/client?timeout_ms=5000&max_connections
     *            =500&hb_sec=10&servers=sibling_server1:port,silbling_server2:
     *            port
     *            </ul>
     */
    public RpcConfiguration(String url) {
        UrlParser rpcUrl = new UrlParser(url);
        parameters = rpcUrl.getParameters();
        timeout = parameters.getParameter(RpcConfiguration.KEY_RPC_URL_PARAM_TIMEOUT_MS, 5000);
        maxConnections = parameters.getParameter(
                RpcConfiguration.KEY_RPC_URL_PARAM_MAX_CONNECTIONS, 500);
        autoReconnectInMs = parameters.getParameter(
                RpcConfiguration.KEY_RPC_URL_PARAM_AUTO_RECONNECT_MS, -1);
        mainServerAddress = new RpcSocketAddress(rpcUrl.getHost(), rpcUrl.getPort());
        serversWithAccessedTime = new ConcurrentHashMap<RpcAddress, Long>();
        serversWithAccessedTime.put(mainServerAddress, System.nanoTime());
        addServers(parameters.getParameter(KEY_RPC_URL_PARAM_SERVERS, ""));
        connectPolicy = parameters.getIndexedParameter(KEY_RPC_URL_PARAM_CONNECT_POLICY,
                CONNECT_POLICIES, CONNECT_POLICY_ALL, true);
        secondsOfHb = parameters
                .getParameter(RpcConfiguration.KEY_RPC_URL_PARAM_HEART_BEAT_SEC, -1);
        Assert.isTrue(timeout > 0, "timeout should > 0.");
        startupRetryTimes = parameters.getParameter(
                RpcConfiguration.KEY_RPC_URL_PARAM_STARTUP_RETRY_TIMES, 1);
        Assert.isTrue(startupRetryTimes > 0, "startupRetryTimes should > 0.");
    }

    /**
     * reset the main server address and all other servers.
     *
     * @param mainServerAddress:can not be null otherwise throw
     *            {@link IllegalArgumentException}.
     * @param servers:could be null or empty.
     */
    public void setServers(RpcAddress mainServerAddress, Set<RpcAddress> servers) {
        Assert.notNull(mainServerAddress, "mainServerAddress can not be null.");
        serversWithAccessedTime.clear();
        this.mainServerAddress = mainServerAddress;
        addServers(servers);
    }

    public void addServers(String servers) {
        if (servers != null && !"".equals(servers)) {
            Set<RpcAddress> toServers = new HashSet<RpcAddress>();
            String[] addresses = servers.split(",");
            for (String address : addresses) {
                String[] ipAndPort = address.split(":");
                if (ipAndPort.length == 2) {
                    try {
                        RpcSocketAddress rpcAddress = new RpcSocketAddress(ipAndPort[0], Integer
                                .valueOf(ipAndPort[1]));
                        toServers.add(rpcAddress);
                    } catch (NumberFormatException e) {
                        logger.error("illegal address:" + address);
                    }
                } else {
                    logger.error("illegal address:" + address);
                }
            }
            addServers(toServers);
        }
    }

    public void addServers(Set<RpcAddress> servers) {
        if (servers != null) {
            for (RpcAddress server : servers) {
                serversWithAccessedTime.put(server, System.nanoTime());
            }
        }
    }

    public MapParameters getParameters() {
        return parameters;
    }

    public int getStartupRetryTimes() {
        return startupRetryTimes;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getAutoReconnectInMs() {
        return autoReconnectInMs;
    }

    public Set<RpcAddress> getServers() {
        Set<RpcAddress> servers = new HashSet<RpcAddress>();
        servers.addAll(serversWithAccessedTime.keySet());
        return servers;
    }

    /**
     * sort the targets addresses by the last access time in ASC order.
     *
     * <pre>
     * e.g.: RpcAddress1: lastAccessTime=yesterday
     *       RpcAddress2: lastAccessTime=this noon
     *       RpcAddress3: lastAccessTime=this morning
     * result will be:
     *       RpcAddress1,RpcAddress3,RpcAddress2
     * </pre>
     *
     * @return: the sorted RpcAddress array
     */
    public RpcAddress[] getOrderedServers() {
        RpcAddress[] targets = serversWithAccessedTime.keySet().toArray(new RpcAddress[0]);
        Arrays.sort(targets, new Comparator<RpcAddress>() {
            public int compare(RpcAddress addr1, RpcAddress addr2) {
                return serversWithAccessedTime.get(addr1) > serversWithAccessedTime.get(addr2) ? 1
                        : -1;
            }
        });
        return targets;
    }

    public RpcAddress getMainAddress() {
        return mainServerAddress;
    }

    public int getSecondsOfHb() {
        return secondsOfHb;
    }

    public void setConnectPolicy(int connectPolicy) {
        this.connectPolicy = connectPolicy;
    }

    public int getConnectPolicy() {
        return connectPolicy;
    }

    public void recordAccessedTime(RpcAddress serverAddress) {
        if (serverAddress != null && serversWithAccessedTime.containsKey(serverAddress)) {
            serversWithAccessedTime.put(serverAddress, System.nanoTime());
        }
    }

    /**
     * Indicate the given RpcClientIoHandler accept server address list from
     * remote server.
     *
     * @return
     */
    public boolean isAcceptServersThroughHandshake() {
        return acceptServersThroughHandshake;
    }

    public void setAcceptServersThroughHandshake(boolean acceptServersThroughHandshake) {
        this.acceptServersThroughHandshake = acceptServersThroughHandshake;
    }

    public void setWhiteIpList(Collection<String> ipList) {
        Assert.notNull(ipList, "ipList can not be null.");
        synchronized (whiteIpList) {
            whiteIpList.clear();
            whiteIpList.addAll(ipList);
        }
    }

    public boolean isIpInWhiteList(String ip) {
        Assert.hasText(ip, "ip can not be empty.");
        synchronized (whiteIpList) {
            return whiteIpList.contains(ip);
        }
    }
}
