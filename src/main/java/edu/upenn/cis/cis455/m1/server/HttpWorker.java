package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpServer.HttpThreadPool;
import edu.upenn.cis.cis455.m1.server.implementations.BasicRequest;
import edu.upenn.cis.cis455.m1.server.implementations.BasicResposne;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.HttpParsing;

/**
 * Stub class for a thread worker for
 * handling Web requests
 */
public class HttpWorker extends Thread {
    final static Logger logger = LogManager.getLogger(HttpWorker.class);
    private HttpThreadPool pool;
    private boolean keepActive;
    
    public HttpWorker(HttpThreadPool pool) {
    	this.pool = pool;
    	this.keepActive = false;
    }
    
	@Override
	public void run() {
		while (keepActive) {
			HttpTask task = pool.getTaskQueue().poll();
			work(task.getSocket());
		}
	}
	
	public void turnOffWorker() {
		this.keepActive = false;
	}

	public void work(Socket sc) {
		// get the header of the request
		Request req = null;
		Response res = null;
		try {
			InputStream in = sc.getInputStream();
			String clientAddr = sc.getInetAddress().toString();
			while (true) {
				try {
					Map<String, String> headers = new HashMap<>();
					Map<String, List<String>> parms = new HashMap<>();
					String uri = HttpParsing.parseRequest(clientAddr, in, headers, parms);
					logger.info("Accepting request for " + uri + " from " + clientAddr + "with header" + headers);
					//TODO: send an 100 response
					if (headers.get("protocolVersion").equals("HTTP/1.1")) {
						HttpIoHandler.sendResponse(sc, req, BasicResposne.get100Response());
					}
					//TODO: generate the request object, take care of chucked request
					
					//TODO: handle the request and response
					
					// check if persistent connection
					if (headers.get("protocolVersion").equals("HTTP/1.0")
							|| parms.containsKey("connection") && parms.get("connection").contains("close")) {
						break;
					}
				} catch (HaltException e) {
					if (req != null) {
						HttpIoHandler.sendException(sc, req, e);
					}
				}
			}	
		} catch (Exception e) {
			logger.error(e);
		} finally {
			if (sc != null) {
				try {
					sc.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}
}
