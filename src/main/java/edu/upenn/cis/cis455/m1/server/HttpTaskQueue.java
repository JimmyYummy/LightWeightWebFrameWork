package edu.upenn.cis.cis455.m1.server;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class HttpTaskQueue {
	private Queue<HttpTask> q;
	private int size;
	
	public HttpTaskQueue() {
		q = new LinkedList<HttpTask>();
		size = 1000;
	}
	
	public void setSize(int s) {
		size = s;
	}
	
	public void offer(HttpTask t) {
		while (q.size() >= size);
		q.offer(t);
		this.notifyAll();
	}
	
	public synchronized HttpTask poll() {
		while (q.size() == 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		HttpTask t = q.poll();
		this.notifyAll();
		return t;
	}
}
