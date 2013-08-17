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

import com.sokeeper.domain.resource.ResourceKey;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class AssociationChangesEvent extends ChangesEvent {
    private static final long serialVersionUID = 2373297108329804178L;
    private ResourceKey       leftKey          = new ResourceKey();
    private ResourceKey       rightKey         = new ResourceKey();

    public String getLeftType() {
        return leftKey.getResourceType();
    }

    public void setLeftType(String leftType) {
        leftKey.setResourceType(leftType);
    }

    public String getLeftId() {
        return leftKey.getResourceName();
    }

    public void setLeftId(String leftId) {
        leftKey.setResourceName(leftId);
    }

    public String getRightType() {
        return rightKey.getResourceType();
    }

    public void setRightType(String rightType) {
        rightKey.setResourceType(rightType);
    }

    public String getRightId() {
        return rightKey.getResourceName();
    }

    public void setRightId(String rightId) {
        rightKey.setResourceName(rightId);
    }

    public ResourceKey getLeftKey() {
        return leftKey;
    }

    public ResourceKey getRightKey() {
        return rightKey;
    }

}
