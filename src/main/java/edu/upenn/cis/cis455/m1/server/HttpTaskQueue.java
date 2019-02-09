package edu.upenn.cis.cis455.m1.server;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class HttpTaskQueue {
	
	final static Logger logger = LogManager.getLogger(HttpTaskQueue.class);
	
	private boolean isActive;
	
	private Queue<HttpTask> q;
	
	public HttpTaskQueue() {
		q = new ArrayDeque<HttpTask>();
		isActive = true;
	}
	
	public synchronized void offer(HttpTask t) {
		if (isActive) {
			logger.info("offered new task: " + t.getSocket());
			q.offer(t);
			if (q.size() == 1) {
				this.notify();
			}
		} else {
			logger.error("offering task to a unactive task queue");
			throw new IllegalStateException("The queue is deactivated");
		}
		
	}
	
	public synchronized HttpTask poll() {
		while (q.size() == 0 && isActive) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				logger.error("Error caught: The Polling is Interrupted (may not be an error) - " + e.getMessage());
			}
		}
		
		HttpTask t = null;
		if (isActive) {
			t = q.poll();
		}
		logger.info("task polled: " + t);
		return t;
	}
	
	public synchronized void unactive() {
		isActive = false;
		this.notifyAll();
	}
	
	public synchronized int size() {
		return q.size();
	}
}
