package edu.upenn.cis.cis455;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.interfaces.WebService;
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
    
    /**
     * Create an HTTP request given an incoming socket
     */
    public static Request createRequest(Socket socket,
                         String uri,
                         boolean keepAlive,
                         Map<String, String> headers,
                         Map<String, List<String>> parms) {
    	BasicRequest req = BasicRequest.getBasicRequestExceptBody(uri, headers, parms);
        try {
        	req.addBody(socket.getInputStream());
			req.persistentConnection(keepAlive);
        } catch (HaltException | IOException e) {
			logger.error("Error on Creating new Request: " + e.getMessage());
		}
        return req;
    }
    
    public static Request getRequestForException() {
    	return BasicRequest.getRequestForException();
    }
    
    /**
     * Gets a request handler for files (i.e., static content) or dynamic content
     */
    public static HttpRequestHandler createRequestHandlerInstance(Path serverRoot) {
        return new GeneralRequestHandler(serverRoot);
    }
    
	public static BasicRequestHandler createReqeustHandler(HttpMethod method, Context context, HttpServer server) {
		switch (method) {
		case GET:
			return new GetRequestHandler(context, server);
		case HEAD:
			return new HeadRequestHandler(context, server);
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
        return new BasicResponse();
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
        
        return null;
    }

	public static HandlerResolver getHandlerResolver() {
		if (hr == null) {
			hr = new HandlerResolver();
		}
		return hr;
	}
	
	public static HttpThreadPool getNewThreadPool(HttpServer server, int num) {
		return new HttpThreadPool(server, num);
	}

	public static HttpServer getHttpServer() {
		if (hs == null) {
			hs = new HttpServer();
		}
		return hs;
	}
}