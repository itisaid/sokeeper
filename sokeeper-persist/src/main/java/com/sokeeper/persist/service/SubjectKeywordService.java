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
package com.sokeeper.persist.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.exception.PersistLayerException;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public interface SubjectKeywordService {
 
	/**
	 * Search given page Subjects
	 * @param question: the user input query clause includes the keywords
	 * @param pageNo: the pageNo which user want see, start from 0
	 * @param pageSize: how many records each page want show
	 * @return the qualified result which sorted
	 * @throws IllegalArgumentException
	 * @throws PersistLayerException
	 */
	public List<SubjectEntity> search(String question, int pageNo , int pageSize)  throws IllegalArgumentException, PersistLayerException;
	
	/**
	 * seed the Subject,Keyword and SubjectKeyword data from seed data files
	 * @param subjectFile: the subjectFile format in
	 * <pre>
	 * subjectId:subjectName
	 * subjectId:subjectName
	 * </pre>
	 * @param keywordSubjectFile: the subject keyword file in format
	 * <pre>
	 * keyword:subject,apperanceTimes;subject2,apperanceTimes2;
	 * keyword:subject,apperanceTimes;subject2,apperanceTimes2;
	 * </pre>
	 * @throws IllegalArgumentException
	 * @throws PersistLayerException
	 * @throws FileNotFoundException when given subject file or subject keyword file not found
	 */
	public String seed(String subjectFile,String keywordSubjectFile) throws IllegalArgumentException, PersistLayerException, FileNotFoundException, IOException;
}
