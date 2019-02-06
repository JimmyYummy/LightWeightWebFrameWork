package edu.upenn.cis.cis455.methodHandlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.interfaces.Context;

public class PostRequestHandler extends PutRequestHandler {
	
	protected final static Logger logger = LogManager.getLogger(PostRequestHandler.class);
	
	public PostRequestHandler(Context context) {
		super(context);
	}
}
