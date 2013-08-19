package com.hbd.cmdb.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;

import com.hbd.cmdb.BaseInfo;

public class ReviewParser {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ReviewParser rp = new ReviewParser();
		rp.parse();

	}

	public void parse() throws Exception {
		File[] fs = ParserUtil.listSubjectDir();
		for (File f : fs) {
			String subPath = f.getAbsolutePath();
			File reviewData = new File(subPath + BaseInfo.reviewFile);
			if (reviewData.exists()) {
				System.out.println("----"+subPath);
				continue;
			}
			System.out.println("####"+subPath);
			File subDir = new File(subPath);
			File[] reviews = subDir.listFiles(new FileFilter() {
				public boolean accept(File dir) {
					String name = dir.getName();
					if (name.contains(".rev")) {
						return true;
					} else {
						return false;
					}
				}
			});
			StringBuffer content = new StringBuffer();
			if (reviews == null) {
				continue;
			}
			for (File review : reviews) {
				BufferedReader br = new BufferedReader(new FileReader(review));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.indexOf("v:description") != -1
							&& line.indexOf("div") != -1) {
						ParserUtil.splitLine(content);
						content.append(ParserUtil.filterTag(line));
						if (line.indexOf("</div>") == -1) {
							content.append(ParserUtil.getInfo(br, "div"));
						} else {
							content.append("\r\n".getBytes());
						}

					}
				}
				br.close();
			}
			if (content.toString().length() > 1) {
				FileOutputStream subOut = new FileOutputStream(new File(subPath
						+ BaseInfo.reviewFile));
				subOut.write(content.toString().getBytes());
				subOut.close();
			}
		}
	}

}
