package edu.upenn.cis.cis455.m2.server.implementations;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BasicCookie {
	private Path path;
	private String name;
	private String value;
	private int maxAge;
	private boolean secured;
	private boolean httpOnly;
	
	public BasicCookie(String path, String name, String value, int maxAge, 
			boolean secured, boolean httpOnly) {
		if (path == null) {
			path = "/";
		} else {
			path = "/" + path;
		}
		if (name == null || value == null) {
			throw new IllegalArgumentException("The name or value cannot be null.");
		}
		this.path = Paths.get(path).normalize();
		this.name = name;
		this.value = value;
		this.maxAge = maxAge;
		this.secured  = secured;
		this.httpOnly = httpOnly;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append('=');
		sb.append(value);
		sb.append(String.format(";path=\"%s\"", path));
		if (maxAge >= 0) {
			sb.append(";maxAge=" + maxAge);
		}
		if (secured) {
			sb.append(";Secure");
		}
		if (httpOnly) {
			sb.append(";HttpOnly");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return path.hashCode() + name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (! (o instanceof BasicCookie)) {
			return  false;
		}
		BasicCookie c = (BasicCookie) o;
		return this.path.equals(c.path) && this.name.equals(c.name);
	}
}
