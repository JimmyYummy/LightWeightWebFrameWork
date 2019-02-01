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
			Request req = null;
			Response res = null;
			try {

				// generate the request
				try {
					Map<String, String> headers = new HashMap<>();
					Map<String, List<String>> parms = new HashMap<>();
					String clientAddr = sc.getInetAddress().toString();
					String uri = HttpParsing.parseRequest(clientAddr, in, headers, parms);
					logger.info("Accepting request for " + uri + " from " + clientAddr + "with header" + headers);
					// generate the request object, take care of chucked request
					req = BasicRequest.BasicRequestFactory.getBasicRequest(uri, in, headers, parms);
				} catch (Exception e) {
					throw new HaltException(400, "failed to parse the request");
				}
				// send an 100 response
				if (req.headers("protocolVersion").equals("HTTP/1.1")) {
					HttpIoHandler.sendResponse(sc, req, BasicResponse.get100Response());
				}
				// find the proper router
				HttpRequestHandler handler = null;
				if ((handler = server.getHandlerResolver().getHandler(req.port())) == null) {
					throw new HaltException(401, "Connection Refused on the port");
				}
				// use handler to generate the response
				handler.handle(req, new BasicResponse());
				// use IO handler to send response
				// persistent? (based on the handler's response)
				if (!HttpIoHandler.sendResponse(sc, req, res))
					return;
			} catch (HaltException e) {
				logger.error(e);
				// return error response if error occurs
				// persistent? (based on the handler's response)
				if (!HttpIoHandler.sendException(sc, req, e))
					return;
			} catch (Exception e) {
				logger.error(e);
				throw e;
			}
		}
	}
}
