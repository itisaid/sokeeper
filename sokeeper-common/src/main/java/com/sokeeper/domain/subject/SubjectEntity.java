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
package com.sokeeper.domain.subject;

import com.sokeeper.domain.DomainEntity;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class SubjectEntity extends DomainEntity {
	private static final long serialVersionUID = -6587566059233849314L;
	
	private final static int MAX_NAME_LENGTH=255;
	private String name;
	private Long externalId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Assert.hasText(name, "name can not be empty");
		this.name = name.substring(0,name.length() > MAX_NAME_LENGTH ? MAX_NAME_LENGTH : name.length() );
	}

	public Integer getHash() {
		return name.hashCode();
	}

	public void setHash(Integer hash) {
	}

	public Long getExternalId() {
		return externalId;
	}

	public void setExternalId(Long externalId) {
		this.externalId = externalId;
	}
 
	
}