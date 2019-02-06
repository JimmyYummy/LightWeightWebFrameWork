package edu.upenn.cis.cis455.methodHandlers;

import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;

public abstract class methodHandlerFactory {
	public static BasicRequestHandler createReqeustHandler(HttpMethod method, Context context, HttpServer server) {
		switch (method) {
		case GET:
			return new GetRequestHandler(context, server);
		case HEAD:
			return new HeadRequestHandler(context, server);
		case POST:
			return new PostRequestHandler(context);
		case PUT:
			return new PutRequestHandler(context);
		case DELETE:
			return new DeleteRequestHandler(context);
		case OPTIONS:
			return new OptionsRequestHandler(context);
		default:
			return new BasicRequestHandler();
		}

	}
}
