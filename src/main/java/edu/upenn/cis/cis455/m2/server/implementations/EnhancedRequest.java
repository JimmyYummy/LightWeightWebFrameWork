package edu.upenn.cis.cis455.m2.server.implementations;

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
	private String sessionId;

	private EnhancedRequest() {};
	
	public static EnhancedRequest getBasicRequestExceptBody(String url, Map<String, String> headers,
            Map<String, List<String>> params) {
		EnhancedRequest req = new EnhancedRequest();
		return null;
	}
	
	@Override
	public Session session() {
		return session(true);
	}
	
	@Override
	public Session session(boolean create) {
		if (ServiceFactory.getSession(sessionId) == null)  {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String queryParams(String param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> queryParamsValues(String param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> queryParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String queryString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void attribute(String attrib, Object val) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object attribute(String attrib) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> attributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> cookies() {
		// TODO Auto-generated method stub
		return null;
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

}
