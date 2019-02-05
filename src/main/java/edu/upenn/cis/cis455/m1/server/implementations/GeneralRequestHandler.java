package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
	private List<Filter> generalBeforeFilters;
	private List<Filter> generalAfterFilters;
	private Map<Path, Map<String, List<Filter>>> typeBeforeFilters;
	private Map<Path, Map<String, List<Filter>>> typeAfterFilters;
	private Context context;
	private HttpServer server;
	private static Path shutdown = Paths.get("/shutdown").normalize();
	private static Path control = Paths.get("/control").normalize();
	private static Path unimatcher = Paths.get("*");
	private static Path errorLogPath = Paths.get("error.log");

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
		generalBeforeFilters = context.getGeneralBeforeFilters();
		generalAfterFilters = context.getGeneralAfterFilters();
		typeBeforeFilters = context.getBeforeFilters();
		typeAfterFilters = context.getAfterFilters();

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
		logger.info("checking before filter");
		try {
			for (Path filterPath : typeBeforeFilters.keySet()) {
				if (checkFilterMatch(filterPath, requestPath)) {
					Map<String, List<Filter>> typeToFilters = typeBeforeFilters.get(filterPath);
					if (typeToFilters.containsKey(response.type())) {
						List<Filter> filters = typeToFilters.get(response.type());
						for (Filter f : filters) {
							f.handle(request, response);
						}
					}
				}
			}

			for (Filter f : generalBeforeFilters) {
				f.handle(request, response);
			}

		} catch (HaltException he) {
			throw he;
		} catch (Exception e) {
			logger.error("Error caught: Uncaught General Exception On Before Filter - " + e.getMessage());
			throw new HaltException(500, "Unexpected Error" + requestPath);
		}

		if (response.status() != 200) {
			throw new HaltException(401, "Unauthorized " + requestPath);
		}
		logger.info("dispatch to handler: " + request.requestMethod());
		// dispatch the request to corresponding handlers
		HttpMethod method = Enum.valueOf(HttpMethod.class, request.requestMethod());
		boolean handled = methodHandlerMap.get(method).handle(request, response);
		if (!handled) {
			throw new HaltException(404, "Not Found " + requestPath);
		}
		// TODO: check after filter here
		logger.info("checking after filters");
		try {
			for (Path filterPath : typeAfterFilters.keySet()) {
				if (checkFilterMatch(filterPath, requestPath)) {
					Map<String, List<Filter>> typeToFilters = typeAfterFilters.get(filterPath);
					if (typeToFilters.containsKey(response.type())) {
						List<Filter> filters = typeToFilters.get(response.type());
						for (Filter f : filters) {
							f.handle(request, response);
						}
					}
				}
			}

			for (Filter f : generalAfterFilters) {
				f.handle(request, response);
			}
		} catch (HaltException he) {
			throw he;
		} catch (Exception e) {
			logger.error("Error caught: Exception on After Filter - " + e.getMessage());
			throw new HaltException(500, "Unexpected Error " + requestPath);
		}

		// added content type if not specified
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
			res.type("text/html");
			StringBuilder sb = new StringBuilder();
			Map<String, String> infos = server.getThreadPoolInfo();
			// start of doc
			sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Sample File</title>\n</head>\n"
					+ "<body>\n<h1>Welcome</h1>\n<ul>\n");
			// ThreadPool Monitor
			sb.append("<li>Thread Pool:\n" + "	<ul>\n");
			for (Map.Entry<String, String> threadInfo : infos.entrySet()) {
				sb.append(String.format("<li>%s: %s</li>\n", threadInfo.getKey(), threadInfo.getValue()));
			}
			// Shutdown URL
			sb.append("	</ul>\n" + "</li>\n" + "<li><a href=\"/shutdown\">Shut down</a></li>\n");
			// Error log
			File errorLogFile = errorLogPath.toFile();
			if (errorLogFile.exists() && errorLogFile.isFile() && errorLogFile.canRead()) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(errorLogFile));
					sb.append("<li>Error Log:\n" + "	<ul>\n");
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
				} catch (IOException e) {
					logger.error("error on reading error log");
					e.printStackTrace();
				} finally {
					
				}
			}
			//end of doc
			sb.append("</ul>\n</body>\n</html>");
			res.body(sb.toString());
			return;
		}

		private boolean fileFetchingHandle(Request request, Response response) throws HaltException {
			// TODO: use exception
			Path requestPath = Paths.get("./" + request.pathInfo()).normalize();
			if (checkPermission(requestPath)) {
				response.status(403);
				response.body("Permission Denied on the requested path.");
				return true;
			}
			Path filePath = rootPath.resolve(requestPath);
			logger.info("requesting file on paht: " + filePath);
			File requestedFile = filePath.toFile();
			// Check whether the file exists
			if (!requestedFile.exists() || !requestedFile.isFile()) {
				throw new HaltException(404, "Not Found " + requestPath);
			}
			// Check special conditions
			if (request.headers().contains("if-modified-since")) {

				ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-modified-since"));
				if (reqDate != null && reqDate.toInstant().toEpochMilli() < requestedFile.lastModified()) {
					throw new HaltException(304, "Not Modified " + requestPath);
				}
			}

			if (request.headers().contains("if-modified-since")) {

				ZonedDateTime reqDate = DateTimeUtil.parseDate(request.headers("if-unmodified-since"));
				if (reqDate != null && reqDate.toInstant().toEpochMilli() > requestedFile.lastModified()) {
					throw new HaltException(412, "Precondition Failed " + requestPath);
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
				logger.error("Error caught: FileIOException on GET - " + e.getMessage());
				throw new HaltException(500, "internal Error " + requestPath);
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

	// TODO: future improvement to throw halt exceptions
	public boolean checkPermission(Path requestPath) {
		return requestPath.startsWith("etc/passwd");
	}

	// TODO:
	public boolean checkFilterMatch(Path filterPath, Path requestPath) {

		return checkPathMatch(0, 0, filterPath, requestPath);
	}

	private static boolean checkPathMatch(int fIdx, int rIdx, Path fPath, Path rPath) {
		if (fIdx == fPath.getNameCount() && rIdx == rPath.getNameCount())
			return true;
		if (fIdx == fPath.getNameCount() || rIdx == rPath.getNameCount())
			return false;
		if (fPath.getName(fIdx).equals(unimatcher)) {
			for (int i = rIdx + 1; i <= rPath.getNameCount(); i++) {
				if (checkPathMatch(fIdx + 1, i, fPath, rPath))
					return true;
			}
			return false;
		}
		if (!fPath.getName(fIdx).equals(rPath.getName(rIdx)))
			return false;
		return checkPathMatch(fIdx + 1, rIdx + 1, fPath, rPath);
	}
}
