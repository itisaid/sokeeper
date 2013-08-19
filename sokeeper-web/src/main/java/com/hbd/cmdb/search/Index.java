package com.hbd.cmdb.search;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbd.cmdb.BaseInfo;
import com.hbd.cmdb.index.IndexInfo;

public class Index {
	Map<String, List<CmdbEntry<String, Integer>>> indexMap = new HashMap<String, List<CmdbEntry<String, Integer>>>();
	Map<String, String> subjectSummaryMap = new HashMap<String, String>();
	Map<String, Integer> subjectKeyMap = new HashMap<String, Integer>();

	private static Index instance = new Index();

	private Index() {
		initIndex();
		initSubject();
		initKey();
	}

	public static Index getInstance() {
		return instance;
	}

	public Map<String, List<CmdbEntry<String, Integer>>> getIndexMap() {
		return indexMap;
	}

	public Map<String, String> getSubjectSummaryMap() {
		return subjectSummaryMap;
	}

	public Map<String, Integer> getSubjectKeyMap() {
		return this.subjectKeyMap;
	}

	private void initKey() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					IndexInfo.subjectKeyFile), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] subject = line.split(":");
				if (subject.length != 2) {
					throw new RuntimeException(line);
				}
				String[] key = subject[1].split(";");
				if (key.length < 1) {
					throw new RuntimeException(line);
				}
				String[] count = key[0].split(",");
				if (count.length < 1) {
					throw new RuntimeException(line);
				}
				subjectKeyMap.put(subject[0], Integer.valueOf(count[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				br.close();
			} catch (Throwable e1) {
				e1.printStackTrace();
				throw new RuntimeException("init failed.");
			}
		}
	}

	/**
	 * init indexMap
	 */
	private void initIndex() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					IndexInfo.wordIndexFile), "UTF-8"));
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
				e1.printStackTrace();
				throw new RuntimeException("init failed.");
			}
		}
	}

	private void initSubject() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(BaseInfo.totalSubjectFile), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] sub = line.split(":");
				subjectSummaryMap.put(sub[0], sub[1]);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("init failed.");
		}
	}

	public static void main(String[] args) {
		Index init = new Index();
		for (Entry<String, Integer> e : init.getSubjectKeyMap().entrySet()) {
			System.out.println(e.getKey() + ":" + e.getValue());
		}
	}
}
