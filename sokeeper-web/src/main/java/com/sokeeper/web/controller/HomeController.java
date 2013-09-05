/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.sokeeper.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sokeeper.domain.subject.SubjectEntity;
import com.sokeeper.domain.subject.SubjectEntity.KeywordCount;
import com.sokeeper.persist.service.SubjectKeywordService;
import com.sokeeper.web.dto.MovieDto;
import com.sokeeper.web.dto.QueryDto;


/**
 * @author James Fu (fuyinhai@gmail.com)
 */
@Controller
public class HomeController {
	
    @Autowired
    private SubjectKeywordService subjectKeywordService;
    
    @RequestMapping
    public ModelAndView index( QueryDto query , Map<String, Object> out) {
        Assert.notNull(subjectKeywordService, "subjectKeywordService can not be null.");
        out.put("query", query);
        
        List<MovieDto> movies = new ArrayList<MovieDto>();
        if (query.getKeywords() != null && !query.getKeywords().isEmpty()) {
            List<SubjectEntity> subjects = subjectKeywordService.search(query.getKeywords() == null ? "" : query.getKeywords(), 0, 40); 
            for (int i=0; i<subjects.size(); i++) {
            	SubjectEntity entity = subjects.get(i);
            	MovieDto movie = new MovieDto();
            	movie.setName( entity.getName());
            	movie.setInfo(entity.getInfo());
            	movie.setImageUrl("/images/poster/"+entity.getExternalId()+".jpg");
            	movie.setKeywordCountList(entity.getKeywordCountList());
            	movie.setScore(entity.getScore());
            	movie.setSummary(entity.getSummary());
            	movie.setSubjectId(entity.getExternalId());
            	movies.add(movie);
            }
        }
        out.put("movies",movies);
        
        return new ModelAndView("home/index");
    } 
}
