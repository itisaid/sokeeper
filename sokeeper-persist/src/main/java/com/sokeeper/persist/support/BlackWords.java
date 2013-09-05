package com.sokeeper.persist.support;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Set;

public class BlackWords {

	static Set<String> blackWords = new HashSet<String>();
	private static BlackWords instance = new BlackWords();

	private BlackWords() {
		blackWords.add(" ");
		try {
			BufferedReader br = ServiceUtil.getReader("blackwords.dat");
			String line;
			while ((line = br.readLine()) != null) {
				blackWords.add(line.split(" ")[0]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static BlackWords getInstance() {
		return instance;
	}

	public boolean isBlackWord(String word) {
		return blackWords.contains(word);
	}

}
