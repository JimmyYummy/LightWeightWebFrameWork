package edu.upenn.cis.cis455.m1.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m1.server.implementations.BasicResponse;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
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

	public HttpWorker(HttpServer server) {
		this.server = server;
		this.keepActive = true;
		this.isWorking = false;
	}

	@Override
	public void run() {
		while (keepActive) {
			HttpTask task = server.getRequestQueue().poll();
			this.isWorking = true;
			work(task.getSocket());
			this.isWorking = false;
		}
		logger.info("" + this + " is turned off");
	}

	public void turnOffWorker() {
		this.keepActive = false;
	}

	public void work(Socket sc) {
		// get the input stream of the socket
		InputStream in = null;
		try {
			in = new BufferedInputStream(sc.getInputStream());
		} catch (IOException e) {
			logger.error(e);
		}
		// do the loop processing, assuming persistent connections
		while (keepActive) {
			BasicRequest req = null;
			Response res = null;
			try {
				// generate the request
				logger.info("parsing the inital line and headers one socket: " + sc);
				Map<String, String> headers = new HashMap<>();
				Map<String, List<String>> parms = new HashMap<>();
				String clientAddr = sc.getInetAddress().toString();
				InputUtil.skipBlankLines(in);
				String uri = HttpParsing.parseRequest(clientAddr, in, headers, parms);
				if (headers.containsKey("user-agent")) {
					headers.put("useragent", headers.get("user-agent"));
				}
				logger.info("Accepting request for " + uri + " from " + clientAddr + "\nwith header: " + headers);
				// generate the request object, take care of chucked request
				req = BasicRequest.getBasicRequestExceptBody(uri, headers, parms);
				logger.info("Get request without body from: " + clientAddr);
				req.addBody(in);
				logger.info("Get reqesut with body: " + req);
				// send an 100 response
				if (req.headers("protocolVersion").equals("HTTP/1.1")) {
					logger.info("sending 100 response");
					HttpIoHandler.sendContinueResponse(sc);
				}
				// find the proper router
				HttpRequestHandler handler = null;
				if ((handler = server.getHandlerResolver().getHandler(req.port())) == null) {
					throw new HaltException(401, "Connection Refused on the port");
				}
				// use handler to generate the response
				res = new BasicResponse();
				handler.handle(req, res);
				// use IO handler to send response
				// persistent? (based on the handler's response) and the input stream
				if (!HttpIoHandler.sendResponse(sc, req, res) || InputUtil.reachedEndOfStream(in)) {
					sc.close();
					logger.info("closed connection: " + sc);
					return;
				}
			} catch (HaltException e) {
//				logger.error("" + e + e.statusCode() + e.body());
//				// return error response if error occurs
//				// persistent? (based on the handler's response)
//				if (req == null)
//					req = BasicRequest.getRequestForException();
//				if (!HttpIoHandler.sendException(sc, req, e)) {
//					logger.info("closed connection: " + sc);
//					try {
//						sc.close();
//					} catch (IOException e1) {
//						logger.error(e);
//					}
//					return;
//				}
				throw e;
			} catch (Exception e) {
				logger.error(e);
			}
		}
		// close connection and return
		try {
			sc.close();
		} catch (IOException e) {
			logger.error(e);
		}
		logger.info("closed connection: " + sc);
		return;
	}

	public boolean isWorking() {
		return isWorking;
	}
}
