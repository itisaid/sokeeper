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
package com.sokeeper.service.support;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sokeeper.domain.subject.KeywordEntity;
import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.domain.subject.SubjectKeyword;
import com.sokeeper.service.SubjectKeywordService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class SubjectKeywordServiceImpl implements SubjectKeywordService {

	final protected Logger logger = LoggerFactory.getLogger(getClass());
	final protected static int BATCH_SIZE = 10000;

	private List<String> keywordsList = new ArrayList<String>();
	private Map<Integer, Integer[]> keywordsHashCode2IdxMap = new HashMap<Integer, Integer[]>();
	private List<SubjectEntity> subjectsList = new ArrayList<SubjectEntity>();
	private Map<Long, Long> subExtToIdMap = new HashMap<Long, Long>();
	private List<List<SubjectKeyword>> keywordSubjectList = new ArrayList<List<SubjectKeyword>>();
	private Set<String> blackWords = new HashSet<String>();

	private SubjectKeywordServiceImpl() throws IllegalArgumentException,
			IOException {
		seed(ResourceHelper.DATA_FILE_SUBJECT,
				ResourceHelper.DATA_FILE_KEYWORD_SUBJECT,
				ResourceHelper.DATA_FILE_BLACK_WORDS);
	}

	private static SubjectKeywordServiceImpl instance;

	public synchronized static SubjectKeywordServiceImpl getInstance() {
		if(instance == null){
			try {
				instance = new SubjectKeywordServiceImpl();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return instance;
	}

	public String seed(String subjectFile, String keywordSubjectFile,
			String blackWordsFile) throws IllegalArgumentException, IOException {
		// STEP 1: clear cache
		keywordsList.clear();
		keywordsHashCode2IdxMap.clear();
		subjectsList.clear();
		subExtToIdMap.clear();
		keywordSubjectList.clear();

		// STEP 2: gc
		System.gc();

		// STEP 3: remember memory and time
		long memo = Runtime.getRuntime().totalMemory();
		long time = System.currentTimeMillis();

		// STEP 4: seed subjectDataFile
		seedSubjectDataFile(subjectFile);

		// STEP 5: seed keywordSubjectDataFile
		seedKeywordSubjectFile(keywordSubjectFile);

		// STEP 6: seed blackWords
		seedBlackWordsFile(blackWordsFile);

		// STEP 7: warm up
		search("情节感人", 0, 40);
		search("暴力", 0, 40);
		search("优美", 0, 40);
		search("可爱", 0, 40);

		// STEP 8: calculate the memory usage
		long memoUsed = (Runtime.getRuntime().totalMemory() - memo)
				/ (1024 * 1024);
		logger.info("seed spent memory:" + memoUsed + "M");

		String msg = "seed spent memory:" + memoUsed + "M in"
				+ (System.currentTimeMillis() - time) + "ms";
		msg += " keywordsList:" + keywordsList.size();
		msg += " subjectsList:" + subjectsList.size();
		return msg;
	}

	public void seedSubjectDataFile(String subjectFile)
			throws FileNotFoundException, IOException {
		Assert.hasText(subjectFile, "subjectFile cannot be empty");

		BufferedReader reader = null;
		List<SubjectEntity> batchedEntities = new ArrayList<SubjectEntity>();
		try {
			reader = ResourceHelper.getInstance().getReader(subjectFile);
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

	public void seedKeywordSubjectFile(String keywordSubjectFile)
			throws FileNotFoundException, IOException {
		Assert.hasText(keywordSubjectFile, "keywordSubjectFile cannot be empty");

		BufferedReader reader = null;
		// STEP 1: parse the keyword subject map
		try {
			reader = ResourceHelper.getInstance().getReader(keywordSubjectFile);
			String strLine = null;
			List<SubjectKeyword> batchedSubjectKeyword = new ArrayList<SubjectKeyword>();
			// STEP 2: read the subject's external id -> internal id map
			do {
				strLine = reader.readLine();
				if (strLine != null) {
					int sep = strLine.indexOf(':');
					if (sep > 0) {
						// STEP 3: get the keyword
						String keyword = strLine.substring(0, sep);
						if (blackWords.contains(keyword)) {
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
										logger.warn("illegal subjectId found from keywordSubjectLine:"
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
											logger.warn("illegal occurance found from keywordSubjectLine:"
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

	public void seedBlackWordsFile(String blackWordsFile) throws IOException {
		blackWords.add(" ");
		BufferedReader br = ResourceHelper.getInstance().getReader(
				blackWordsFile);
		String line;
		while ((line = br.readLine()) != null) {
			blackWords.add(line.split(" ")[0]);
		}
	}

	protected void subjectFound(List<SubjectEntity> batchedEntities) {
		for (SubjectEntity subject : batchedEntities) {
			Long subjectId = new Long(subjectsList.size());
			subExtToIdMap.put(subject.getExternalId(), subjectId);
			subject.setId(subjectId);
			subjectsList.add(subject);
		}
	}

	protected Long keywordFound(KeywordEntity keywordEntity) {
		Long idOfKeyword = new Long(keywordsList.size());
		keywordsList.add(keywordEntity.getName());

		Integer hashCode = keywordEntity.getName().hashCode();
		Integer[] ids = keywordsHashCode2IdxMap.get(hashCode);
		if (ids == null) {
			ids = new Integer[1];
		} else {
			Integer[] newIds = new Integer[ids.length + 1];
			System.arraycopy(ids, 0, newIds, 0, ids.length);
			ids = newIds;
		}
		ids[ids.length - 1] = idOfKeyword.intValue();
		keywordsHashCode2IdxMap.put(hashCode, ids);

		return idOfKeyword;
	}

	protected void subjectKeywordFound(List<SubjectKeyword> entities) {
		for (SubjectKeyword sk : entities) {
			Long keywordIdx = sk.getKeywordId();
			for (int idx = keywordSubjectList.size(); idx <= keywordIdx; idx++) {
				keywordSubjectList.add(new ArrayList<SubjectKeyword>());
			}
			keywordSubjectList.get(keywordIdx.intValue()).add(sk);
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

	public List<SubjectEntity> search(String question, int pageNo, int pageSize)
			throws IllegalArgumentException {
		long time = System.nanoTime();
		// STEP 1: check input parameters
		Assert.hasText(question, "question can not be empty");
		Assert.isTrue(pageNo >= 0, "pageNo cannot be negative number");
		Assert.isTrue(pageSize > 0, "pageSize should be positive number");

		// STEP 2: declare the variables
		List<SubjectEntity> subjects = new ArrayList<SubjectEntity>();

		// STEP 3: recognize terms
		Set<Integer> keywordIds = extractKeywordIds(question);

		// STEP 4: query all qualified subjectId by keywordId
		if (!keywordIds.isEmpty()) {
			final Map<Integer, Double> scoresOfSubject = searchScoredSubjectIds(keywordIds);

			// STEP 5: sort the subjects by their score
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

			// STEP 6: extracted wanted page result
			if (sortedSubjectIds.length < pageSize * pageNo) {
				pageNo = 0;
			}
			Integer endIdx = pageSize * (pageNo + 1);
			sortedSubjectIds = Arrays.copyOfRange(sortedSubjectIds, pageSize
					* pageNo,
					endIdx > sortedSubjectIds.length ? sortedSubjectIds.length
							: endIdx);

			// STEP 7: perform the query and sort the result by score
			for (Integer subjectId : sortedSubjectIds) {
				subjects.add(subjectsList.get(subjectId));
			}
		}

		if (logger.isInfoEnabled()) {
			time = System.nanoTime() - time;
			logger.info("search:" + question + " spent:" + (time / 1000000)
					+ "ms");
		}

		return subjects;
	}

	private Set<Integer> extractKeywordIds(String question) {
		Set<Integer> keywordIds = new HashSet<Integer>();
		List<Term> terms = ToAnalysis.parse(question);

		for (Term term : terms) {
			String keyword = term.getName();
			Integer[] ids = keywordsHashCode2IdxMap.get(keyword.hashCode());
			if (ids != null) {
				for (int idx : ids) {
					if (keyword.equals(keywordsList.get(idx))) {
						keywordIds.add(idx);
					}
				}
			}
		}
		return keywordIds;
	}

	/**
	 * the score method can be optimized in future
	 * 
	 * @param keywordIds
	 * @return
	 */
	protected Map<Integer, Double> searchScoredSubjectIds(
			Set<Integer> keywordIds) {
		final Map<Integer, Double> scoresOfSubject = new HashMap<Integer, Double>();
		for (Integer idOfKeyword : keywordIds) {
			if (idOfKeyword >= keywordSubjectList.size()) {
				continue;
			}
			for (SubjectKeyword sk : keywordSubjectList.get(idOfKeyword)) {
				Double score = scoresOfSubject.get(sk.getSubjectId());
				if (score == null) {
					score = 0.0D;
				}
				SubjectEntity entity = subjectsList.get(sk.getSubjectId()
						.intValue());
				score += entity.calcTfIdfRate(sk.getKeywordOccur());
				scoresOfSubject.put(sk.getSubjectId().intValue(), score);
			}
		}
		return scoresOfSubject;
	}
}
