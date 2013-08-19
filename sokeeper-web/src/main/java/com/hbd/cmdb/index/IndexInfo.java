package com.hbd.cmdb.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbd.cmdb.BaseInfo;

public class IndexInfo {
	public static String wordCountFile = BaseInfo.indexPath + "wordcount.dat";
	public static String wordIndexFile = BaseInfo.indexPath + "wordindex.dat";
	public static String blackWordsFile = BaseInfo.indexPath + "blackwords.dat";
	public static String subjectKeyFile = BaseInfo.indexPath + "subjectkey.dat";
	
	public static List<Entry<String,Integer>> sortMap(Map<String,Integer> map){
		// sort
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>();
		for (Entry<String, Integer> entry : map.entrySet()) {
			list.add(entry);
		}
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> e1,
					Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}

		});
		return list;
	}
	
	public static List<Entry<String,Double>> sortMapDouble(Map<String,Double> map){
		// sort
		List<Entry<String, Double>> list = new ArrayList<Entry<String, Double>>();
		for (Entry<String, Double> entry : map.entrySet()) {
			list.add(entry);
		}
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> e1,
					Entry<String, Double> e2) {
				double d = e2.getValue() - e1.getValue();
				if(d>0){
					return 1;
				}
				if(d<0){
					return -1;
				}
				return 0;
			}

		});
		return list;
	}
}
