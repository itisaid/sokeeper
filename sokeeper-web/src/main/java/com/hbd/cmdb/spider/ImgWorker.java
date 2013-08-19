package com.hbd.cmdb.spider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import com.hbd.cmdb.BaseInfo;

public class ImgWorker extends BaseWorker {

	public ImgWorker() {
		fs = BaseInfo.path + "img.txt";
	}

	@Override
	void loadSubject(String subject) throws Exception {
		String imgUrl = getImgUrl(subject);
		SpiderUtil.request(imgUrl, BaseInfo.imgPath + subject + ".jpg");
	}

	String getImgUrl(String subject) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(BaseInfo.output
				+ subject + "/subject.html"));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.indexOf("v:image") != -1 && line.indexOf(".jpg") != -1) {
				int begin = line.indexOf("http");
				int end = line.indexOf("jpg");

				if (begin == -1 || end == -1) {
					System.err.println("can't find img:" + subject);
					return null;
				}
				String link = line.substring(begin, end + 3);
				return link;

			}
		}
		System.err.println("can't find img:" + subject);
		return null;
	}

}
