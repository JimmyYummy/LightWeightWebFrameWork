package edu.upenn.cis.cis455.m1.server.implementations;

public class Application extends SingleAppWebService {
	MultipleAppWebService webService;

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
		if (! context.isRunning()) {
			throw new IllegalStateException("the app is not running");
		}
		webService.stop(this);
	}
	
	protected void registerService(MultipleAppWebService mws) {
		if (context.isRunning()) {
			throw new IllegalStateException("The service is already running");
		}
		webService = mws;
		context.setRunning();
	}
}
