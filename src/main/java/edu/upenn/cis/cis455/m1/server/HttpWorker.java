package edu.upenn.cis.cis455.m1.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.HttpParsing;
import edu.upenn.cis.cis455.util.InputUtil;

/**
 * Stub class for a thread worker for handling Web requests
 */
public class HttpWorker extends Thread {
	final static Logger logger = LogManager.getLogger(HttpWorker.class);

	private HttpServer server;
	private boolean keepActive;
	private boolean isWorking;

	public HttpWorker() {
		this.server = ServiceFactory.getHttpServer();
		this.keepActive = true;
		this.isWorking = false;
	}

	@Override
	public void run() {
		try {
			while (keepActive) {
				HttpTask task = server.getRequestQueue().poll();
				if (task == null)
					continue;
				this.isWorking = true;
				work(task);
				this.isWorking = false;
			}
			logger.info("" + this + " is turned off");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Uncaught error: " + e.getMessage());
		}

	}

	public void turnOffWorker() {
		this.keepActive = false;
	}

	public void work(HttpTask task) {
		// get the input stream of the socket
		InputStream in = null;
		if (task == null) {
			logger.error("Unexcepted null task");
		}
		Socket sc = task.getSocket();
		if (sc == null) {
			logger.error("Unexpected null socket");
			return;
		}
		try {
			in = new BufferedInputStream(sc.getInputStream());
		} catch (IOException e) {
			logger.error("Error caught: IOException on Get Socket's Inputstream / create BufferedInputStream - " + e.getMessage());
		}
		// do the loop processing, assuming persistent connections
		while (keepActive) {
			Request req = null;
			Response res = null;
			int version = 0;
			try {
				// generate the request
				logger.info("parsing the inital line and headers one socket: " + sc);
				Map<String, String> headers = new HashMap<>();
				Map<String, List<String>> parms = new HashMap<>();
				String clientAddr = sc.getInetAddress().toString();
				InputUtil.skipBlankLines(in);
				String uri = HttpParsing.parseRequest(clientAddr, in, headers, parms);
				
				headers.put("port", String.valueOf(task.getPort()));
				logger.info("Accepting request for " + uri + " from " + clientAddr + "\nwith header: " + headers);
				
				// get the version of the default request for exception
				String protocolVersion = headers.get("protocolVersion").split(";")[0].trim().toUpperCase();
				if (protocolVersion.endsWith("1")) {
					version = 1;
				}
				
				// generate the request object, take care of chucked request
				req = ServiceFactory.createRequest(sc, uri, false, headers, parms);
				// check if the PUT/POST request has a proper body
				if ("POST".equals(req.requestMethod()) || "PUT".equals(req.requestMethod())) {
					if (req.contentLength() == 0) {
						throw new HaltException(400, "No content for the " + req.requestMethod() + " request");
					}
				}
				// check if is persistent connection
				boolean persist = false;
				if (req.protocol().equals("HTTP/1.1")
					&& ! "close".equals(req.headers("connection"))){
					persist = true;
				}
				req.persistentConnection(persist);
				// find the proper router
				HttpRequestHandler handler = null;
				if ((handler = server.getHandlerResolver().getHandler(req.port())) == null) {
					throw new HaltException(401, "Connection Refused on the port " + sc.getPort());
				}
				// use handler to generate the response
				res = ServiceFactory.createResponse();
				handler.handle(req, res);
				// use IO handler to send response
				// persistent? (based on the handler's response) and the input stream
				if (!HttpIoHandler.sendResponse(sc, req, res) || InputUtil.reachedEndOfStream(in)) {
					try {
						sc.close();
					} catch (IOException e1) {
						logger.error("Error caught: IOException on Closing Socket after Responding - " + e1.getMessage());
					}
					logger.info("closed connection: " + sc);
					return;
				}
			} catch (HaltException e) {
				logger.error("Error caught: Port " + sc.getPort() + " HaltException on Request Processing - " 
							+ e.statusCode() + " " + e.body() + " " + e.getMessage());
				// return error response if error occurs
				// persistent? (based on the handler's response)
				if (req == null)
					req = ServiceFactory.getRequestForException(version);
				if (!HttpIoHandler.sendException(sc, req, e)) {
					logger.info("closed connection: " + sc);
					try {
						sc.close();
					} catch (IOException e1) {
						logger.error("Error caught: IOException on Closing Socket after Exception - " + e1.getMessage());
					}
					return;
				}
//				throw e;
			} catch (Exception e) {
				logger.error("Error caught: Unexception Exception on Task Working - " + e.getMessage());
				e.printStackTrace();
				HaltException he = new HaltException(500, e.getMessage());
				if (req == null) {
					req = ServiceFactory.getRequestForException(version);
				}
				
				if (!HttpIoHandler.sendException(sc, req, he)) {
					logger.info("closed connection: " + sc);
					try {
						sc.close();
					} catch (IOException e1) {
						logger.error("Error caught: IOException on Closing Socket after Exception - " + e1.getMessage());
					}
					return;
				}
			}
		}
		// close connection and return
		try {
			sc.close();
		} catch (IOException e) {
			logger.error("Error caught: IOException on Closing Socket after Finishing Task - " + e.getMessage());
		}
		logger.info("closed connection: " + sc);
		return;
	}

	public boolean isWorking() {
		return isWorking;
	}
}
