package com.hbd.cmdb.spider;

public class ImgLoader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WorkerThread wt = new WorkerThread(new ImgWorker());
		wt.start();
		while (true) {
			if (wt.isError()) {
				wt = new WorkerThread(new ReviewWorker());
				wt.start();
			}
			if (wt.isFinished()) {
				break;
			}
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
