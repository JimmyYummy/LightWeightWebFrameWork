package edu.upenn.cis.cis455.methodHandlers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.PathUtil;

public class BasicRequestHandler {
	protected final static Logger logger = LogManager.getLogger(BasicRequestHandler.class);
	
	private Map<Path, Route> routes;

	// create a dummy handler
	public BasicRequestHandler() {
		routes = new HashMap<>();
	}

	public void addRoutes(Map<Path, Route> routeMap) {
		routes = routeMap;
	}

	public boolean handle(Request request, Response response) throws HaltException {
		return routerHandle(request, response);
	}

	protected final boolean routerHandle(Request request, Response response) throws HaltException {
		// get the path of the request
		Path requestPath = Paths.get(request.pathInfo()).normalize();
		logger.info(request.requestMethod() + " checking routes: " + requestPath);
		// find the route here
		Path[] paths = routes.keySet().toArray(new Path[0]);
		Arrays.sort(paths, Comparator.reverseOrder());
		for (Path routePath : paths) {
			if (PathUtil.checkPathMatch(routePath, requestPath)) {
				try {
					logger.info("reqeust " + request + " caught on path: " + routePath);
					Object body = routes.get(routePath).handle(request, response);
					response.body(body.toString());

				} catch (Exception e) {
					logger.error("Error caught: Exception on Router Handling - " + e.getMessage());
					throw new HaltException(500, "Error while handling the request. " + requestPath);
				}
				return true;
			}
		}
		return false;
	}

}
