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
		validInterval = 1800;
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
	public synchronized void invalidate() {
		valid = false;
		attrs.clear();
		// checkout from service factory
	}

	@Override
	public int maxInactiveInterval() {
		return validInterval;
	}

	@Override
	public synchronized void maxInactiveInterval(int interval) {
		access();
		validInterval = interval;
	}

	@Override
	public synchronized void access() {
		lastAccessedTime = Instant.now().toEpochMilli();
	}

	@Override
	public synchronized void attribute(String name, Object value) {
		access();
		attrs.put(name, value);
	}

	@Override
	/**
	 * return null if the entry does not exist
	 */
	public synchronized Object attribute(String name) {
		access();
		return attrs.getOrDefault(name, null);
	}

	@Override
	public synchronized Set<String> attributes() {
		access();
		if (valid) {
			return attrs.keySet();
		}
		return null;
	}

	@Override
	public synchronized void removeAttribute(String name) {
		access();
		attrs.remove(name);
	}

}
