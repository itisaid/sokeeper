package com.hbd.cmdb.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import com.hbd.cmdb.BaseInfo;
import com.hbd.cmdb.index.IndexInfo;
import com.hbd.cmdb.parser.ParserUtil;

public class SearchMain {
	Map<String, List<CmdbEntry<String, Integer>>> indexMap = new HashMap<String, List<CmdbEntry<String, Integer>>>();
	Map<String, String> subjectSummaryMap = new HashMap<String, String>();

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String keyWords = "父子情";
		SearchMain sm = new SearchMain();
		sm.init();
		sm.printResult(sm.search(keyWords));
	}

	void init() throws Exception {
		initIndex();
		initSubject();
	}

	/**
	 * init indexMap
	 */
	public void initIndex() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(IndexInfo.wordIndexFile));
			String line;
			while ((line = br.readLine()) != null) {
				String[] word = line.split(":");
				if (word.length != 2) {
					continue;
				}
				String[] subject = word[1].split(";");
				if (subject.length < 1) {
					continue;
				}
				List<CmdbEntry<String, Integer>> list = new ArrayList<CmdbEntry<String, Integer>>();
				for (int i = 0; i < subject.length; i++) {
					String[] count = subject[i].split(",");
					CmdbEntry<String, Integer> entry = new CmdbEntry<String, Integer>();
					entry.setKey(count[0]);
					entry.setValue(Integer.valueOf(count[1]));
					list.add(entry);
				}
				indexMap.put(word[0], list);
			}
		} catch (Exception e) {
			try {
				br.close();
			} catch (Throwable e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	void initSubject() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				BaseInfo.totalSubjectFile));
		String line;
		while ((line = br.readLine()) != null) {
			String[] sub = line.split(":");
			subjectSummaryMap.put(sub[0], sub[1]);
		}

	}

	public List<Entry<String, Integer>> search(String keyWords) {
		List<Term> terms = ToAnalysis.paser(keyWords);
		Map<String, Integer> subjectMap = new HashMap<String, Integer>();
		for (Term term : terms) {
			String key = term.getName();
			System.out.println("search key:" + key);
			if (indexMap.containsKey(key)) {
				for (CmdbEntry<String, Integer> e : indexMap.get(key)) {
					String subject = e.getKey();
					int value = e.getValue();
					Integer count = subjectMap.get(subject);
					if (count == null) {
						subjectMap.put(subject, value);
					} else {
						subjectMap.put(subject, count + value);
					}
				}
			}
		}
		List<Entry<String, Integer>> list = IndexInfo.sortMap(subjectMap);

		return list;
	}

	public void printResult(List<Entry<String, Integer>> list) {
		int num = 0;
		for (Entry<String, Integer> e : list) {
			if (num++ > 20) {
				break;
			}
			String key = e.getKey();
			System.out.println(subjectSummaryMap.get(key));
		}
	}

}
