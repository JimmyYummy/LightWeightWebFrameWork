package edu.upenn.cis.cis455.m2.server.implementations;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Session;

public class EnhancedRequest extends Request {
	private BasicRequest innerRequest;
	private Map<String, Object> attrs;
	private Map<String, String> params;
	private Map<String, List<String>> qParms;
	private String queryString;

	private Map<String, String> cookies;
	private String sessionId;

	@SuppressWarnings("unused")
	private EnhancedRequest() {
	};

	public EnhancedRequest(Socket socket, String uri, boolean keepAlive, Map<String, String> headers,
			Map<String, List<String>> parms) {
		this.innerRequest = ServiceFactory.createBasicRequest(socket, uri, keepAlive, headers, parms);
		this.attrs = new HashMap<>();
		this.params = new HashMap<>();
		this.qParms = parms;
		
		int qStart = uri.indexOf('?');
		queryString = qStart == -1 ? "" : uri.substring(qStart);
		
		cookies = new HashMap<>();
		if (headers.containsKey("cookie")) {
			String[] cookiePairs = headers.get("cookie").split(";");
			for (String cookiePair : cookiePairs) {
				int equalPosition = cookiePair.indexOf("=");
				if (equalPosition == -1) {
					break;
				}
				String key = cookiePair.substring(0, equalPosition);
				String val = cookiePair.substring(equalPosition + 1);
				cookies.put(key, val);
			}
		}
		sessionId = cookies.getOrDefault("id", null);
	}

	@Override
	public Session session() {
		return session(true);
	}

	@Override
	public Session session(boolean create) {
		if (ServiceFactory.getSession(sessionId) == null) {
			if (create) {
				sessionId = ServiceFactory.createSession();
			} else {
				sessionId = null;
			}
		}
		return ServiceFactory.getSession(sessionId);
	}

	@Override
	public Map<String, String> params() {
		return params;
	}

	@Override
	public String queryParams(String param) {
		return qParms.get(param).toString();
	}

	@Override
	public List<String> queryParamsValues(String param) {
		return qParms.get(param);
	}

	@Override
	public Set<String> queryParams() {
		return qParms.keySet();
	}

	@Override
	public String queryString() {
		return queryString;
	}

	@Override
	public void attribute(String attrib, Object val) {
		attrs.put(attrib, val);

	}

	@Override
	public Object attribute(String attrib) {
		return attrs.get(attrib);
	}

	@Override
	public Set<String> attributes() {
		return attrs.keySet();
	}

	@Override
	public Map<String, String> cookies() {
		return cookies;
	}
	
	
	// Old Methods from M1

	@Override
	public String requestMethod() {
		return innerRequest.requestMethod();
	}

	@Override
	public String host() {
		return innerRequest.host();
	}

	@Override
	public String userAgent() {
		return innerRequest.userAgent();
	}

	@Override
	public int port() {
		return innerRequest.port();
	}

	@Override
	public String pathInfo() {
		return innerRequest.pathInfo();
	}

	@Override
	public String url() {
		return innerRequest.url();
	}

	@Override
	public String uri() {
		return innerRequest.uri();
	}

	@Override
	public String protocol() {
		return innerRequest.protocol();
	}

	@Override
	public String contentType() {
		return innerRequest.contentType();
	}

	@Override
	public String ip() {
		return innerRequest.ip();
	}

	@Override
	public String body() {
		return innerRequest.body();
	}

	@Override
	public int contentLength() {
		return innerRequest.contentLength();
	}

	@Override
	public String headers(String name) {
		return innerRequest.headers(name);
	}

	@Override
	public Set<String> headers() {
		return innerRequest.headers();
	}

}
