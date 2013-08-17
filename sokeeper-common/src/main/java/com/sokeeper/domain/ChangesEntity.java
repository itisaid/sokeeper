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

import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class ChangesEntity extends AddressedEntity {
    public static final String CHANGES_CREATED      = "CREATED";
    public static final String CHANGES_UPDATED      = "UPDATED";
    public static final String CHANGES_DELETED      = "DELETED";
    public static final String OWNER_DIED           = "DIED";
    private static final long  serialVersionUID     = 8481215837983841294L;

    private Long               sequence;
    private String             changes;

    private Boolean            acceptOwnerDiedEvent = false;

    public Boolean getAcceptOwnerDiedEvent() {
        return acceptOwnerDiedEvent;
    }

    public void setAcceptOwnerDiedEvent(Boolean acceptOwnerDiedEvent) {
        Assert.notNull(acceptOwnerDiedEvent, "acceptOwnerDiedEvent can not be null.");
        this.acceptOwnerDiedEvent = acceptOwnerDiedEvent;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

}
