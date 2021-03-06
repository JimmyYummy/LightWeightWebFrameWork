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

import edu.upenn.cis.cis455.m2.server.interfaces.*;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HandlerResolver;
import edu.upenn.cis.cis455.m1.server.HttpIoHandler;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.HttpThreadPool;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m1.server.implementations.BasicResponse;
import edu.upenn.cis.cis455.m1.server.implementations.GeneralRequestHandler;
import edu.upenn.cis.cis455.m1.server.implementations.SingleAppWebService;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m2.server.implementations.BasicCookie;
import edu.upenn.cis.cis455.m2.server.implementations.BasicSession;
import edu.upenn.cis.cis455.m2.server.implementations.EnhancedRequest;
import edu.upenn.cis.cis455.m2.server.implementations.EnhancedResponse;
import edu.upenn.cis.cis455.methodHandlers.BasicRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.DeleteRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.GetRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.HeadRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.OptionsRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.PostRequestHandler;
import edu.upenn.cis.cis455.methodHandlers.PutRequestHandler;


public class ServiceFactory {
	
	final static Logger logger = LogManager.getLogger(ServiceFactory.class);
	
	private static volatile WebService ws;
	private static volatile HandlerResolver hr;
	private static volatile HttpServer hs;
	private static volatile HttpThreadPool pool;
	private static volatile Request exceptionRequest0;
	private static volatile Request exceptionRequest1;
	private static volatile Map<String, Session> idToSession = new HashMap<>();
	
    /**
     * Get the HTTP server associated with port 8080
     */
    public static WebService getServerInstance() {
    	if (ws == null) {
		synchronized (SingleAppWebService.class) {
    			logger.info("creating the singlton web service instance");
    			ws = new SingleAppWebService();
		}
    	}
        return ws;
    }
    
    public static WebService getNewWebService() {
    	logger.info("creating the repeatable web service instance");
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
    	return createEnhancedRequest(socket, uri, keepAlive, headers, parms);
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
    	// send an 100 response
		if (req.headers("protocolVersion").equals("HTTP/1.1")) {
			logger.info("sending 100 response");
			HttpIoHandler.sendContinueResponse(socket);
		}
        try {
        	req.addBody(new BufferedInputStream(socket.getInputStream()));
			req.persistentConnection(keepAlive);
        } catch (HaltException | IOException e) {
			logger.error("Error on Creating new Request: " + e.getMessage());
		}
        return req;
    }
    
    public static EnhancedRequest createEnhancedRequest(Socket socket,
            String uri,
            boolean keepAlive,
            Map<String, String> headers,
            Map<String, List<String>> parms) {
    	return new EnhancedRequest(socket, uri, keepAlive, headers, parms);
    }
    
    /**
     * Gets a new HTTP Response object
     */
    public static Response createResponse() {
        return createEnhancedResponse();
    }
    
	public static BasicResponse createBasicResponse() {
		return new BasicResponse();
	}
    
	public static EnhancedResponse createEnhancedResponse() {
		return new EnhancedResponse();
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
			return new GetRequestHandler(context);
		case HEAD:
			return new HeadRequestHandler(context);
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
    
	public synchronized static HandlerResolver getHandlerResolver() {
		if (hr == null) {
			logger.info("creating the request handler resolver instance");
			hr = new HandlerResolver();
		}
		return hr;
	}
	
	public synchronized static HttpThreadPool getThreadPool(int num) {
		if (pool == null) {
			if (hs == null) {
				throw new IllegalStateException("Cannot create the thread poll without server initialized");
			}
			logger.info("creating the thread pool instance");
			pool = new HttpThreadPool(num);
		}
		return pool;
	}

	public synchronized static HttpServer getHttpServer() {
		if (hs == null) {
			logger.info("creating the HttpServer instance to run all apps");
			hs = new HttpServer();
		}
		return hs;
	}
	
	public synchronized static boolean webServiceCreated() {
		return ! (ws == null);
	}

    /**
     * Creates a blank session ID and registers a Session object for the request
     */
    public static String createSession() {
    	Session session = new BasicSession();
    	idToSession.put(session.id(), session);
    	logger.info("new session in the pool, id: " + session.id());
        return session.id();
    }
    
    /**
     * Looks up a session by ID and updates / returns it
     */
    public static Session getSession(String id) {
        if (id == null) return null;
        Session session = idToSession.getOrDefault(id, null);
        if (session == null) return null;
        session.access();
        return idToSession.getOrDefault(id, null);
    }
    
    public static void deregisterSession(String id) {
    	if (idToSession.remove(id) != null) {
    		logger.info("session removed from pool, id: " + id);
    	} else {
    		logger.info("pool does not contain this session, id: "+ id);
    	}
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
			BasicRequest breq = BasicRequest.getBasicRequestExceptBody(url, headers, params);
			exceptionRequest0 = EnhancedRequest.wrapBasicRequest(breq);
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
			BasicRequest breq = BasicRequest.getBasicRequestExceptBody(url, headers, params);
			exceptionRequest1 = EnhancedRequest.wrapBasicRequest(breq);
    	}
    	return exceptionRequest1;
	}
	
	public static BasicCookie createCookie(String path, String name, String value, 
			int maxAge, boolean secured, boolean httpOnly) {
		return new BasicCookie(path, name, value, maxAge, secured, httpOnly);
	}
	
	public static BasicCookie createMockCookie(String path, String name) {
		return new BasicCookie(path, name, "", -1, false, false);
	}
}
