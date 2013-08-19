package com.hbd.cmdb.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CmSearchUtil {
	static Map<String, String> subjectSummaryMap = Index.getInstance()
			.getSubjectSummaryMap();
	static int maxResult = 40;

	public static List<String> result(List<Entry<String, Double>> list) {
		List<String> res = new ArrayList<String>();
		int num = 0;
		for (Entry<String, Double> e : list) {
			if (num++ > maxResult) {
				break;
			}
			String key = e.getKey();
			res.add(subjectSummaryMap.get(key));
		}
		return res;
	}
}
