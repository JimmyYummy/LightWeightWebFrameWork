package edu.upenn.cis.cis455.m1.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpThreadPool {
	
	final static Logger logger = LogManager.getLogger(HttpThreadPool.class);
	
	private List<HttpWorker> workers;
	
	public HttpThreadPool(int num) {
		logger.info("creating the threadpool");
		workers = new ArrayList<>(num);
		addThreads(num);
		logger.info("HttpThreadPool running");
	}
	
	public synchronized Map<String, String> getPoolInfo() {
		Map<String, String> infos = new HashMap<>(workers.size());
		for (HttpWorker w : workers) {
			infos.put(w.toString(), w.isWorking() ? "handling URL" : "waiting");
		}
		
		return infos;
	}

	public synchronized void addThread() {
		HttpWorker t = new HttpWorker();
		workers.add(t);
		t.start();
		logger.info("created new worker, current pool size: " + workers.size());
	}

	public void addThreads(int num) {
		for (int i = 0; i < num; i++) {
			addThread();
		}
	}
	
	public synchronized void closeThread(Thread t) {
		HttpWorker w = (HttpWorker) t;
		workers.remove(w);
		w.turnOffWorker();
		logger.info("deleted http worker, current pool size: " + workers.size());
	}

	public int closeThreads(int n) {
		for (int i = 0; i < n; i++) {
			if (workers.size() == 0) return i;
			closeThread(workers.get(workers.size() - 1));
		}
		return n;
	}
			
	public void closeAll() {
		while (! workers.isEmpty()) {
			closeThread(workers.get(workers.size() - 1));
		}
		logger.info("All the workers are turned off");
	}
}
