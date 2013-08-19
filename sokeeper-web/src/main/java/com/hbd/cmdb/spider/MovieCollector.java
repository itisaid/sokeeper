package com.hbd.cmdb.spider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.hbd.cmdb.BaseInfo;

/**
 * 
 * @author frank
 * 
 */
public class MovieCollector {

	String rootUrl = "http://movie.douban.com/tag/";

	Set<String> movieUrls = new HashSet<String>();
	Set<String> tags = new HashSet<String>();
	String tagPath = BaseInfo.path + "tag.txt";
	FileOutputStream out;

	FileOutputStream tagOut;

	public void collect() throws Exception {
		try {
			readSubject();
			out = new FileOutputStream(new File(BaseInfo.subjectPath), true);
			tagOut = new FileOutputStream(new File(tagPath), true);

			processTag(SpiderUtil.request(rootUrl));

		} finally {
			SpiderUtil.httpClient.getConnectionManager().shutdown();
			out.close();
			tagOut.close();
		}
	}

	void readSubject() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				BaseInfo.subjectPath));
		String line;
		while ((line = br.readLine()) != null) {
			movieUrls.add(line);
		}
		br.close();
		BufferedReader brt = new BufferedReader(new FileReader(tagPath));
		String linet;
		while ((linet = brt.readLine()) != null) {
			tags.add(linet);
		}
		brt.close();
	}

	void processTag(String tagPage) throws Exception {
		List<String> tagList = SpiderUtil.findLink(tagPage);
		for (String tag : tagList) {
			if (tag.charAt(0) == '.') {
				if (!tags.contains(tag)) {
					String tagLink = rootUrl + tag;
					processTagPage(tagLink);
					tags.add(tag);
					tagOut.write((tag + "\r\n").getBytes());
				}
			}
		}
	}

	void processTagPage(String tagLink) throws Exception {
		System.out.println(tagLink);
		String rb = getTagPage(tagLink);
		findMovieInTagPage(rb);
		String nextPage = getNextTagPage(rb);
		if (nextPage != null) {
			processTagPage(nextPage);
		}
	}

	String getNextTagPage(String rb) {
		int ind = rb.lastIndexOf("后页");
		if (ind == -1) {
			return null;
		}
		Stack stack = new Stack();
		int j = ind;
		boolean ok = false;
		int i = 0;
		while (true) {
			if (ok) {
				stack.push(rb.charAt(j));
			}
			if (rb.charAt(j) == '"') {
				i++;
			}
			j--;
			if (i == 1) {
				ok = true;
			}
			if (i >= 2) {
				stack.pop();
				break;
			}
		}
		StringBuffer sb = new StringBuffer();
		while (!stack.isEmpty()) {
			sb.append(stack.pop());
		}
		String s = sb.toString();
		if (s.indexOf("http") == -1) {
			return null;
		}
		return s;
	}

	void findMovieInTagPage(String res) throws Exception {
		List<String> movieLinkList = SpiderUtil.findLink(res);
		for (String movieLink : movieLinkList) {
			if (isMovieLink(movieLink)) {
				String subject = movieLink.split("/")[4];
				if (!movieUrls.contains(subject)) {
					movieUrls.add(subject);
					out.write((subject + "\r\n").getBytes());
				}
			}
		}
	}

	String getTagPage(String link) throws Exception {
		return SpiderUtil.request(link);
	}

	boolean isMovieLink(String link) {
		if (link.startsWith("http://movie.douban.com/subject/")) {
			return true;
		} else {
			return false;
		}
	}

}
