package edu.upenn.cis.cis455.m1.server.implementations;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.Socket;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.util.HttpParsing;

@SuppressWarnings("deprecation")
public class BasicRequestTest {
	String url;
	Map<String, String> headers; 
	Map<String, List<String>> parms;
	Socket sc;
	
	@Before
	public void setUp() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sc = TestHelper.getMockSocket("GET https://www.tutorialspoint.com:80/cgi-bin/process.cgi?check=first&check=second&pass=true HTTP/1.1\n"
				+ "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\n"
				+ "Host: www.tutorialspoint.com:80\n" + "Content-Type: text/xml; charset=utf-8\n"
				+ "Content-Length: length\n" + "Accept-Language: en-us\n" + "Accept-Encoding: gzip, deflate\n"
				+ "Connection: Keep-Alive\n" + "\n" + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<string xmlns=\"http://clearforest.com/\">string</string>\n"
				+ "123321", out);
		
		headers = new HashMap<>();
		parms = new HashMap<>();

		url = HttpParsing.parseRequest("0.0.0.0", sc.getInputStream(), headers, parms);
		System.out.println(url);
		System.out.println(headers);
		System.out.println(parms);
	}

	@Test
	public void test() {
		Request r = null;
		try {
			r = BasicRequest.BasicRequestFactory.getBasicRequest(url, sc.getInputStream(), headers, parms);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
