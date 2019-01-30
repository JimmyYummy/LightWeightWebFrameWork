package edu.upenn.cis.cis455.m1.server.interfaces;

public interface ThreadPool {
	public void addThreads(int num);
	public void addThread();
	public void closeThread(Thread t);
	public int closeThreads(int n);
	public void closeAll();
}