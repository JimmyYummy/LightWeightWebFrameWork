package edu.upenn.cis.cis455.handlers;

import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;

public abstract class BasicAbsFilter implements Filter {

	@Override
	public abstract void handle(Request request, Response response) throws Exception;

}
