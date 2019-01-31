package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.HttpParsing;

public class BasicRequestHandler implements HttpRequestHandler {
	final static Logger logger = LogManager.getLogger(BasicRequestHandler.class);
			
	private Map<Integer, Context> contexts;

	public BasicRequestHandler() {
		contexts = new HashMap<>();
	}

	@Override
	public void handle(Request request, Response response) throws HaltException {
		if (! contexts.containsKey(request.port())) {
			throw new HaltException(401, "Connection Refused on the port");
		}
		Context localContext = contexts.get(request.port());
		String path = request.pathInfo();
		// check filter here
		boolean processed = false;
		if (processed) {
			throw new HaltException(401, "Unauthorized");
		}
		// chexck special url here
		if (specialURlHandle(localContext, path, request, response)) {
			return;
		}
		// find the route here
		for (String routingPath : localContext.getRegisteredPaths()) {
			if (isMatch(routingPath, path)) {
				try {
					localContext.getRoute(routingPath).handle(request, response);
				} catch (Exception e) {
					logger.error(e);
					throw new HaltException(500, "Internal Error");
				}
				return;
			}
		}
		fileFetchingHandle(request, response, localContext.getFileLocation());
			
	}
	
	private void fileFetchingHandle(Request request, Response response, String pathPrefix) throws HaltException {
		File requestedFile = new File(pathPrefix + request.pathInfo());
		if (! requestedFile.exists()) {
			throw new HaltException(404, "Not Found");
		}
		try {
			byte[] allBytes = Files.readAllBytes(Paths.get(pathPrefix + request.pathInfo()));
			response.bodyRaw(allBytes);
			response.type(HttpParsing.getMimeType(request.pathInfo()));
			response.status(200);
		} catch (IOException e) {
			logger.error(e);
			throw new HaltException(500, "internal Error");
		}
	}
	
	private boolean isMatch(String routePath, String reqPath) {
		return reqPath.startsWith(routePath);
	}
	
	private boolean specialURlHandle(Context context, String path, Request req, Response res) {
		boolean handled = false;
		if ("/shutdown".equals(path)) {
			handled = true;
			context.setUnactive();
		} else if ("/control".equals(path)) {
			handled = true;
			//TODO: implement /control response
		}
		return handled;
	}
	
	/**
	 * 
	 * @param context
	 * @return whether there is a previous mapping for the class path
	 */
	public boolean setContext(int port, Context context) {
		return contexts.put(port, context) == null;
	}
}
