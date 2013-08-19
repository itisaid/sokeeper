package com.hbd.cmdb.search.sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbd.cmdb.search.CmdbEntry;

public class SimpleSearchSort implements SearchSort {
	private static SimpleSearchSort instance = new SimpleSearchSort();

	private SimpleSearchSort() {
	}

	public static SimpleSearchSort getInstance() {
		return instance;
	}

	public Map<String, Double> sort(
			List<List<CmdbEntry<String, Integer>>> subjectList) {
		Map<String, Double> subjectMap = new HashMap<String, Double>();
		for (List<CmdbEntry<String, Integer>> keyList : subjectList) {
			for (CmdbEntry<String, Integer> e : keyList) {
				String subject = e.getKey();
				double value = e.getValue();
				// System.out.println(subjectSummaryMap.get(subject)+"   "+value);
				Double count = subjectMap.get(subject);
				if (count == null) {
					subjectMap.put(subject, value);
				} else {
					subjectMap.put(subject, count + value);
				}
			}
		}
		return subjectMap;
	}

}
