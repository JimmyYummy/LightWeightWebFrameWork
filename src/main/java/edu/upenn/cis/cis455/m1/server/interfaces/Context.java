package edu.upenn.cis.cis455.m1.server.interfaces;

import java.util.Collection;

import edu.upenn.cis.cis455.handlers.Route;

public interface Context {
	public int getPort();
	
	public String getIpaddr();
	
	public String getFileLocation();
	
	public int getThreadNum();
	
	public boolean isActive();
	
	public void setUnactive();
	
	public Route getRoute(String path);
	
	public Collection<String> getRegisteredPaths();
}
