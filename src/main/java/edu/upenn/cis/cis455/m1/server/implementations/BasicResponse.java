package edu.upenn.cis.cis455.m1.server.implementations;

import java.util.HashMap;
import java.util.Map;

import edu.upenn.cis.cis455.m1.server.interfaces.Response;

public class BasicResponse extends Response {
	public Map<String, String> headers;
	
	public BasicResponse(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public BasicResponse() {
		headers = new HashMap<>();
	}
	
	@Override
	public String getHeaders() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(String.format("%s : %s\r\n", header.getKey(), header.getValue()));
		}
		return sb.toString();
	}

}
