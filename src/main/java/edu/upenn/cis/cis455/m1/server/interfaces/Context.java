package edu.upenn.cis.cis455.m1.server.interfaces;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
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
	
	public List<Filter> getGeneralBeforeFilters();
	
	public List<Filter> getGeneralAfterFilters();

	public Map<Path, Map<String, List<Filter>>> getBeforeFilters();
	
	public Map<Path, Map<String, List<Filter>>> getAfterFilters();
	
	public ServerSocket getServSocket();
	
	public void putServSocket(ServerSocket socket);
}
