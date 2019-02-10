package edu.upenn.cis.cis455.m2.server.implementations;

import java.time.Instant;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.m2.server.interfaces.Session;

public class BasicSession extends Session {
	private final String id;
	private final long creationTime;
	private long lastAccessedTime;
	private boolean valid;
	private int validInterval;
	private Map<String, Object> attrs;
	
	
	
	public BasicSession() {
		id = UUID.randomUUID().toString();
		creationTime = Instant.now().toEpochMilli();
		lastAccessedTime = creationTime;
		valid = true;
		validInterval = 600000;
		attrs = new HashMap<>();
	}
	
	@Override
	public String id() {
		return id;
	}

	@Override
	public long creationTime() {
		return creationTime;
	}

	@Override
	public long lastAccessedTime() {
		return lastAccessedTime;
	}

	@Override
	public void invalidate() {
		valid = false;
	}

	@Override
	public int maxInactiveInterval() {
		return validInterval;
	}

	@Override
	public void maxInactiveInterval(int interval) {
		validInterval = interval;
	}

	@Override
	public void access() {
		lastAccessedTime = Instant.now().toEpochMilli();
	}

	@Override
	public void attribute(String name, Object value) {
		attrs.put(name, value);
	}

	@Override
	/**
	 * return null if the entry does not exist
	 */
	public Object attribute(String name) {
		return attrs.getOrDefault(name, null);
	}

	@Override
	public Set<String> attributes() {
		return attrs.keySet();
	}

	@Override
	public void removeAttribute(String name) {
		attrs.remove(name);
	}

}
