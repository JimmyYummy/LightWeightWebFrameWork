package edu.upenn.cis.cis455;

import org.apache.logging.log4j.Level;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m2.server.interfaces.Session;
import edu.upenn.cis.cis455.m2.server.interfaces.WebService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static edu.upenn.cis.cis455.WebServiceController.*;

/**
 * 
 * USER GUIDE:
 * 
 * @author Jing Wang
 *
 *
 * 1. How to use the framework: Just like SparkJava!
 *
 *
 *
 * 2. The architecture for the project and mechanics
 * WebServiceController -- WebService -- Context (hold the information for the WebService)
 * 											|
 * 											|
 * 					HandlerResolver	--	HttpServer -- Daemon Threads <-- request ---------- client
 * 							   \		/		\		/ (offer new task to the queue)		|
 * 	(2. find the handler of the \	ThreadPool	TaskQueue									|
 * 	corresponding server socket) \	  | ... |   / (poll from queue)  						|
 * 								  \	  |		|  /											|
 *			HttpRequestHandler ----- HttpWorkers --------------- HttpIOHandler  ---> response / error
 *	(3. handler handler requests) 	(working or waiting)				(4. send response or exception, 
 *									(1. create request and response)	could be chunked or unchunked, 
 *									(1.1 may send a 100 response)		HTTP/1.0 or HTTP/1.1. return if 
 *					
 *
 *
 *													keep a persistent connection)
 * 3. Process inside the HttpRequestHandler
 * 
 *	HttpRequestHandler -- GetRequestHandler ----- corresponding routers
 *		|			   |_ HeadRequestHandler ----		.
 *		Filters		   |_ PostRequestHandler ----		.
 * (before and after,  |_ PutRequestHandler  ----		.
 * general or specific |_ DeleteRequestHandler --		.
 * file type)		   |_ OptionsRequestHandler -		.
 * 
 * 	1) The HttpRequestHandler applies the before filters on the request.
 * 
 * 	2) The HttpRequestHandler find the corresponding sub-handler based on the request's HTTP method.
 * 
 * 	3) The sub-handler (e.g. GetRequestHandler) try to find the proper router of the path.
 * 		Special case: the GET Request will be checked if it was a special URL request first before 
 * 		checking the routers, this will avoid the control panel / shutdown URL being overwritten by route
 *   
 *  4) If the sub-handler failed to find a router to handle the request, it will use the default method
 *     to process the request (e.g. for GET, try to return the file on the path).
 *  	Special case: for the HEAD request, it will try to run the handler as a GET request if there is 
 *  	no HEAD route matches the request. In other words, the GET routes and special URL may also work
 *  	on the HEAD request (but the response will have body content removed), as long as the HEAD 
 *  	request is not caught on a HEAD route.
 *  
 *  5) If even the default method failed, throw a HaltException.
 *  
 *  6) The HttpRequestHandler apply the after filters on the request.
 *  
 *  7) Finish the handling process.
 * 
 * 
 * 
 * 4. = Extra Credit =
 * 
 *  1) Persistent connection and chunked encoding
 * 
 * 	Persistent connection is used by default on HTTP/1.1 requests, unless the request's header contains 
 *  "connection: close". HTTP/1.0 request will always be a transient connection
 *  
 * 	Chunked encoding could be process if there is an header "transfer-encoding: chunked" in the request, 
 * 	or the content's body will be taken care of normally. If there is the request contains a body, but 
 * 	it does not specify the "Content-Length" header, the request will be regarded as having no body, and 
 *  its body will be regarded as another request (wrongly formatted), and a 400 error will be sent back.
 * 
 *  2) Performance testing: Not finished yet
 * 
 *  3) Multiple simultaneous sockets / servers
 * 
 *  To create a new web service on the server, please get a new WebService from the ServiceFactory using
 *  getNewWebService() method, then configure the WebService by calling the methods of the instance of 
 *  the WebService. 
 *  
 *  Be noted that each WebService's port will increment by 1 from 8888 based on the order
 *  of their creation. If the default WebService is created later (based on the lazy initialization, it 
 *  won't be created until any WebServiceController's method is called).
 *  
 *  4) General wildcards in routes: supported already
 * 
*/

