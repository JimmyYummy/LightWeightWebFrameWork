package edu.upenn.cis.cis455.m1.server.implementations;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;

public class MockContext implements Context {
	
	private String fileLocation;
	
	public MockContext(String fileLocation) {
		if (fileLocation == null) {
			throw new IllegalArgumentException("the file location (root path cannot be null)");
		}
		this.fileLocation = fileLocation;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public String getIpaddr() {
		return "0.0.0.0";
	}

	
	@Override
	public String getFileLocation() {
		return fileLocation;
	}

	@Override
	public int getThreadNum() {
		return 0;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public void setUnactive() {

	}

	@Override
	public Map<HttpMethod, Map<Path, Route>> getRoutes() {
		return Collections.emptyMap();
	}

	@Override
	public List<Filter> getGeneralBeforeFilters() {
		return Collections.emptyList();
	}

	@Override
	public List<Filter> getGeneralAfterFilters() {
		return Collections.emptyList();
	}

	@Override
	public Map<Path, Map<String, List<Filter>>> getBeforeFilters() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Path, Map<String, List<Filter>>> getAfterFilters() {
		return Collections.emptyMap();
	}

	@Override
	public ServerSocket getServSocket() {
		return null;
	}

	@Override
	public void putServSocket(ServerSocket socket) {

	}

}
