package edu.upenn.cis.cis455.m1.server.implementations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;
import edu.upenn.cis.cis455.methodHandlers.*;
import edu.upenn.cis.cis455.util.PathUtil;

public class GeneralRequestHandler implements HttpRequestHandler {
	final static Logger logger = LogManager.getLogger(GeneralRequestHandler.class);

	private Map<HttpMethod, BasicRequestHandler> methodHandlerMap;
	private List<Filter> generalBeforeFilters;
	private List<Filter> generalAfterFilters;
	private Map<Path, Map<String, List<Filter>>> typeBeforeFilters;
	private Map<Path, Map<String, List<Filter>>> typeAfterFilters;


	public GeneralRequestHandler(Path rootPath) {
		Context context = new MockContext(rootPath.normalize().toString());
		methodHandlerMap = createHandlerMap(context);
	}

	public GeneralRequestHandler(Context context) {
		// create sub-handers and feed in the routes
		methodHandlerMap = createHandlerMap(context);
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
	}

	private Map<HttpMethod, BasicRequestHandler> createHandlerMap(Context context) {
		Map<HttpMethod, BasicRequestHandler> m = new HashMap<>();
		for (HttpMethod method : HttpMethod.values()) {
			m.put(method, ServiceFactory.createReqeustHandler(method, context));
		}
		return m;
	}
	
	@Override
	public void handle(edu.upenn.cis.cis455.m1.server.interfaces.Request request,
			edu.upenn.cis.cis455.m1.server.interfaces.Response response) throws HaltException {
		handle((Request) request, (Response) response);
		
	}

	public void handle(Request request, Response response) throws HaltException {
		// get the path of the request
		Path requestPath = Paths.get(request.pathInfo()).normalize();
		logger.info("handling request path: " + requestPath);
		// check before filter here
		logger.info("checking before filter");
		try {
			for (Path filterPath : typeBeforeFilters.keySet()) {
				if (PathUtil.checkPathMatch(filterPath, requestPath, request)) {
					Map<String, List<Filter>> typeToFilters = typeBeforeFilters.get(filterPath);
					if (typeToFilters.containsKey(request.requestMethod())) {
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
		
		//check after filter here
		logger.info("checking after filters");
		try {
			for (Path filterPath : typeAfterFilters.keySet()) {
				if (PathUtil.checkPathMatch(filterPath, requestPath, request)) {
					Map<String, List<Filter>> typeToFilters = typeAfterFilters.get(filterPath);
					if (typeToFilters.containsKey(request.requestMethod())) {
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
	
}
