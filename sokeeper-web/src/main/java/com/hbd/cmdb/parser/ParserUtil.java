package com.hbd.cmdb.parser;

import java.io.BufferedReader;
import java.io.File;
import java.util.Stack;

import com.hbd.cmdb.BaseInfo;

public class ParserUtil {

	public static File[] listSubjectDir() {
		File dir = new File(BaseInfo.output);
		File[] fs = dir.listFiles();
		return fs;
	}

	public static String getInfo(BufferedReader br, String tag)
			throws Exception {
		Stack<Boolean> stack = new Stack<Boolean>();
		stack.push(true);
		String line;
		StringBuffer content = new StringBuffer();
		while ((line = br.readLine()) != null) {
			String text = filterTag(line);
			if (text != null && !"".equals(text)) {
				content.append(text + "\r\n");
			}
			if (line.indexOf("<" + tag) != -1
					&& !(line.indexOf("</" + tag) != -1)) {
				stack.push(true);
			}
			if (!(line.indexOf("<" + tag) != -1)
					&& line.indexOf("</" + tag) != -1) {
				stack.pop();
			}
			if (stack.isEmpty()) {
				break;
			}

		}
		return content.toString();
	}

	public static void splitLine(StringBuffer sb) {
		sb.append("-----\r\n");
	}

	public static String filterTag(String line) {
		int length = line.length();
		boolean isTag = false;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (line.charAt(i) == '<') {
				isTag = true;
			}
			if (!isTag) {
				sb.append(line.charAt(i));
			}
			if (line.charAt(i) == '>' && isTag) {
				isTag = false;
			}
		}
		return sb.toString().trim();
	}
}
