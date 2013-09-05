package com.sokeeper.persist.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ServiceUtil {
	@SuppressWarnings("resource")
	public static BufferedReader getReader(String theFile)
			throws FileNotFoundException {
		BufferedReader reader;
		if (new File(theFile).exists()) {
			reader = new BufferedReader(new FileReader(theFile));
		} else {
			InputStream stream = ServiceUtil.class.getResourceAsStream("/" + theFile);
			reader = new BufferedReader(new InputStreamReader(stream));
		}
		return reader;
	}
}
