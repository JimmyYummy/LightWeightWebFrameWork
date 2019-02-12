package edu.upenn.cis.cis455.methodHandlers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
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
			if (PathUtil.checkPathMatch(routePath, requestPath, request)) {
				try {
					logger.info("reqeust " + request + " caught on path: " + routePath);
					Object body = routes.get(routePath).handle(request, response);
					if (body == null) body = "";
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
	
	protected void modificationHeaderCheck(Request request, File requestedFile, Path requestPath) {
		if (request.headers().contains("if-modified-since")) {

			ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-modified-since"));
			if (reqDate != null && reqDate.toInstant().toEpochMilli() < requestedFile.lastModified()) {
				throw new HaltException(304, "Not Modified " + requestPath);
			}
		}

		if (request.headers().contains("if-unmodified-since")) {

			ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-unmodified-since"));
			if (reqDate != null && reqDate.toInstant().toEpochMilli() > requestedFile.lastModified()) {
				throw new HaltException(412, "Precondition Failed " + requestPath);
			}
		}
	}

}
