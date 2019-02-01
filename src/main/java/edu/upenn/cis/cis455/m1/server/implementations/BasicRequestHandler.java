package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
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
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.HttpParsing;

public class BasicRequestHandler implements HttpRequestHandler {
	final static Logger logger = LogManager.getLogger(BasicRequestHandler.class);
		
	private Path rootPath;
	private Map<Path, Route> routes;
	private List<Path> routePathRanking;
	private Map<Path, Filter> filters;
	private Context context;
	private HttpServer server;
	private static Path shutdown = Paths.get("shutdown");
	private static Path control = Paths.get("control");
	
	public BasicRequestHandler(Path rootPath) {
		this.rootPath = rootPath;
	}
	
	public BasicRequestHandler(Context context, HttpServer server) {
		rootPath = Paths.get(context.getFileLocation()).normalize();
		routes = context.getRoutes();
		filters = context.getFilters();
		this.context = context;
		this.server = server;
		List<Path> routePathRanking = new ArrayList<>(routes.keySet());
		Collections.sort(routePathRanking, Comparator.reverseOrder());
	}

	@Override
	public void handle(Request request, Response response) throws HaltException {
		Path requestPath = Paths.get("./"+ request.pathInfo()).normalize();
		// check filter here
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
		// chexck special url here
		if (specialURlHandle(requestPath, request, response)) {
			return;
		}
		// find the route here
		for (Path routePath : routePathRanking) {
			if (requestPath.startsWith(routePath)) {
				try {
					routes.get(routePath).handle(request, response);
				} catch (Exception e) {
					logger.error(e);
					throw new HaltException(500, "Error while handling the request.");
				}
				break;
			}
		}
		fileFetchingHandle(request, response, requestPath);
	}
	
	private void fileFetchingHandle(Request request, Response response, Path requsetPath) throws HaltException {
		Path filePath = rootPath.resolve(requsetPath);
		File requestedFile = filePath.toFile();
		// Check whether the file exists
		if (! requestedFile.exists()) {
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
		} catch (IOException e) {
			logger.error(e);
			throw new HaltException(500, "internal Error");
		}
	}
	
	
	private boolean specialURlHandle(Path reqPath, Request req, Response res) {
		if (shutdown.equals(reqPath)) {
			this.context.setUnactive();
			return true;
		}
		if (control.equals(reqPath)) {
			handleControlRequest(req, res);
			return true;
		}
		return false;
	}
	
	private void handleControlRequest(Request req, Response res) {
		return;
	}
	
	
}
