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
package com.sokeeper.web.dto;

import java.io.Serializable;
import java.util.List;

import com.sokeeper.domain.subject.SubjectEntity.KeywordCount;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class MovieDto implements Serializable {

	private static final long serialVersionUID = 2462062713366903759L;

	private String imageUrl;
	private String name;
	private String summary;
	private String info;
	private float score;
	private List<KeywordCount> keywordCountList;
	private long subjectId;

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public List<KeywordCount> getKeywordCountList() {
		return keywordCountList;
	}

	public void setKeywordCountList(List<KeywordCount> keywordCountList) {
		this.keywordCountList = keywordCountList;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(long subjectId) {
		this.subjectId = subjectId;
	}

}
