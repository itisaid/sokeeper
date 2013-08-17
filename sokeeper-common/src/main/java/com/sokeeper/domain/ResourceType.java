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

import java.io.Serializable;

import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ResourceType extends DomainEntity implements Serializable {

    private static final long serialVersionUID = 5448420986334273954L;

    private String            typeName;
    private Boolean           onlineResource   = false;
    private Boolean           keepHistoric     = false;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Boolean getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(Boolean onlineResource) {
        Assert.notNull(onlineResource, "onlineResource can not be null.");
        this.onlineResource = onlineResource;
    }

    public Boolean getKeepHistoric() {
        return keepHistoric;
    }

    public void setKeepHistoric(Boolean keepHistoric) {
        Assert.notNull(keepHistoric, "keepHistoric can not be null.");
        this.keepHistoric = keepHistoric;
    }

}
