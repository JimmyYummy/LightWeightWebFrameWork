package edu.upenn.cis.cis455.m1.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.implementations.GeneralRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;

public class HandlerResolver {
	
	final static Logger logger = LogManager.getLogger(HandlerResolver.class);
	
	private Map<Integer, HttpRequestHandler> handlerMap;
	
	public HandlerResolver() {
		handlerMap = new HashMap<>();
	}

	protected void addHandler(Context context, HttpServer server) {
		if (handlerMap.containsKey(context.getPort())) {
			throw new IllegalArgumentException("Port already in use");
		}
		handlerMap.put(context.getPort(), new GeneralRequestHandler(context, server));
		logger.info("new handler added for new application on port " + context.getPort());
	}
	
	/**
	 * return the request handle on the port if exists,
	 * else return null
	 */
	public HttpRequestHandler getHandler(int port) {
		return handlerMap.getOrDefault(port, null);
	}
}