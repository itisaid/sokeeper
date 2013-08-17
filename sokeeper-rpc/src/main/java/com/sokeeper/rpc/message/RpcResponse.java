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

import com.sokeeper.util.Assert;


/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 3693743114837586569L;

    private String            id;
    private Object            result;

    public RpcResponse(String id, Object result) {
        Assert.notNull(id, "id can not be null");
        this.id = id;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public Object getResult() {
        return result;
    }

}
