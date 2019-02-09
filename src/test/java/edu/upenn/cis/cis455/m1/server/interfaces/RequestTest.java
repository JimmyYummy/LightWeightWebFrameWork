package edu.upenn.cis.cis455.m1.server.interfaces;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.util.HttpParsing;

public class RequestTest {
	private Request req;

	@Before
	public void setUp() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String content = "GET localhost:8080/index.html?testq=1&testq=2&testq2=3 HTTP/1.1\r\n"
						+ "host: localhost:8080\r\n\r\n";
		Socket sc = TestHelper.getMockSocket(content, out);
		InputStream in = sc.getInputStream(); 
		Map<String, String> headers = new HashMap<>();
		Map<String, List<String>> parms = new HashMap<>();
		String uri = HttpParsing.parseRequest("0.0.0.0:1", in, headers, parms);
		req = ServiceFactory.createRequest(sc, uri, false, headers, parms);
	}

	@Test
	public void testRequestMethod() {
		assertEquals("GET", req.requestMethod());
	}

	@Test
	public void testHost() {
		assertEquals("localhost:8080", req.host());
	}

	@Test
	public void testUserAgent() {
		assertEquals("UNKNOWN", req.userAgent());
	}

	@Test
	public void testPort() {
		assertEquals(8080, req.port());
	}

	@Test
	public void testPathInfo() {
		assertEquals("/index.html", req.pathInfo());
	}

	@Test
	public void testUrl() {
		assertEquals("localhost:8080/index.html?testq=1&testq=2&testq2=3", req.url());
	}

	@Test
	public void testUri() {
		assertEquals(req.url(), req.uri());
	}

	@Test
	public void testProtocol() {
		assertEquals("HTTP/1.1", req.protocol());
	}

	@Test
	public void testContentType() {
		assertEquals(null, req.contentType());
	}

	@Test
	public void testIp() {
		assertEquals("0.0.0.0:1", req.ip());
	}

	@Test
	public void testBody() {
		assertEquals("", req.body());
	}

	@Test
	public void testContentLength() {
		assertEquals(0, req.contentLength());
	}

	@Test
	public void testHeadersString() {
		Set<String> headers = new HashSet<>();
		headers.add("host");
		headers.add("http-client-ip");
		headers.add("remote-addr");
		headers.add("useragent");
		headers.add("protocolVersion");
		headers.add("Method");
		headers.add("pathinfo");
		assertEquals(headers, req.headers());
	}

	@Test
	public void testHeaders() {
		assertEquals("localhost:8080", req.headers("host"));
	}

	@Test
	public void testPersistentConnection() {
		assertFalse(req.persistentConnection());
	}

	@Test
	public void testPersistentConnectionBoolean() {
		req.persistentConnection(true);
		assertTrue(req.persistentConnection());
	}

}
