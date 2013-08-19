package com.hbd.cmdb.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.domain.Term;

import com.hbd.cmdb.index.IndexInfo;
import com.hbd.cmdb.search.sort.SearchSort;
import com.hbd.cmdb.search.sort.WeightSearchSort;

public class Searcher {
	private static Searcher instance = new Searcher();
	private BlackWords blackWords = BlackWords.getInstance();
	Map<String, List<CmdbEntry<String, Integer>>> indexMap = Index
			.getInstance().getIndexMap();
	SearchSort sorter = WeightSearchSort.getInstance();

	private Searcher() {
		init();
	}

	public static Searcher getInstance() {
		return instance;
	}

	public void init() {
		Index.getInstance();
		SearchUtil.splitWords("hello, 钱");// init ansj by firstly be invoked.
	}

	public List<String> search(String keyWords) {
		List<Term> terms = SearchUtil.splitWords(keyWords);

		List<List<CmdbEntry<String, Integer>>> subjectList = new ArrayList<List<CmdbEntry<String, Integer>>>();
		for (Term term : terms) {
			String key = term.getName();
			// System.out.println("search key:" + key);
			List<CmdbEntry<String, Integer>> keyList = indexMap.get(key);
			if (keyList != null) {
				subjectList.add(keyList);
			}
		}
		Map<String, Double> subjectMap = sorter.sort(subjectList);
		List<Entry<String, Double>> list = IndexInfo.sortMapDouble(subjectMap);

		return CmSearchUtil.result(list);
	}

}
