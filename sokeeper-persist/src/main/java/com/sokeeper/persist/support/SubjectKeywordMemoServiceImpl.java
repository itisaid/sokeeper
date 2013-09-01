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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sokeeper.domain.subject.KeywordEntity;
import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.domain.subject.SubjectKeyword;
import com.sokeeper.exception.PersistLayerException;
import com.sokeeper.persist.service.SubjectKeywordService;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Component("subjectKeywordService")
public class SubjectKeywordMemoServiceImpl extends SubjectKeywordServiceImpl implements SubjectKeywordService {
	
	private List<String>  keywordsList = new ArrayList<String>();
	private List<SubjectEntity> subjectsList = new ArrayList<SubjectEntity>();
	private Map<Long,Long> subExtToIdMap = new HashMap<Long,Long>();   
	private List<List<SubjectKeyword>> keywordSubjectList = new ArrayList<List<SubjectKeyword>>();
	
	public SubjectKeywordMemoServiceImpl() throws IllegalArgumentException, PersistLayerException, IOException {
	    seed("subject.dat", "keysubject.dat");
	    search("情节感人",0,40);
	}
	
	public List<SubjectEntity> search(String question, int pageNo , int pageSize)  throws IllegalArgumentException, PersistLayerException {	
		// STEP 1: check input parameters
		Assert.hasText(question,"question can not be empty");
		Assert.isTrue(pageNo >= 0 , "pageNo cannot be negative number");
		Assert.isTrue(pageSize > 0 , "pageSize should be positive number");
		
		// STEP 2: declare the variables
		List<SubjectEntity> subjects = new ArrayList<SubjectEntity>();
	    Set<Integer> keywordIds= new HashSet<Integer>();
	    
		// STEP 3: recognize terms
		List<Term> terms = ToAnalysis.parse(question);
		
		for (Term term: terms) {
			String keyword = term.getName();
			Integer idOfKeyword = keywordsList.indexOf(keyword);
			if (idOfKeyword >= 0) {
				keywordIds.add(idOfKeyword);
			}
		}
		
		// STEP 4: query all qualified subjectId by keywordId
		if (!keywordIds.isEmpty()) {
			final Map<Integer,Double> scoresOfSubject = searchScoredSubjectIds(keywordIds);
			
		    // STEP 5: sort the subjects by their score 
		    Integer[] sortedSubjectIds = new Integer[scoresOfSubject.size()];
		    scoresOfSubject.keySet().toArray(sortedSubjectIds);
		    Arrays.sort(sortedSubjectIds, new Comparator<Integer>(){
				@Override
				public int compare(Integer subject1Id, Integer subject2Id) {
					double d = scoresOfSubject.get(subject2Id) - scoresOfSubject.get(subject1Id);
					if (d > 0) {
						return 1 ;
					}
					if (d < 0) {
						return -1;
					}
					return 0;
				} 
		    });
		    
		    // STEP 6: extracted wanted page result
		    if (sortedSubjectIds.length < pageSize * pageNo) {
		    	pageNo = 0 ;
		    }
		    Integer endIdx = pageSize * ( pageNo + 1 ) ;
		    sortedSubjectIds = Arrays.copyOfRange(sortedSubjectIds, pageSize * pageNo ,  endIdx > sortedSubjectIds.length ? sortedSubjectIds.length : endIdx  );
		    
		    // STEP 7: perform the query and sort the result by score
		    for (Integer subjectId : sortedSubjectIds) {
		    	subjects.add(subjectsList.get(subjectId));
		    } 
		}		
	    return subjects ;	
	}

	/**
	 * the score method can be optimized in future
	 * @param keywordIds
	 * @return
	 */
	protected Map<Integer,Double> searchScoredSubjectIds(Set<Integer> keywordIds) {
		final Map<Integer,Double> scoresOfSubject = new HashMap<Integer,Double>();
		int numOfKeyword = keywordIds.size();
		for (Integer idOfKeyword : keywordIds) {
			if ( idOfKeyword >= keywordSubjectList.size()) {
				continue;
			}
			for (SubjectKeyword sk : keywordSubjectList.get(idOfKeyword)){
				Double score = scoresOfSubject.get(sk.getSubjectId());
				if (score == null) {
					score = 0.0D ;
				}
				score += (double)sk.getKeywordOccur() / (double)numOfKeyword;
				scoresOfSubject.put(sk.getSubjectId().intValue(), score);
			}
		}
		return scoresOfSubject;
	}
	
	protected Map<Long,Long> getSubjectIdMap() { 
		return subExtToIdMap;
	}
	
	protected void subjectFound(List<SubjectEntity> batchedEntities) {
		for (SubjectEntity subject : batchedEntities) {
			Long subjectId = new Long(subjectsList.size());
			subExtToIdMap.put(subject.getExternalId(), subjectId );
			subject.setId(subjectId);
			subjectsList.add(subject);
		}
	}

	protected Long keywordFound(KeywordEntity keywordEntity) { 
		Long idOfKeyword = new Long(keywordsList.size());
		keywordsList.add(keywordEntity.getName());
		return idOfKeyword;
	}
	
	protected void subjectKeywordFound(List<SubjectKeyword> entities) {
		for (SubjectKeyword sk : entities) {
			Long keywordIdx = sk.getKeywordId();
			for( int idx = keywordSubjectList.size(); idx <= keywordIdx; idx ++ ){
				keywordSubjectList.add(new ArrayList<SubjectKeyword>());
			}  
			keywordSubjectList.get(keywordIdx.intValue()).add(sk);
		}
	}
}
