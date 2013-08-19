package com.hbd.cmdb.search.sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbd.cmdb.search.CmdbEntry;

public class WeightSearchSort implements SearchSort {
	private static WeightSearchSort instance = new WeightSearchSort();

	private WeightSearchSort() {
	}

	public static WeightSearchSort getInstance() {
		return instance;
	}

	public Map<String, Double> sort(
			List<List<CmdbEntry<String, Integer>>> subjectList) {
		Map<String, Double> subjectMap = new HashMap<String, Double>();
		for (List<CmdbEntry<String, Integer>> keyList : subjectList) {
			for (CmdbEntry<String, Integer> e : keyList) {
				String subject = e.getKey();
				double value = e.getValue();
				double weighValue = Math.pow(value, 2)
						/ subjectKeyMap.get(subject);
				Double count = subjectMap.get(subject);
				if (count == null) {
					subjectMap.put(subject, weighValue);
				} else {
					subjectMap.put(subject, count + weighValue);
				}
			}
		}
		return subjectMap;
	}

}
