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
package com.sokeeper.persist.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sokeeper.domain.subject.KeywordEntity;
import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.domain.subject.SubjectKeyword;
import com.sokeeper.domain.subject.SubjectEntity.KeywordCount;
import com.sokeeper.exception.PersistLayerException;
import com.sokeeper.persist.service.SubjectKeywordService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
// @Component("subjectKeywordService")
public class SubjectKeywordServiceImpl extends BaseResourceService implements
		SubjectKeywordService {
	final protected Logger logger = LoggerFactory.getLogger(getClass());
	final protected static int BATCH_SIZE = 10000;

	@SuppressWarnings("unchecked")
	public List<SubjectEntity> search(String question, int pageNo, int pageSize)
			throws IllegalArgumentException, PersistLayerException {
		// STEP 1: check input parameters
		Assert.hasText(question, "question can not be empty");
		Assert.isTrue(pageNo >= 0, "pageNo cannot be negative number");
		Assert.isTrue(pageSize > 0, "pageSize should be positive number");

		// STEP 2: declare the variables
		List<SubjectEntity> subjects = new ArrayList<SubjectEntity>();
		Set<String> keywords = new HashSet<String>();
		Set<Integer> keywordIds = new HashSet<Integer>();

		// STEP 3: recognize terms
		List<Term> terms = ToAnalysis.parse(question);

		// STEP 4: calculate the hasCode for each term and query out the related
		// keywordId
		Set<Integer> hashCodes = new HashSet<Integer>();
		for (Term term : terms) {
			String keyword = term.getName();
			Integer hashCode = keyword.hashCode();
			hashCodes.add(hashCode);
			keywords.add(keyword);

		}
		if (!keywords.isEmpty()) {
			List<Map<String, Object>> keywordsList = (List<Map<String, Object>>) getSqlMapClientTemplate()
					.queryForList("subject.listKeywordsByHash",
							hashCodes.toArray(new Integer[0]));
			for (Map<String, Object> map : keywordsList) {
				if (keywords.contains(map.get("name"))) {
					keywordIds.add((Integer) map.get("id"));
				}
			}
		}

		// STEP 5: query all qualified subjectId by keywordId
		if (!keywordIds.isEmpty()) {
			final Map<Integer, Double> scoresOfSubject = searchScoredSubjectIds(keywordIds);

			// STEP 6: sort the subjects by their score
			Integer[] sortedSubjectIds = new Integer[scoresOfSubject.size()];
			scoresOfSubject.keySet().toArray(sortedSubjectIds);
			Arrays.sort(sortedSubjectIds, new Comparator<Integer>() {
				@Override
				public int compare(Integer subject1Id, Integer subject2Id) {
					double d = scoresOfSubject.get(subject2Id)
							- scoresOfSubject.get(subject1Id);
					if (d > 0) {
						return 1;
					}
					if (d < 0) {
						return -1;
					}
					return 0;
				}
			});

			// STEP 7: extracted wanted page result
			if (sortedSubjectIds.length < pageSize * pageNo) {
				pageNo = 0;
			}
			Integer endIdx = pageSize * (pageNo + 1);
			sortedSubjectIds = Arrays.copyOfRange(sortedSubjectIds, pageSize
					* pageNo,
					endIdx > sortedSubjectIds.length ? sortedSubjectIds.length
							: endIdx);

			// STEP 8: perform the query and sort the result by score
			subjects = (List<SubjectEntity>) getSqlMapClientTemplate()
					.queryForList("subject.listSubjectsByIds", sortedSubjectIds);
			Collections.sort(subjects, new Comparator<SubjectEntity>() {
				@Override
				public int compare(SubjectEntity subject1,
						SubjectEntity subject2) {
					double d = scoresOfSubject.get(subject2.getId().intValue())
							- scoresOfSubject.get(subject1.getId().intValue());
					if (d > 0) {
						return 1;
					}
					if (d < 0) {
						return -1;
					}
					return 0;
				}
			});
		}
		return subjects;
	}

	/**
	 * the score method can be optimized in future
	 * 
	 * @param keywordIds
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<Integer, Double> searchScoredSubjectIds(
			Set<Integer> keywordIds) {
		final Map<Integer, Double> scoresOfSubject = new HashMap<Integer, Double>();
		List<Map<String, Object>> subjectsList = (List<Map<String, Object>>) getSqlMapClientTemplate()
				.queryForList("subject.listSubjectsByKeywordIds",
						keywordIds.toArray(new Integer[0]));
		int numOfKeyword = keywordIds.size();
		for (Map<String, Object> map : subjectsList) {
			Integer subjectId = (Integer) map.get("subject_id");
			Integer keywordId = (Integer) map.get("keyword_id");
			Integer keywordOccur = (Integer) map.get("keyword_occur");
			if (subjectId != null && keywordOccur != null && keywordId != null) {
				Double score = scoresOfSubject.get(subjectId);
				if (score == null) {
					score = 0.0D;
				}
				score += (double) keywordOccur / (double) numOfKeyword;
				scoresOfSubject.put(subjectId, score);
			}
		}
		return scoresOfSubject;
	}

	@Transactional
	public void seed(String subjectFile, String keywordSubjectFile)
			throws IllegalArgumentException, PersistLayerException, IOException {
		long memo = Runtime.getRuntime().freeMemory();

		// STEP 1: seed subjectDataFile
		seedSubjectDataFile(subjectFile);

		// STEP 2: seed keywordSubjectDataFile
		seedKeywordSubjectFile(keywordSubjectFile);

		// STEP 3: now persist all those data into database
		long memoUsed = memo - Runtime.getRuntime().freeMemory();
		logger.info("seed spent memory:" + memoUsed + "M");
	}

	public void seedKeywordSubjectFile(String keywordSubjectFile)
			throws FileNotFoundException, IOException {
		Assert
				.hasText(keywordSubjectFile,
						"keywordSubjectFile cannot be empty");

		BufferedReader reader = null;
		// STEP 1: parse the keyword subject map
		try {
			reader = ServiceUtil.getReader(keywordSubjectFile);
			String strLine = null;
			List<SubjectKeyword> batchedSubjectKeyword = new ArrayList<SubjectKeyword>();
			// STEP 2: read the subject's external id -> internal id map
			Map<Long, Long> subExtToIdMap = getSubjectIdMap();
			BlackWords bw = BlackWords.getInstance();
			do {
				strLine = reader.readLine();
				if (strLine != null) {
					int sep = strLine.indexOf(':');
					if (sep > 0) {
						// STEP 3: get the keyword
						String keyword = strLine.substring(0, sep);
						if (bw.isBlackWord(keyword)) {
							continue;
						}
						// STEP 4: create keyword
						KeywordEntity keywordEntity = new KeywordEntity();
						keywordEntity.setName(keyword);
						Long keywordId = keywordFound(keywordEntity);
						keywordEntity.setId(keywordId);

						strLine = strLine.substring(sep + 1);
						// STEP 5: get the subject/occur pair isolated by ;
						String[] subjectPairs = strLine.split(";");
						for (String subExtIdOccurPair : subjectPairs) {
							if (!subExtIdOccurPair.isEmpty()) {
								sep = subExtIdOccurPair.indexOf(",");
								if (sep > 0) {
									Long subExtId = null;
									try {
										subExtId = Long
												.valueOf(subExtIdOccurPair
														.substring(0, sep));
									} catch (NumberFormatException e) {
										logger
												.warn("illegal subjectId found from keywordSubjectLine:"
														+ strLine);
										continue;
									}
									Long subId = subExtToIdMap.get(subExtId);
									if (subId != null) {
										Long occur = null;
										try {
											occur = Long
													.valueOf(subExtIdOccurPair
															.substring(sep + 1));
										} catch (NumberFormatException e) {
											logger
													.warn("illegal occurance found from keywordSubjectLine:"
															+ strLine);
											continue;
										}

										// STEP 6: create SubjectKeyword
										SubjectKeyword subjectKeyword = new SubjectKeyword();
										subjectKeyword
												.setKeywordId(keywordEntity
														.getId());
										subjectKeyword.setSubjectId(subId);
										subjectKeyword.setKeywordOccur(occur);
										batchedSubjectKeyword
												.add(subjectKeyword);
										// STEP 7: batch create
										if (batchedSubjectKeyword.size() >= BATCH_SIZE) {
											subjectKeywordFound(batchedSubjectKeyword);
											batchedSubjectKeyword = new ArrayList<SubjectKeyword>();
										}
									}
								}
							}
						}
					}
				}
			} while (strLine != null);

			// STEP 8: update the remained
			if (!batchedSubjectKeyword.isEmpty()) {
				subjectKeywordFound(batchedSubjectKeyword);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	public void seedSubjectDataFile(String subjectFile)
			throws FileNotFoundException, IOException {
		Assert.hasText(subjectFile, "subjectFile cannot be empty");

		BufferedReader reader = null;
		List<SubjectEntity> batchedEntities = new ArrayList<SubjectEntity>();
		try {
			reader = ServiceUtil.getReader(subjectFile);
			String strLine = null;
			List<String> subjectBlock = new ArrayList<String>();
			do {
				strLine = reader.readLine();
				if (strLine != null) {
					if (strLine.startsWith("=*******=")) {
						SubjectEntity subjectEntity = buildSubjectEntity(subjectBlock);
						batchedEntities.add(subjectEntity);
						if (batchedEntities.size() >= BATCH_SIZE) {
							subjectFound(batchedEntities);
							batchedEntities = new ArrayList<SubjectEntity>();
						}
						subjectBlock.clear();
					} else {
						subjectBlock.add(strLine);
					}
				}
			} while (strLine != null);

			if (!batchedEntities.isEmpty()) {
				subjectFound(batchedEntities);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	private SubjectEntity buildSubjectEntity(List<String> subjectBlock) {
		SubjectEntity subjectEntity = new SubjectEntity();
		int strBlockNum = 0;
		StringBuffer info = new StringBuffer();
		String score = null;
		String summary = null;
		for (String line : subjectBlock) {
			if (line.startsWith("-----")) {
				strBlockNum++;
				continue;
			}
			// name
			if (strBlockNum == 0) {
				int sep = line.indexOf(':');
				if (sep > 0) {
					String sdx = line.substring(0, sep).trim();
					String sub = line.substring(sep + 1).trim();
					if (!sdx.isEmpty() && !sub.isEmpty()) {
						subjectEntity.setName(sub);
						try {
							subjectEntity.setExternalId(Long.valueOf(sdx));
						} catch (NumberFormatException e) {
							logger.warn("illegal id found from subjectLine:"
									+ line);
							continue;
						}
					}
				}
			}
			// keyword
			if (strBlockNum == 1 && !line.equals("")) {
				subjectEntity.makeKeywordCountList(line);
			}
			// info
			if (strBlockNum == 2) {
				if (!line.startsWith("IMDb")) {
					info.append(getMainStr(line));
				}
			}
			// score
			if (strBlockNum == 3) {
				if (score == null) {
					score = line;
				}
			}
			// summary
			if (strBlockNum == 4) {
				if (line.indexOf("的剧情简介") == -1 && summary == null) {
					summary = line;
				}
			}

		}
		subjectEntity.setInfo(info.toString());
		try {
			if (score != null) {
				subjectEntity.setScore(Float.valueOf(score));
			}
		} catch (NumberFormatException e) {
			logger.warn("illegal score found from subjectLine:" + score);
		}
		subjectEntity.setSummary(summary);
		return subjectEntity;
	}

	private String getMainStr(String str) {
		String[] split = str.split(":");
		if (split.length > 1) {
			String info = split[1];
			String[] items = info.split(" / ");
			if (items.length > 0) {
				return items[0];
			}
		}
		return "";
	}

	protected Map<Long, Long> getSubjectIdMap() {
		Map<Long, Long> subExtToIdMap = new HashMap<Long, Long>();
		@SuppressWarnings("unchecked")
		List<Map<String, Integer>> list = (List<Map<String, Integer>>) getSqlMapClientTemplate()
				.queryForList("subject.listSubjectExternalId");
		for (Map<String, Integer> map : list) {
			subExtToIdMap.put(map.get("external_id").longValue(), map.get("id")
					.longValue());
		}
		list.clear();
		return subExtToIdMap;
	}

	protected void subjectFound(List<SubjectEntity> batchedEntities) {
		batchUpdateEntities(batchedEntities.toArray(new SubjectEntity[0]),
				"subject.addOrUpdateSubject", batchedEntities.size());
	}

	protected Long keywordFound(KeywordEntity keywordEntity) {
		return (Long) getSqlMapClientTemplate().insert(
				"subject.addOrUpdateKeyword", keywordEntity);
	}

	protected void subjectKeywordFound(
			List<SubjectKeyword> batchedSubjectKeyword) {
		batchUpdateEntities(batchedSubjectKeyword
				.toArray(new SubjectKeyword[0]),
				"subject.addOrUpdateSubjectKeyword", batchedSubjectKeyword
						.size());
	}
}
