package com.hbd.cmdb.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import com.hbd.cmdb.BaseInfo;
import com.hbd.cmdb.parser.ParserUtil;

public class WordCount {


	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		WordCount wc = new WordCount();
		wc.countWord();
		System.out.println("----------------finished");
	}

	public void countWord() throws Exception {
		File[] fs = ParserUtil.listSubjectDir();
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		int fileCount=0;
		for (File f : fs) {
			String subPath = f.getAbsolutePath();
			File reviewData = new File(subPath + BaseInfo.reviewFile);
			if (!reviewData.exists()) {
				continue;
			}
			BufferedReader br = new BufferedReader(new FileReader(reviewData));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("-----")) {
					continue;
				}
				countLine(countMap, line);
			}
			br.close();
			System.out.println(reviewData.getPath()+"------"+fileCount++);
		}

		// sort
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>();
		for (Entry<String, Integer> entry : countMap.entrySet()) {
			list.add(entry);
		}
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> e1,
					Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}

		});

		FileOutputStream subOut = new FileOutputStream(new File(IndexInfo.wordCountFile));
		// for (Entry<String, Integer> entry : countMap.entrySet()) {
		for (Entry<String, Integer> entry : list) {
			String word = entry.getKey();
			int count = entry.getValue();
			subOut.write((word + " " + count + "\r\n").toString().getBytes());
		}
		subOut.close();
	}

	void countLine(Map<String, Integer> countMap, String line) {
		List<Term> terms = ToAnalysis.paser(line);
		for (Term term : terms) {
			String word = term.getName();
			Integer value = countMap.get(word);
			if (value == null) {
				countMap.put(word, 1);
			} else {
				countMap.put(word, value + 1);
			}
		}
	}
}
