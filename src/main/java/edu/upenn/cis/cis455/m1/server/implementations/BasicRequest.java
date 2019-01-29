package edu.upenn.cis.cis455.m1.server.implementations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;

public class BasicRequest extends Request {

	private HttpMethod method;
	private String url;
	private String protocol;
	private Map<String, String> headers;
	private String body;
	
	private BasicRequest() {
		
	}
	
	@Override
	public String requestMethod() {
		return method.toString();
	}

	@Override
	public String host() {
		return headers.get("host");
	}

	@Override
	public String userAgent() {
		return headers.get("useragent");
	}

	@Override
	public int port() {
		return Integer.parseInt(headers.get("port"));
	}

	@Override
	public String pathInfo() {
		return headers.get("pathinfo");
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public String uri() {
		return url;
	}

	@Override
	public String protocol() {
		return protocol;
	}

	@Override
	public String contentType() {
		return headers.get("content-type");
	}

	@Override
	public String ip() {
		return headers.get("remote-addr");
	}

	@Override
	public String body() {
		return body;
	}

	@Override
	public int contentLength() {
		return Integer.parseInt(headers.get("content-length"));
	}

	@Override
	public String headers(String name) {
		return headers.get(name);
	}

	@Override
	public Set<String> headers() {
		return headers.keySet();
	}
	
	public static class BasicRequestFactory {
		
		private static Collection<String> requiredHeaders;
		
		static {
			requiredHeaders = new ArrayList<>();
			requiredHeaders.add("host");
			requiredHeaders.add("useragent");
			requiredHeaders.add("content-type");
			requiredHeaders.add("content-length");
			requiredHeaders.add("remote-addr");
		}
		
		public BasicRequest getBasicRequest(HttpMethod method, 
				String url, String protocolVersion, Map<String, String> headers, String body) {
			BasicRequest request = new BasicRequest();
			request.method = method;
			if ("HTTP/1.1".equals(protocolVersion) || "HTTP/1.2".equals(protocolVersion)) {
				request.protocol = protocolVersion;
			} else {
				throw new IllegalArgumentException("Unsupported Http protocol verison");
			}
			if (! isValidURL(url)) {
				throw new IllegalArgumentException("Illegal URL");
			}
			request.url = url;
			for (String header : requiredHeaders) {
				if (! headers.containsKey(header)) {
					throw new IllegalArgumentException ("Missing header: " + header);
				}
			}
			request.headers = new HashMap(headers);
			String port = "80";
			String host = headers.get("host");
			int idx = host.lastIndexOf(':');
			if (idx != -1) {
				port = host.substring(idx + 1);
			}
			request.headers.put("port", port);
			
			int start = url.lastIndexOf(host) + 1;
			int end = url.indexOf('?');
			if (end == -1) end = url.length();
			if (start >= end) {
				throw new IllegalArgumentException("malformat url or host");
			}
			String pathInfo = url.substring(start, end);
			request.headers.put("pathinfo", pathInfo);
			
			request.body = body;
			return request;
		}
		
		private static boolean isValidURL(String url) {
			return false;
		}
	}
}
