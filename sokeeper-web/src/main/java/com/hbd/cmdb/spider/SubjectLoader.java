package com.hbd.cmdb.spider;

/**
 * create subject.html
 * 
 * @author frank
 * 
 */
public class SubjectLoader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WorkerThread wt = new WorkerThread(new SubjectWorker());
		wt.start();
		while (true) {
			if (wt.isError()) {
				wt = new WorkerThread(new SubjectWorker());
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
