package edu.upenn.cis.cis455.methodHandlers;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;

public class HeadRequestHandler extends GetRequestHandler {

	public HeadRequestHandler(Context context, HttpServer server) {
		super(context, server);
	}

	@Override
	public boolean handle(Request request, Response response) throws HaltException {
		if (routerHandle(request, response)) {
			return true;
		}
		boolean handled = super.handle(request, response);
		response.body("");
		return handled;
	}

}