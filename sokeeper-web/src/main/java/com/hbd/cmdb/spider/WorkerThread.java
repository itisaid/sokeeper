package com.hbd.cmdb.spider;

public class WorkerThread extends Thread {
	Worker worker;
	private boolean finished;
	private boolean error;

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public WorkerThread(Worker worker) {
		this.worker = worker;
	}

	public void run() {
		try {
			worker.execute();
			finished = true;
		} catch (Throwable t) {
			t.printStackTrace();
			error = true;
		}
	}
}
