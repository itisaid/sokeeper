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
package com.sokeeper.rpc.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sokeeper.util.RpcAddress;


/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcLocalExceptionMultipleTargets extends RpcLocalException {
    private static final long       serialVersionUID = -77408268128024660L;

    private Map<RpcAddress, String> failedTargets    = new HashMap<RpcAddress, String>();
    private String                  serviceName      = null;

    public void addFailedTarget(RpcAddress target, String message) {
        if (target != null) {
            failedTargets.put(target, message);
        }
    }

    public RpcAddress[] getFailedTargets() {
        return failedTargets.keySet().toArray(new RpcAddress[0]);
    }

    public String getFailedMessage(RpcAddress target) {
        if (target != null) {
            return failedTargets.get(target);
        }
        return null;
    }

    public boolean hasFailedTargets() {
        return failedTargets.size() > 0;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMessage() {
        StringBuffer strBuffer = new StringBuffer(super.getMessage());
        for (Entry<RpcAddress, String> pair : failedTargets.entrySet()) {
            strBuffer.append("\n--->" + pair.getKey() + " " + pair.getValue());
        }
        return strBuffer.toString();
    }

    public RpcLocalExceptionMultipleTargets(String serviceName) {
        super(serviceName);
        this.serviceName = serviceName;
    }

}
