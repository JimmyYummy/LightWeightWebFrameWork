package edu.upenn.cis.cis455.m1.server.implementations;

public class Application extends SingleAppWebService {

	@Override
	public void start() {
		System.err.println("The application class cannot be lauched by itself, "
				+ "please use MultipleAppWebService to run the application");
	}
	
	@Override
	public void awaitInitialization() {
		return;
	}
	
	@Override
	public void stop() {
		context.setUnactive();
	}
	
	protected void run() {
		context.setRunning();
	}
}
