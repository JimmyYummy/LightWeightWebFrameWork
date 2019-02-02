package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.HttpParsing;

public class GeneralRequestHandler implements HttpRequestHandler {
	final static Logger logger = LogManager.getLogger(GeneralRequestHandler.class);

	private Path rootPath;
	private Map<HttpMethod, BasicRequestHandler> methodHandlerMap;
	private Map<Path, Filter> filters;
	private Context context;
	private HttpServer server;
	private static Path shutdown = Paths.get("/shutdown").normalize();
	private static Path control = Paths.get("/control").normalize();

	public GeneralRequestHandler(Path rootPath) {
		this.rootPath = rootPath;
		methodHandlerMap = createHandlerMap();
	}

	public GeneralRequestHandler(Context context, HttpServer server) {
		rootPath = Paths.get(context.getFileLocation()).normalize();

		// create sub-handers and feed in the routes
		methodHandlerMap = createHandlerMap();
		Map<HttpMethod, Map<Path, Route>> routes = context.getRoutes();
		for (Map.Entry<HttpMethod, Map<Path, Route>> ent : routes.entrySet()) {
			HttpMethod method = ent.getKey();
			Map<Path, Route> routeMap = ent.getValue();
			methodHandlerMap.get(method).addRoutes(routeMap);
		}
		// do the filter mapping
		filters = context.getFilters();

		this.context = context;
		this.server = server;
	}

	private Map<HttpMethod, BasicRequestHandler> createHandlerMap() {
		Map<HttpMethod, BasicRequestHandler> m = new HashMap<>();
		for (HttpMethod method : HttpMethod.values()) {
			m.put(method, createReqeustHandler(method));
		}
		return m;
	}

	private BasicRequestHandler createReqeustHandler(HttpMethod method) {
		switch (method) {
		case GET:
			return this.new GetRequestHandler();
		case HEAD:
			return this.new HeadRequestHandler();
		// TODO: add more methods
		default:
			return this.new BasicRequestHandler();
		}

	}

	@Override
	public void handle(Request request, Response response) throws HaltException {
		// get the path of the request
		Path requestPath = Paths.get(request.pathInfo()).normalize();
		logger.info("handling request path: " + requestPath);
		// check before filter here
		logger.info("checking filter");
		for (Map.Entry<Path, Filter> filterPair : filters.entrySet()) {
			if (requestPath.startsWith(filterPair.getKey())) {
				try {
					filterPair.getValue().handle(request, response);
				} catch (Exception e) {
					logger.error(e);
					throw new HaltException(500, "Error while handling the request.");
				}
			}
		}
		if (response.status() != 200) {
			throw new HaltException(401, "Unauthorized");
		}
		logger.info("dispatch to handler: " + request.requestMethod());
		// dispatch the request to corresponding handlers
		HttpMethod method = Enum.valueOf(HttpMethod.class, request.requestMethod());
		boolean handled = methodHandlerMap.get(method).handle(request, response);
		if (!handled) {
			throw new HaltException(404, "Not Found");
		}
		// TODO: check after filter here
		
		// 
		if (response.type() == null && response.body().length() != 0) {
			response.type("text/plain");
		}
	}

	private class BasicRequestHandler {
		private Map<Path, Route> routes;

		// create a dummy handler
		private BasicRequestHandler() {
			routes = new HashMap<>(0);
		}

		private void addRoutes(Map<Path, Route> routeMap) {
			routes = routeMap;
		}

		protected boolean handle(Request request, Response response) throws HaltException {
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
				if (requestPath.equals(routePath)) {
					try {
						logger.info("reqeust " + request + " caught on path: " + routePath);
						routes.get(routePath).handle(request, response);

					} catch (Exception e) {
						logger.error(e);
						throw new HaltException(500, "Error while handling the request.");
					}
					return true;
				}
			}
			return false;
		}

	}

	private class GetRequestHandler extends BasicRequestHandler {

		@Override
		protected boolean handle(Request request, Response response) throws HaltException {
			// get the path of the request
			Path requestPath = Paths.get(request.pathInfo()).normalize();
			// check special URL here
			if (specialURlHandle(requestPath, request, response)) {
				logger.info("reqeust " + request + " caught on special URL");
				return true;
			}
			// check the routes here
			if (routerHandle(request, response)) {
				logger.info("reqeust " + request + " caught on route");
				return true;
			}
			// try to return the file if exist, or raise an exception
			if (fileFetchingHandle(request, response)) {
				logger.info("reqeust " + request + " caught on file Path");
				return true;
			}
			logger.info("reqeust " + request + " uncaught");
			return false;
		}

		private boolean specialURlHandle(Path reqPath, Request req, Response res) {
			if (shutdown.equals(reqPath)) {
				server.closeApp(context);
				res.body("The server is shut down.");
				return true;
			}
			if (control.equals(reqPath)) {
				handleControlRequest(req, res);
				return true;
			}
			return false;
		}

		private void handleControlRequest(Request req, Response res) {
			// TODO: actual implements the method
			return;
		}

		private boolean fileFetchingHandle(Request request, Response response) throws HaltException {
			Path requestPath = Paths.get("./" + request.pathInfo()).normalize();
			Path filePath = rootPath.resolve(requestPath);
			logger.info("requesting file on paht: " + filePath);
			File requestedFile = filePath.toFile();
			// Check whether the file exists
			if (!requestedFile.exists() || ! requestedFile.isFile()) {
				throw new HaltException(404, "Not Found");
			}
			// Check special conditions
			if (request.headers().contains("if-modified-since")) {

				ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-modified-since"));
				if (reqDate != null && reqDate.toInstant().toEpochMilli() < requestedFile.lastModified()) {
					throw new HaltException(304, "Not Modified");
				}
			}

			if (request.headers().contains("if-modified-since")) {

				ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-unmodified-since"));
				if (reqDate != null && reqDate.toInstant().toEpochMilli() > requestedFile.lastModified()) {
					throw new HaltException(412, "Precondition Failed");
				}
			}
			// try return the file
			try {
				byte[] allBytes = Files.readAllBytes(filePath);
				response.bodyRaw(allBytes);
				response.type(HttpParsing.getMimeType(request.pathInfo()));
				response.status(200);
				return true;
			} catch (IOException e) {
				logger.error(e);
				throw new HaltException(500, "internal Error");
			}
		}

	}

	private class HeadRequestHandler extends GetRequestHandler {

		@Override
		protected boolean handle(Request request, Response response) throws HaltException {
			if (routerHandle(request, response)) {
				return true;
			}
			boolean handled = super.handle(request, response);
			response.body("");
			return handled;
		}

	}

}
