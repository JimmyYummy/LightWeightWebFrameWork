package edu.upenn.cis.cis455.m1.server;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class HttpTaskQueue {
	
	final static Logger logger = LogManager.getLogger(HttpTaskQueue.class);
	
	private Queue<HttpTask> q;
	
	public HttpTaskQueue() {
		q = new LinkedList<HttpTask>();
	}
	
	public synchronized void offer(HttpTask t) {
		q.offer(t);
		if (q.size() == 1) {
			this.notify();
		}
	}
	
	public synchronized HttpTask poll() {
		while (q.size() == 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		HttpTask t = q.poll();
		return t;
	}
}
