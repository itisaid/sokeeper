package com.hbd.cmdb.spider;

import java.io.File;
import java.io.FileOutputStream;

import com.hbd.cmdb.BaseInfo;

public class SubjectWorker extends BaseWorker {

	public SubjectWorker() {
		fs = BaseInfo.path + "fs.txt";
	}

	void loadSubject(String subject) throws Exception {
		String url = urlPrefix + subject;
		String res = SpiderUtil.request(url);
		if (res == null) {
			return;
		}
		File dir = new File(BaseInfo.output + subject);
		if (!dir.exists()) {
			dir.mkdir();
		}

		FileOutputStream subOs = new FileOutputStream(new File(BaseInfo.output
				+ subject + "/subject.html"));
		subOs.write(res.getBytes());
		subOs.close();

	}

}
