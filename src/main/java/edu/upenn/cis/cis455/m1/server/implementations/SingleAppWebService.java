package edu.upenn.cis.cis455.m1.server.implementations;
import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.WebService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
/**
 * The single app web service implementation
 */

/**
 * @author jimmyyummy
 *
 */
public class SingleAppWebService extends WebService {
		
	private SingleAppContext context;
	/**
	 * 
	 */
	public SingleAppWebService() {
		context = new SingleAppContext();
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#start()
	 */
	@Override
	public void start() {
		if (context.isRunning) {
			throw new IllegalStateException("The service is already running");
		}
		context.isRunning = true;
		context.isActive = true;
		basicServer = new HttpServer();
		basicServer.start(context);
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#stop()
	 */
	@Override
	public void stop() {
		context.isActive = false;
		basicServer.closeServer();
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#staticFileLocation(java.lang.String)
	 */
	@Override
	public void staticFileLocation(String directory) {
		if (context.isRunning) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.fileLocation = directory;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#get(java.lang.String, edu.upenn.cis.cis455.handlers.Route)
	 */
	@Override
	public void get(String path, Route route) {
		if (! context.isRunning) {
			this.awaitInitialization();
		}
		this.context.routeResolver.put(Paths.get(path), route);
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#ipAddress(java.lang.String)
	 */
	@Override
	public void ipAddress(String ipAddress) {
		if (context.isRunning) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.ipaddr = ipAddress;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#port(int)
	 */
	@Override
	public void port(int port) {
		if (context.isRunning) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.port = port;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#threadPool(int)
	 */
	@Override
	public void threadPool(int threads) {
		if (context.isRunning) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.threadNum = threads;
	}
	
	public class SingleAppContext implements Context {
		
		private Map<Path, Route> routeResolver;
		private Map<Path, Filter> filterResolver;
		private int port;
		private String ipaddr;
		private String fileLocation;
		private int threadNum;
		private boolean isActive;
		private boolean isRunning;
		
		private SingleAppContext() {
			routeResolver = new HashMap<>();
			filterResolver = new HashMap<>(0);
			port = 8080;
			ipaddr = "0.0.0.0";
			fileLocation = "./www";
			threadNum = 100;
			isActive = false;
			isRunning = false;
		}

		/**
		 * @return the port
		 */
		public int getPort() {
			return port;
		}

		/**
		 * @return the ipaddr
		 */
		public String getIpaddr() {
			return ipaddr;
		}

		/**
		 * @return the fileLocation
		 */
		public String getFileLocation() {
			return fileLocation;
		}

		/**
		 * @return the threadNum
		 */
		public int getThreadNum() {
			return threadNum;
		}
		
		public boolean isActive() {
			return isActive; 
		}

		/**
		 * @param active the isActive to set
		 */
		public void setUnactive() {
			this.isActive = false;
		}
		
		public Map<Path, Route> getRoutes() {
			return routeResolver;
		}
		
		public Map<Path, Filter> getFilters() {
			return filterResolver;
		}
		
	}
}
