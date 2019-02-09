package edu.upenn.cis.cis455.m1.server;

import static org.junit.Assert.*;

import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

public class HttpTaskQueueTest {
	private HttpTaskQueue q;
	private HttpTask t;

	@Before
	public void setUp() throws Exception {
		t = new HttpTask(new Socket(), 8080);
		q = new HttpTaskQueue();
		for (int i = 0; i < 10; i++) {
			q.offer(t);
		}
	}

	@Test
	public void testOffer() {
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				q.offer(t);
			}).start();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(20, q.size());
	}
	
	@Test(expected = IllegalStateException.class)
	public void testUnactiveOffer() {
		q.unactive();
		q.offer(t);
	}

	@Test
	public void testPoll() {
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				q.poll();
			}).start();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(0, q.size());
	}

	@Test
	public void testUnactivePoll() {
		q.unactive();
		assertEquals(null, q.poll());
	}

}
