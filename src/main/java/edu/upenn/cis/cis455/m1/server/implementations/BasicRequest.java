package edu.upenn.cis.cis455.m1.server.implementations;

import java.util.ArrayList;
import java.util.Collection;
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
	
	private BasicRequest() {
		
	}
	
	@Override
	public String requestMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String host() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String userAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int port() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String pathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String url() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String uri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String protocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String contentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String ip() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String body() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int contentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String headers(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> headers() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static class BasicRequestFactory {
		
		private static Collection<String> requiredHeaders;
		
		static {
			requiredHeaders = new ArrayList<>();
			requiredHeaders.add("host");
			requiredHeaders.add("useragent");
			requiredHeaders.add("port");
			requiredHeaders.add("pathinfo");
			requiredHeaders.add("contentType");
			requiredHeaders.add("ip");
		}
		
		public BasicRequest getBasicRequest(HttpMethod method, 
				String url, String protocolVersion, Map<String, String> headers) {
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
			request.headers = headers;
			return request;
		}
		
		private static boolean isValidURL(String url) {
			return false;
		}
	}
}
