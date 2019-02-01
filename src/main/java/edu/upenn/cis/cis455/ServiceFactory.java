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
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequestHandler;
import edu.upenn.cis.cis455.m1.server.implementations.BasicResponse;
import edu.upenn.cis.cis455.m1.server.implementations.SingleAppWebService;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.m2.server.interfaces.Session;


public class ServiceFactory {
	
	final static Logger logger = LogManager.getLogger(ServiceFactory.class);
	
	private static WebService ws;
    /**
     * Get the HTTP server associated with port 8080
     */
    public static WebService getServerInstance() {
    	if (ws == null) {
    		ws = new SingleAppWebService();
    	}
        return ws;
    }
    
    /**
     * Create an HTTP request given an incoming socket
     */
    public static Request createRequest(Socket socket,
                         String uri,
                         boolean keepAlive,
                         Map<String, String> headers,
                         Map<String, List<String>> parms) {
        try {
			Request req =  BasicRequest.BasicRequestFactory.getBasicRequest(uri, socket.getInputStream(), headers, parms);
			req.persistentConnection(keepAlive);
        } catch (HaltException | IOException e) {
			logger.error(e);
		}
        return null;
    }
    
    /**
     * Gets a request handler for files (i.e., static content) or dynamic content
     */
    public static HttpRequestHandler createRequestHandlerInstance(Path serverRoot) {
        return new BasicRequestHandler(serverRoot);
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
}