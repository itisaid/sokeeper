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

import java.util.ArrayList;
import java.util.List;

import com.sokeeper.domain.DomainEntity;
import com.sokeeper.util.Assert;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class SubjectEntity extends DomainEntity {
	private static final long serialVersionUID = -6587566059233849314L;

	private final static int MAX_NAME_LENGTH = 50;
	private final static int MAX_SUMMARY_LENGTH = 70;
	private final static int MAX_INFO_LENGTH = 90;
	private String name;
	private Long externalId;
	private String info;
	private float score = 0;
	private String summary;
	private List<KeywordCount> keywordCountList = new ArrayList<KeywordCount>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Assert.hasText(name, "name can not be empty");
		this.name = name.substring(0,
				name.length() > MAX_NAME_LENGTH ? MAX_NAME_LENGTH : name
						.length());
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

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info.substring(0,
				info.length() > MAX_INFO_LENGTH ? MAX_INFO_LENGTH : info
						.length());
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		if (summary == null) {
			return;
		}
		this.summary = summary.substring(0,
				summary.length() > MAX_SUMMARY_LENGTH ? MAX_SUMMARY_LENGTH
						: summary.length());
	}

	public List<KeywordCount> getKeywordCountList() {
		return keywordCountList;
	}

	public void makeKeywordCountList(String str) {
		String[] wordCounts = str.split(";");
		if (wordCounts != null && wordCounts.length > 0) {
			for (String wordCount : wordCounts) {
				String[] wc = wordCount.split(",");
				if (wc != null && wc.length > 1) {
					KeywordCount keywordCount = new KeywordCount();
					keywordCount.setKey(wc[0]);
					try {
						keywordCount.setCount(Integer.valueOf(wc[1]));
					} catch (NumberFormatException e) {
					}
					keywordCountList.add(keywordCount);
				}
			}
		}
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public class KeywordCount {
		String key;
		int count = 0;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

	}
}