package com.hbd.cmdb.spider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;

import com.hbd.cmdb.BaseInfo;
import com.hbd.cmdb.index.IndexInfo;
import com.hbd.cmdb.parser.ParserUtil;

public class BuildSubjectData {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		File[] fs = ParserUtil.listSubjectDir();
		FileOutputStream out = new FileOutputStream(new File(
				BaseInfo.totalSubjectFile));
		for (File f : fs) {
			String subPath = f.getAbsolutePath();
			File subjectFile = new File(subPath + BaseInfo.subjectFile);
			if (!subjectFile.exists()) {
				continue;
			}
			String subject = subPath.split("/")[4];
			BufferedReader br = new BufferedReader(new FileReader(subjectFile));
			String line = br.readLine();
			// out.write("-----".getBytes());
			out.write((subject + ":" + line + "\r\n").getBytes());
			br.close();
		}
		out.close();
	}

}