public class WebServer {
    public static void main(String[] args) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        // TODO: make sure you parse *BOTH* command line arguments properly
        System.out.println(Arrays.toString(args));
        int port = Integer.parseInt(args[0]);
        String rootPath = args[1];
//        String rootPath = "./www";
//        int port = 8888;
        // TODO: launch your server daemon  
        staticFileLocation(rootPath);
        port(port);
        awaitInitialization();
        
        before((req, res) -> {
        	Path secured = Paths.get("/etc/passwd").normalize();
        	Path reqPath = Paths.get(req.pathInfo()).normalize();
        	if (reqPath.equals(secured)) {
        		throw new HaltException(403, "Access forbidden");
        	}
        });
                
        get("/control", (req, res) -> {
        	Path errorLogPath = Paths.get("error.log");
        	res.type("text/html");
    		StringBuilder sb = new StringBuilder();
    		Map<String, String> infos = ServiceFactory.getHttpServer().getThreadPoolInfo();
    		// start of doc
    		sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Control Panel</title>\n</head>\n"
    				+ "<body>\n<h1>Control Panel</h1>\n<ul>\n");
    		// ThreadPool Monitor
    		sb.append("<li>Thread Pool:\n" + "	<ul>\n");
    		for (Map.Entry<String, String> threadInfo : infos.entrySet()) {
    			sb.append(String.format("<li>%s: %s</li>\n", threadInfo.getKey(), threadInfo.getValue()));
    		}
    		// Shutdown URL
    		sb.append("	</ul>\n" + "</li>\n" + "<li><a href=\"/shutdown\">Shut down</a></li>\n");
    		// Error log
    		File errorLogFile = errorLogPath.toFile();
    		if (errorLogFile.exists() && errorLogFile.isFile() && errorLogFile.canRead()) {
    			BufferedReader reader = null;
    			try {
    				reader = new BufferedReader(new FileReader(errorLogFile));
    				sb.append("<li>Error Log:\n" + "	<ul>\n");
    				String line = null;
    				while ((line = reader.readLine()) != null) {
    					sb.append(line);
    				}
    			} catch (IOException e) {
    				e.printStackTrace();
    			} finally {
    				if (reader != null) reader.close();
    			}
    		}
    		// end of doc
    		sb.append("</ul>\n</body>\n</html>");
    		return sb.toString();
        });
        
        
        get("/shutdown", (req, res) -> {
        	stop();
    		return "The server is shut down.";
        });
        
        get("/", (req, res) -> {
        	res.redirect("/index.html");
        	return "";
        });
        
        get("/session/get", (req, res) -> {
        	Session session = req.session();
        	session.attribute("ip", req.ip());
        	res.cookie("/session", "session-id", session.id());
        	return "session created";
        });
        
        get("/session/check", (req, res) -> {
        	Session session = req.session(false);
        	if (session == null) return "No built session for the client";
        	return "Hello, client ip: " + session.attribute("ip") + " session-id: " + session.id();
        });
        
        get("/outcheck", (req, res) -> {
        	return "No session: " + (req.session(false) == null);
        });
        
        get("/:user/sayhello", (req, res) -> {
        	StringBuilder sb = new StringBuilder();
        	sb.append("Hello, ");
        	sb.append(req.params("user"));
        	sb.append("! Your qureries are:\n");
        	Set<String> queries = req.queryParams();
        	for (String query : queries) {
        		sb.append(String.format("\t%s: %s\n", query, req.queryParams(query)));
        	}
        	return sb.toString();
        });
        
      WebService ws = ServiceFactory.getNewWebService();
      ws.port(9999);
      ws.get("/compute", (req, res) -> {
      	
      	return fibonacciNum(40);
      });
      ws.awaitInitialization();        
        
        
        System.out.println("Waiting to handle requests!");
    }
    
    
    // return the n-th fibonacciNumber, computed through recursion
    private static int fibonacciNum(int n) {
    	if (n == 0 || n == 1) return 1;
    	return fibonacciNum(n - 1) + fibonacciNum(n - 2);
    }

}
