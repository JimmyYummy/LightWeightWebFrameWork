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
			server.error(this);
			throw e;
		}

	}

	public void turnOffWorker() {
		this.keepActive = false;
	}

	public void work(HttpTask task) {
		// get the input stream of the socket
		InputStream in = null;
		Socket sc = task.getSocket();
		try {
			in = new BufferedInputStream(sc.getInputStream());
		} catch (IOException e) {
			logger.catching(e);
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
				headers.put("port", String.valueOf(task.getPort()));
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
				InputUtil.skipBlankLines(in);
				if (!HttpIoHandler.sendResponse(sc, req, res) || InputUtil.reachedEndOfStream(in)) {
					sc.close();
					logger.info("closed connection: " + sc);
					return;
				}
			} catch (HaltException e) {
				logger.catching(e);
				// return error response if error occurs
				// persistent? (based on the handler's response)
				if (req == null)
					req = BasicRequest.getRequestForException();
				if (!HttpIoHandler.sendException(sc, req, e)) {
					logger.info("closed connection: " + sc);
					try {
						sc.close();
					} catch (IOException e1) {
						logger.catching(e);
					}
					return;
				}
//				throw e;
			} catch (Exception e) {
				logger.catching(e);
				e.printStackTrace();
			}
		}
		// close connection and return
		try {
			sc.close();
		} catch (IOException e) {
			logger.catching(e);
		}
		logger.info("closed connection: " + sc);
		return;
	}

	public boolean isWorking() {
		return isWorking;
	}
}
