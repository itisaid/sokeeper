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

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class SubjectKeyword extends DomainEntity {  
	private static final long serialVersionUID = 228142828306631920L;
	
	private Long subjectId;
	private Long keywordId;
	private Long keywordOccur;
	
	public Long getSubjectId() {
		return subjectId;
	}
	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}
	public Long getKeywordId() {
		return keywordId;
	}
	public void setKeywordId(Long keywordId) {
		this.keywordId = keywordId;
	}
	public Long getKeywordOccur() {
		return keywordOccur;
	}
	public void setKeywordOccur(Long keywordOccur) {
		this.keywordOccur = keywordOccur;
	}
	
}