package edu.upenn.cis.cis455.m1.server;

import static org.junit.Assert.*;

import java.net.Socket;

import org.junit.Test;

public class HttpTaskTest {


	@Test
	public void testHttpTask() {
		Socket sc = new Socket();
		HttpTask task = new HttpTask(sc, 8080);
		assertEquals(sc, task.getSocket());
		assertEquals(8080, task.getPort());
	}
}
