package edu.upenn.cis.cis455.m1.server.interfaces;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.ServiceFactory;

public class ResponseTest {
	private Response res;
	
	@Before
	public void setUp() throws Exception {
		res = ServiceFactory.createResponse();
	}

	@Test
	public void testStatus() {
		assertEquals(200, res.status());
	}

	@Test
	public void testStatusInt() {
		res.status(201);
		assertEquals(201, res.status());
	}

	@Test
	public void testBody() {
		assertEquals("", res.body());
	}

	@Test
	public void testBodyRaw() {
		assertArrayEquals(new byte[0], res.bodyRaw());
	}

	@Test
	public void testBodyRawByteArray() {
		String s = "raw body";
		byte[] b = s.getBytes();
		res.bodyRaw(b);
		assertArrayEquals(b, res.bodyRaw());
		assertEquals(s, res.body());
	}

	@Test
	public void testBodyString() {
		String s = "raw body";
		byte[] b = s.getBytes();
		res.body(s);
		assertArrayEquals(b, res.bodyRaw());
		assertEquals(s, res.body());
	}

	@Test
	public void testType() {
		assertEquals("text/plain", res.type());
	}

	@Test
	public void testTypeString() {
		res.type("text/html");
		assertEquals("text/html", res.type());
	}

	@Test
	public void testGetHeaders() {
		assertEquals("", res.getHeaders());
	}

}
