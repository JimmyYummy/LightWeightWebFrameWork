package edu.upenn.cis.cis455;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.server.interfaces.WebService;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HandlerResolver;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.HttpThreadPool;
import edu.upenn.cis.cis455.m1.server.implementations.Application;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m1.server.implementations.BasicResponse;
import edu.upenn.cis.cis455.m1.server.implementations.GeneralRequestHandler;
import edu.upenn.cis.cis455.m1.server.implementations.SingleAppWebService;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.m2.server.implementations.BasicCookie;
import edu.upenn.cis.cis455.m2.server.interfaces.Session;
import edu.upenn.cis.cis455.methodHandlers.BasicRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.DeleteRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.GetRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.HeadRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.OptionsRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.PostRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.PutRequestHandler;


public class ServiceFactory {
	
	final static Logger logger = LogManager.getLogger(ServiceFactory.class);
	
	private static WebService ws;
	private static HandlerResolver hr;
	private static HttpServer hs;
	private static HttpThreadPool pool;
	private static Request exceptionRequest0;
	private static Request exceptionRequest1;
	private static Map<String, String> cookieToSession;
	
    /**
     * Get the HTTP server associated with port 8080
     */
    public static WebService getServerInstance() {
    	if (ws == null) {
    		ws = new SingleAppWebService();
    	}
        return ws;
    }
    
    public static Application getNewApplication() {
    	return new Application();
    }
    
    public static WebService getNewWebService() {
    	return new SingleAppWebService();
    }
    
    /**
     * Create an HTTP request given an incoming socket
     */
    public static Request createRequest(Socket socket,
            String uri,
            boolean keepAlive,
            Map<String, String> headers,
            Map<String, List<String>> parms) {
    	return createBasicRequest(socket, uri, keepAlive, headers, parms);
    }
    
    
    public static BasicRequest createBasicRequest(Socket socket,
                         String uri,
                         boolean keepAlive,
                         Map<String, String> headers,
                         Map<String, List<String>> parms) {
    	
    	if (headers.containsKey("user-agent")) {
			headers.put("useragent", headers.get("user-agent"));
		}
    	BasicRequest req = BasicRequest.getBasicRequestExceptBody(uri, headers, parms);
        try {
        	req.addBody(new BufferedInputStream(socket.getInputStream()));
			req.persistentConnection(keepAlive);
        } catch (HaltException | IOException e) {
			logger.error("Error on Creating new Request: " + e.getMessage());
		}
        return req;
    }
    
    /**
     * Gets a request handler for files (i.e., static content) or dynamic content
     */
    public static HttpRequestHandler createRequestHandlerInstance(Path serverRoot) {
        return new GeneralRequestHandler(serverRoot);
    }
    
	public static BasicRequestHandler createReqeustHandler(HttpMethod method, Context context) {
		switch (method) {
		case GET:
			return new GetRequestHandler(context, hs);
		case HEAD:
			return new HeadRequestHandler(context, hs);
		case POST:
			return new PostRequestHandler(context);
		case PUT:
			return new PutRequestHandler(context);
		case DELETE:
			return new DeleteRequestHandler(context);
		case OPTIONS:
			return new OptionsRequestHandler(context);
		default:
			return new BasicRequestHandler();
		}

	}

    /**
     * Gets a new HTTP Response object
     */
    public static Response createResponse() {
        return createBasicResponse();
    }
    
	public static BasicResponse createBasicResponse() {
		// TODO Auto-generated method stub
		return new BasicResponse();
	}
    
	public static HandlerResolver getHandlerResolver() {
		if (hr == null) {
			hr = new HandlerResolver();
		}
		return hr;
	}
	
	public static HttpThreadPool getThreadPool(int num) {
		if (pool == null) {
			if (hs == null) {
				throw new IllegalStateException("Cannot create the thread poll without server initialized");
			}
			pool = new HttpThreadPool(num);
		}
		return pool;
	}

	public static HttpServer getHttpServer() {
		if (hs == null) {
			hs = new HttpServer();
		}
		return hs;
	}
	
	public static boolean webServiceCreated() {
		return ! (ws == null);
	}

    /**
     * Creates a blank session ID and registers a Session object for the request
     */
    public static String createSession() {
        return null;
    }
    
    /**
     * Looks up a session by ID and updates / returns it
     */
    public static Session getSession(String id) {
        if (id == null) return null;
        
        return null;
    }
    
    public static Request getRequestForException(int version) {
    	if (version == 1) {
    		return getRequestForException1();
    	}
		return getRequestForException0();

    }

	private static Request getRequestForException0() {
		if (exceptionRequest0 == null) {
			Map<String, String> headers = new HashMap<>();
			headers.put("protocolVersion", "HTTP/1.0");
			headers.put("Method", "GET");
			Map<String, List<String>> params = new HashMap<>();
			String url = "";
			exceptionRequest0 = BasicRequest.getBasicRequestExceptBody(url, headers, params);
    	}
    	return exceptionRequest0;
	}

	private static Request getRequestForException1() {
		if (exceptionRequest1 == null) {
			Map<String, String> headers = new HashMap<>();
			headers.put("protocolVersion", "HTTP/1.1");
			headers.put("connection", "close");
			headers.put("host", "This Host");
			headers.put("Method", "GET");
			Map<String, List<String>> params = new HashMap<>();
			String url = "";
			exceptionRequest1 = BasicRequest.getBasicRequestExceptBody(url, headers, params);
    	}
    	return exceptionRequest1;
	}

	public static String cookieToSessionId(String cookie) {
		return cookieToSession.get(cookie);
	}
	
	public static BasicCookie createCookie(String path, String name, String value, 
			int maxAge, boolean secured, boolean httpOnly) {
		return new BasicCookie(path, name, value, maxAge, secured, httpOnly);
	}
	
	public static BasicCookie createMockCookie(String path, String name) {
		return new BasicCookie(path, name, "", -1, false, false);
	}
}