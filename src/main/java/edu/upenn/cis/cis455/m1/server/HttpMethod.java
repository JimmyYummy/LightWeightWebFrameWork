package edu.upenn.cis.cis455.m1.server;

public enum HttpMethod {
	GET,
	HEAD;

	public static HttpMethod parse(String method) {
		switch (method.toLowerCase()){
		case "get":
			return GET;
		case "head":
			return  HEAD;
		default:
			throw new IllegalArgumentException("unsupported method");
		}
	}
}
