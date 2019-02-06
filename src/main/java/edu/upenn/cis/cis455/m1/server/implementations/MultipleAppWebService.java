package edu.upenn.cis.cis455.m1.server.implementations;

import java.util.Collection;
import java.util.HashSet;

public class MultipleAppWebService extends SingleAppWebService {
	private Collection<Application> otherApps;
	
	public void start(Application app) {
		if (otherApps == null) {
			otherApps = new HashSet<>();
		}
		otherApps.add(app);
		app.run();
		basicServer.start(app.context);
	}
	
	public void stop(Application app) {
		if (! otherApps.contains(app)) {
			throw new IllegalArgumentException("the app is not registered in this webservice");
		}
		app.stop();
	}
	
	@Override
	public void stop() {
		for (Application app : otherApps) {
			app.stop();
		}
		super.stop();
	}
}
