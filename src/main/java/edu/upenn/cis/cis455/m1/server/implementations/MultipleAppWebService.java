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
		app.registerService(this);
		basicServer.start(app.context);
	}
	
	public void stop(Application app) {
		if (! otherApps.contains(app)) {
			throw new IllegalArgumentException("the app is not registered in this webservice");
		}
		basicServer.closeApp(app.context);
	}
	
	@Override
	public void stop() {
		for (Application app : otherApps) {
			stop(app);
		}
		super.stop();
	}
	
	public void stopOwnAppOnly() {
		basicServer.closeApp(context);
	}
}
