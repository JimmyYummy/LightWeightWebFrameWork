package edu.upenn.cis.cis455.m2.server.implementations;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.upenn.cis.cis455.ServiceFactory;

public class BasicCookieTest {
	
	@Test
	public void testToString() {
		BasicCookie c = ServiceFactory.createCookie("/", "a", "b", -1, false, false);
		assertEquals("a=b;path=\"/\"", c.toString());
		
		c = ServiceFactory.createCookie("/", "a", "b", 100, false, false);
		assertEquals("a=b;path=\"/\";maxAge=100", c.toString());
		
		c = ServiceFactory.createCookie("/", "a", "b", -2, true, false);
		assertEquals("a=b;path=\"/\";Secure", c.toString());
		
		c = ServiceFactory.createCookie("/", "a", "b", -1, false, true);
		assertEquals("a=b;path=\"/\";HttpOnly", c.toString());
		
		c = ServiceFactory.createCookie("/", "a", "b", 100, true, true);
		assertEquals("a=b;path=\"/\";maxAge=100;Secure;HttpOnly", c.toString());
		
	}

	@Test
	public void testEqualsObject() {
		BasicCookie c1 = ServiceFactory.createCookie("/", "a", "b", -1, false, false);
		BasicCookie c2 = ServiceFactory.createMockCookie("/", "a");
		assertEquals(c1, c2);
		BasicCookie c3 = ServiceFactory.createMockCookie("/", "b");
		BasicCookie c4 = ServiceFactory.createMockCookie("/c", "a");
		assertNotEquals(c1, c3);
		assertNotEquals(c1, c4);
	}

}
