package edu.upenn.cis.cis455.m2.server.implementations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;

public class EnhancedResponse extends Response {
	Map<String, String> headers;
	Set<BasicCookie> cookies;
	
	public EnhancedResponse() {
		headers = new HashMap<>();
	}

	@Override
	public void header(String header, String value) {
		headers.put(header, value);
	}

	@Override
	public void redirect(String location) {
		redirect(location, 302);
	}

	@Override
	public void redirect(String location, int httpStatusCode) {
		statusCode = httpStatusCode;
		header("location", location);
	}

	@Override
	public void cookie(String name, String value) {
		cookie(name, value, -1);
	}

	@Override
	public void cookie(String name, String value, int maxAge) {
		cookie(name, value, maxAge, false);
	}

	@Override
	public void cookie(String name, String value, int maxAge, boolean secured) {
        cookie(name, value, maxAge, secured, false);

	}

	@Override
	public void cookie(String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        cookie(null, name, value, maxAge, secured, httpOnly);
	}

	@Override
	public void cookie(String path, String name, String value) {
		cookie(path, name, value, -1);

	}

	@Override
	public void cookie(String path, String name, String value, int maxAge) {
		cookie(path, name, value, maxAge, false);
	}

	@Override
	public void cookie(String path, String name, String value, int maxAge, boolean secured) {
		cookie(path, name, value, maxAge, secured, false);
	}

	@Override
	public void cookie(String path, String name, String value, int maxAge, 
			boolean secured, boolean httpOnly) {
		BasicCookie c = ServiceFactory.createCookie(path, name, value, maxAge, secured, httpOnly);
		cookies.add(c);
	}

	@Override
	public void removeCookie(String name) {
		removeCookie(null, name);
	}

	@Override
	public void removeCookie(String path, String name) {
		cookies.remove(ServiceFactory.createMockCookie(path, name));
	}

	@Override
	public String getHeaders() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(String.format("%s:%s\r\n", header.getKey(), header.getValue()));
		}
		for (BasicCookie c : cookies) {
			sb.append(String.format("set-cookie:%s\r\n", c.toString()));
		}
		return sb.toString();
	}

}
