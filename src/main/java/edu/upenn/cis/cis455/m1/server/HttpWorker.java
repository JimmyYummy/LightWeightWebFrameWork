package edu.upenn.cis.cis455.m1.server;

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
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.HttpParsing;

/**
 * Stub class for a thread worker for handling Web requests
 */
public class HttpWorker extends Thread {
	final static Logger logger = LogManager.getLogger(HttpWorker.class);

	private HttpServer server;
	private boolean keepActive;

	public HttpWorker(HttpServer server) {
		this.server = server;
		this.keepActive = true;
	}

	@Override
	public void run() {
		while (keepActive) {
			HttpTask task = server.getRequestQueue().poll();
			work(task.getSocket());
		}
	}

	public void turnOffWorker() {
		this.keepActive = false;
	}

	public void work(Socket sc) {
		// get the input stream of the socket
		InputStream in = null;
		try {
			in = sc.getInputStream();
		} catch (IOException e) {
			logger.error(e);
		}
		// do the loop processing, assuming persistent connections
		while (true) {
			BasicRequest req = null;
			Response res = null;
			try {

				// generate the request
				Map<String, String> headers = new HashMap<>();
				Map<String, List<String>> parms = new HashMap<>();
				String clientAddr = sc.getInetAddress().toString();
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
					HttpIoHandler.sendResponse(sc, req, BasicResponse.get100Response());
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
				// persistent? (based on the handler's response)
				if (!HttpIoHandler.sendResponse(sc, req, res))
					return;
			} 
//			catch (HaltException e) {
//				logger.error("" + e + e.statusCode() + e.body());
//				// return error response if error occurs
//				// persistent? (based on the handler's response)
//				if (req == null ) req = BasicRequest.getRequestForException();
//				if (!HttpIoHandler.sendException(sc, req, e))
//					return;
//			} 
		catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
			return;
		}
	}
}
