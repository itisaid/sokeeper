package com.hbd.cmdb.spider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbd.cmdb.BaseInfo;

public class ReviewWorker extends BaseWorker {

	public ReviewWorker() {
		fs = BaseInfo.path + "review.txt";
	}

	@Override
	void loadSubject(String subject) throws Exception {
		String url = urlPrefix + subject + "/reviews";
		String res = SpiderUtil.request(url);
		Set<String> reviewUrl = new HashSet<String>();
		while (res != null) {
			File dir = new File(BaseInfo.output + subject);
			if (!dir.exists()) {
				dir.mkdir();
			}
			List<String> linkList = SpiderUtil.findLink(res);
			boolean hasReview = false;
			for (String link : linkList) {
				if (isReviewLink(link) && !reviewUrl.contains(link)) {
					String reviewId = link.split("/")[4];
					try {
						Integer.valueOf(reviewId);
					} catch (Exception e) {
						continue;
					}
					String reviewPage = SpiderUtil.request(link);
					if (reviewPage == null) {
						continue;
					}
					hasReview = true;
					FileOutputStream subOs = new FileOutputStream(new File(
							BaseInfo.output + subject + "/"
									+ link.split("/")[4] + ".rev"));
					subOs.write(reviewPage.getBytes());
					subOs.close();
					reviewUrl.add(link);
				}
			}
			String nextUrl = getNextPage(res);
			if (nextUrl != null && hasReview) {
				nextUrl = url + nextUrl;
				res = SpiderUtil.request(nextUrl);
			} else {
				res = null;
			}
		}
	}

	String getNextPage(String res) {
		int nextInd = res.lastIndexOf("后一页");
		if (nextInd == -1) {
			return null;
		}
		int preInd = res.lastIndexOf("前一页");
		if (nextInd - preInd > 200) {
			return null;
		}
		String nextUrlString = res.substring(preInd, nextInd);
		List<String> nextUrls = SpiderUtil.findLink(nextUrlString);
		if (nextUrls.isEmpty() || nextUrls.size() > 1) {
			return null;
		}
		return nextUrls.get(0);
	}

	boolean isReviewLink(String link) {
		if (link.startsWith("http://movie.douban.com/review/")) {
			return true;
		} else {
			return false;
		}
	}

}
