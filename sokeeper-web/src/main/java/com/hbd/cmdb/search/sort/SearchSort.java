package com.hbd.cmdb.search.sort;

import java.util.List;
import java.util.Map;

import com.hbd.cmdb.search.CmdbEntry;
import com.hbd.cmdb.search.Index;

public interface SearchSort {
	Map<String, List<CmdbEntry<String, Integer>>> indexMap = Index
			.getInstance().getIndexMap();
	Map<String, String> subjectSummaryMap = Index.getInstance()
			.getSubjectSummaryMap();
	Map<String, Integer> subjectKeyMap = Index.getInstance().getSubjectKeyMap();

	Map<String, Double> sort(
			List<List<CmdbEntry<String, Integer>>> subjectList);

}
