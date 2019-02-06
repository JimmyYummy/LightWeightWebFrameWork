package edu.upenn.cis.cis455.m1.server;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.util.HttpParsing;

public class HttpWorkerTest {
	HttpWorker worker;
	Socket sc;
	
	
//	@Before
//	public void setUp() throws Exception {
//		worker = new HttpWorker(null);
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		sc = TestHelper.getMockSocket("GET / HTTP/1.1\n"
//				+ "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\n"
//				+ "Host: www.tutorialspoint.com:80\n" + "Content-Type: text/xml; charset=utf-8\n"
//				+ "Content-Length: length\n" + "Accept-Language: en-us\n" + "Accept-Encoding: gzip, deflate\n"
//				+ "Connection: Keep-Alive\n" + "\n" + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
//				+ "<string xmlns=\"http://clearforest.com/\">string</string>\n"
//				+ "123321", out);
//		
//	}

//	@Test
//	public void test() {
//		System.out.println(sc.getInetAddress());
//		try{
//			worker.work(sc);
//		} catch (Exception e) {
//			
//		}
//		try {
//			System.out.println(sc.getOutputStream().toString());
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}

}
