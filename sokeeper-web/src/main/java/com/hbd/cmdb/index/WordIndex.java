package com.hbd.cmdb.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import com.hbd.cmdb.BaseInfo;
import com.hbd.cmdb.parser.ParserUtil;
import com.hbd.cmdb.search.BlackWords;

public class WordIndex {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		WordIndex wi = new WordIndex();
		wi.index();
		System.out.println("-------------end");

	}

	public void index() throws Exception {
		FileOutputStream subKeyOut = new FileOutputStream(new File(
				IndexInfo.subjectKeyFile));
		File[] fs = ParserUtil.listSubjectDir();
		Map<String, Map<String, Integer>> indexMap = new HashMap<String, Map<String, Integer>>();
		int fileCount = 0;
		for (File f : fs) {
			String subPath = f.getAbsolutePath();
			File reviewData = new File(subPath + BaseInfo.reviewFile);
			if (!reviewData.exists()) {
				continue;
			}
			System.out.println(reviewData.getPath() + "------" + fileCount++);
			String subject = subPath.split("/")[4];
			Map<String, Integer> subjectKeyMap = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(reviewData));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("-----")) {
					continue;
				}
				indexWord(subjectKeyMap, indexMap, line, subject);
			}
			br.close();
			List<Entry<String, Integer>> subjectKeyList = IndexInfo
					.sortMap(subjectKeyMap);
			subKeyOut.write((subject + ":").getBytes());
			int max = subjectKeyList.size() >= 5 ? 5 : subjectKeyList.size();
			for (int i = 0; i < max; i++) {
				Entry<String, Integer> e = subjectKeyList.get(i);
				subKeyOut.write((e.getKey() + "," + e.getValue() + ";")
						.getBytes());
			}
			subKeyOut.write("\r\n".getBytes());
			
		}
		subKeyOut.close();
		FileOutputStream subOut = new FileOutputStream(new File(
				IndexInfo.wordIndexFile));
		for (Entry<String, Map<String, Integer>> indexEntry : indexMap
				.entrySet()) {
			String word = indexEntry.getKey();
			if (word == null || word.trim().equals("")
					|| word.indexOf(',') != -1 || word.indexOf(':') != -1
					|| word.indexOf(';') != -1) {
				continue;
			}
			Map<String, Integer> subjectMap = indexEntry.getValue();
			List<Entry<String, Integer>> list = IndexInfo.sortMap(subjectMap);
			int indexSize = 0;
			subOut.write((word + ":").getBytes());
			for (Entry<String, Integer> entry : list) {
				if (indexSize++ > 200) {
					break;
				}
				String subject = entry.getKey();
				int count = entry.getValue();
				subOut.write((subject + "," + count + ";").toString()
						.getBytes());
			}
			subOut.write("\r\n".getBytes());

		}
		subOut.close();
	}

	void indexWord(Map<String, Integer> subjectKeyMap,
			Map<String, Map<String, Integer>> indexMap, String line,
			String subject) {

		List<Term> terms = ToAnalysis.paser(line);
		BlackWords blackWords = BlackWords.getInstance();
		for (Term term : terms) {
			String word = term.getName();
			if (blackWords.isBlackWord(word)) {
				continue;
			}

			Integer subjectKeyCount = subjectKeyMap.get(word);
			if (subjectKeyCount == null) {
				subjectKeyMap.put(word, 1);
			} else {
				subjectKeyMap.put(word, subjectKeyCount + 1);
			}

			Map<String, Integer> subjectMap = indexMap.get(word);
			if (subjectMap == null) {
				subjectMap = new HashMap<String, Integer>();
				subjectMap.put(subject, 1);
				indexMap.put(word, subjectMap);
			} else {
				Integer count = subjectMap.get(subject);
				if (count == null) {
					subjectMap.put(subject, 1);
				} else {
					subjectMap.put(subject, count + 1);
				}
			}
		}

	}
}
