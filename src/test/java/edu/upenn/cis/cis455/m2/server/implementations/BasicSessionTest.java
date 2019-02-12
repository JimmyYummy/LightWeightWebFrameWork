package edu.upenn.cis.cis455.m2.server.implementations;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.m2.server.interfaces.Session;

public class BasicSessionTest {
	private Session session;
	private Object obj;

	@Before
	public void setUp() throws Exception {
		session = ServiceFactory.getSession(ServiceFactory.createSession());
		obj = new Object();
		session.attribute("attr", obj);
	}

	@Test
	public void testId() {
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			if (!ids.add(ServiceFactory.createSession())) {
				fail("Duplicate session id");
			}
		}
	}

	@Test
	public void testCreationTime() {
		assertTrue(session.creationTime() <= Instant.now().toEpochMilli());
	}

	@Test
	public void testLastAccessedTime() {
		try {
			Thread.sleep(1);
			assertEquals(session.creationTime(), session.lastAccessedTime());
			session.access();
			assertTrue(session.creationTime() < session.lastAccessedTime());
			Thread.sleep(1);
			assertTrue(session.lastAccessedTime() <= Instant.now().toEpochMilli());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testInvalidate() {
		assertEquals(obj, session.attribute("attr"));
		session.invalidate();
		assertEquals(null, session.attribute("attr"));
		assertEquals(null, ServiceFactory.getSession(session.id()));
	}

	@Test
	public void testMaxInactiveInterval() {
		assertEquals(1800, session.maxInactiveInterval());
	}

	@Test
	public void testMaxInactiveIntervalInt() {
		assertEquals(obj, session.attribute("attr"));
		session.maxInactiveInterval(0);
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(0, session.maxInactiveInterval());
		assertEquals(null, session.attribute("attr"));
		assertEquals(null, ServiceFactory.getSession(session.id()));
	}

	@Test
	public void testAccess() {
		long old = session.lastAccessedTime();
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		session.access();
		assertTrue(old < session.lastAccessedTime());
	}

	@Test
	public void testAttributeStringObject() {
		session.attribute("test", "testObj");
		assertEquals("testObj", session.attribute("test"));
	}

	@Test
	public void testAttributeString() {
		assertEquals(obj, session.attribute("attr"));
	}

	@Test
	public void testAttributes() {
		Set<String> attrs = new HashSet<>();
		attrs.add("attr");
		assertEquals(attrs, session.attributes());
		
		attrs.add("test");
		session.attribute("test", "testObj");
		assertEquals(attrs, session.attributes());
	}

	@Test
	public void testRemoveAttribute() {
		session.attribute("test", new Object());
		session.removeAttribute("test");
		assertEquals(null, session.attribute("test"));
		
	}

}
