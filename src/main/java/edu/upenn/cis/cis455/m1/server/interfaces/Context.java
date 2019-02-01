package edu.upenn.cis.cis455.m1.server.interfaces;

import java.nio.file.Path;
import java.util.Map;

import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;

public interface Context {
	public int getPort();
	
	public String getIpaddr();
	
	public String getFileLocation();
	
	public int getThreadNum();
	
	public boolean isActive();
	
	public void setUnactive();
	
	public Map<HttpMethod, Map<Path, Route>> getRoutes();
	
	public Map<Path, Filter> getFilters();
}
