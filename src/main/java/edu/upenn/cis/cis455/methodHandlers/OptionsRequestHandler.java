package edu.upenn.cis.cis455.methodHandlers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;

public class OptionsRequestHandler extends BasicRequestHandler {

	protected final static Logger logger = LogManager.getLogger(OptionsRequestHandler.class);

	private Map<HttpMethod, Map<Path, Route>> allRoutes;

	public OptionsRequestHandler(Context context) {
		allRoutes = context.getRoutes();
	}

	public boolean handle(Request request, Response response) throws HaltException {
		// 1. find the router
		if (routerHandle(request, response)) {
			return true;
		}

		return checkMethods(Paths.get(request.pathInfo()).normalize(), response);
	}

	private boolean checkMethods(Path requestPath, Response response) {
		Set<HttpMethod> supportedMethods = new HashSet<>();
		// 2. find the path in other's method handlers' routers
		for (Map.Entry<HttpMethod, Map<Path, Route>> ent : allRoutes.entrySet()) {
			if (ent.getValue().containsKey(requestPath)) {
				supportedMethods.add(ent.getKey());
			}
		}

		// 3. find the path as a file
		if (supportedMethods.isEmpty()) {
			return false;
		}
		supportedMethods.add(HttpMethod.OPTIONS);
		StringBuilder sb = new StringBuilder();
		for (HttpMethod method : supportedMethods) {
			sb.append(method.toString());
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		response.header("Allow", sb.toString());
		return true;
	}
}
