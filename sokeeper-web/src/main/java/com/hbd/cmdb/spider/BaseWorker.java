package com.hbd.cmdb.spider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import com.hbd.cmdb.BaseInfo;

public abstract class BaseWorker implements Worker {
	Set<String> allSubject = new HashSet<String>();
	Set<String> finishedSubject = new HashSet<String>();
	String urlPrefix = "http://movie.douban.com/subject/";
	// go on downloading from break point.
	String fs;
	private FileOutputStream out;

	public void execute() throws Exception {
		init(fs);
		try {
			for (String subject : allSubject) {
				if (!finishedSubject.contains(subject)) {
					loadSubject(subject);
					out.write((subject + "\r\n").getBytes());
				}
			}
		} finally {
			out.close();
		}
	}

	abstract void loadSubject(String subject) throws Exception;

	void init(String fs) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				BaseInfo.subjectPath));
		String line;
		while ((line = br.readLine()) != null) {
			allSubject.add(line);
		}
		br.close();

		br = new BufferedReader(new FileReader(fs));
		while ((line = br.readLine()) != null) {
			finishedSubject.add(line);
		}

		out = new FileOutputStream(new File(fs), true);
	}

}
