package edu.upenn.cis.cis455.m1.server.implementations;

import java.util.HashMap;
import java.util.Map;

import edu.upenn.cis.cis455.m1.server.interfaces.Response;

public class BasicResponse extends Response {
	public Map<String, String> headers;
	private static BasicResponse commonResponse;

	
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

	@Override
	public String toString() {
		return String.format("%d\n\n%s\n\n%s", statusCode, getHeaders(), body());
	}
	
	public boolean putHeader(String key, String val) {
		return headers.put(key, val) != null;
	}
	
    public static Response get100Response() {
    	if (commonResponse == null) {
    		commonResponse = new BasicResponse();
    		commonResponse.status(100);
    	}
    	return commonResponse;
    }
}
