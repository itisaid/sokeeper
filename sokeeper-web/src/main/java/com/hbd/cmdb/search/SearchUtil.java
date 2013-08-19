package com.hbd.cmdb.search;

import java.util.List;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

public class SearchUtil {

	public static List<Term> splitWords(String words) {
		return ToAnalysis.paser(words);
	}
}
