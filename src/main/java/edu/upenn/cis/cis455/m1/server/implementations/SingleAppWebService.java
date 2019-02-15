package edu.upenn.cis.cis455.m1.server.implementations;
import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.TimedPath;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m2.server.interfaces.WebService;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The single app web service implementation
 */

/**
 * @author jimmyyummy
 *
 */
public class SingleAppWebService extends WebService {
		
	protected final AppContext context;
	
	private static AtomicInteger portNum = new AtomicInteger(8888);
	/**
	 * 
	 */
	public SingleAppWebService() {
		context = new AppContext();
		basicServer = ServiceFactory.getHttpServer();
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#start()
	 */
	@Override
	public void start() {
		if (context.isActive()) {
			return;
		}
		context.isActive = true;
		basicServer.start(context);
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#stop()
	 */
	@Override
	public void stop() {
		basicServer.closeApp(context);
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#staticFileLocation(java.lang.String)
	 */
	@Override
	public void staticFileLocation(String directory) {
		if (context.isActive()) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.fileLocation = directory;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#get(java.lang.String, edu.upenn.cis.cis455.handlers.Route)
	 */
	@Override
	public void get(String path, Route route) {
		putInRoutes(path, route, HttpMethod.GET);
	}
	
	@Override
	public void post(String path, Route route) {
		putInRoutes(path, route, HttpMethod.POST);
	}

	@Override
	public void put(String path, Route route) {
		putInRoutes(path, route, HttpMethod.PUT);		
	}

	@Override
	public void delete(String path, Route route) {
		putInRoutes(path, route, HttpMethod.DELETE);	
	}

	@Override
	public void head(String path, Route route) {
		putInRoutes(path, route, HttpMethod.HEAD);
	}

	@Override
	public void options(String path, Route route) {
		putInRoutes(path, route, HttpMethod.OPTIONS);
	}
	
	private void putInRoutes(String path, Route route, HttpMethod method) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		Map<Path, Route> pathToRoute = this.context.routes.get(method);
		Path timedPath = new TimedPath(path);
		if (! pathToRoute.containsKey(timedPath)) {
			pathToRoute.put(timedPath, route);
		}
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#ipAddress(java.lang.String)
	 */
	@Override
	public void ipAddress(String ipAddress) {
		if (context.isActive()) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.ipaddr = ipAddress;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#port(int)
	 */
	@Override
	public void port(int port) {
		if (context.isActive()) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.port = port;
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#threadPool(int)
	 */
	@Override
	public void threadPool(int threads) {
		if (context.isActive()) {
			throw new IllegalStateException("This must be done before the route mapping");
		}
		context.threadNum = threads;
	}
	
	@Override
	public void before(Filter filter) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		context.beforeGeneralFilters.add(filter);
	}

	@Override
    /**
     * Add filters that get called after a request
     */
    public void after(Filter filter) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
    	context.afterGeneralFilters.add(filter);
    }
    
    @Override
    /**
     * Add filters that get called before a request
     */
    public void before(String path, String acceptType, Filter filter) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
    	
    	Map<Path, Map<String, List<Filter>>> filters = context.beforeFilters;
    	putFilters(filters, path, acceptType, filter);
    }

	@Override
    /**
     * Add filters that get called after a request
     */
    public void after(String path, String acceptType, Filter filter) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
    	
    	Map<Path, Map<String, List<Filter>>> filters = context.afterFilters;
    	putFilters(filters, path, acceptType, filter);
    }
    
    private void putFilters(Map<Path, Map<String, List<Filter>>> filters, 
    		String path, String acceptType, Filter filter) {
    	Path timedPath = new TimedPath(path);
    	if (! filters.containsKey(timedPath)) {
    		filters.put(timedPath, new HashMap<>());
    	}
    	Map<String, List<Filter>> typeFilterMap = filters.get(timedPath);
    	if (! typeFilterMap.containsKey(acceptType)) {
    		typeFilterMap.put(acceptType, new ArrayList<>());
    	}
    	typeFilterMap.get(acceptType).add(filter);
    }
    	
	public class AppContext implements Context {
		
		private Map<HttpMethod, Map<Path, Route>> routes;
		private List<Filter> beforeGeneralFilters;
		private List<Filter> afterGeneralFilters;
		private Map<Path, Map<String, List<Filter>>> beforeFilters;
		private Map<Path, Map<String, List<Filter>>> afterFilters;
		private int port;
		private String ipaddr;
		private String fileLocation;
		private int threadNum;
		private boolean isActive;
		private ServerSocket socket;
		
		private AppContext() {
			routes = new HashMap<>();
			for (HttpMethod method : HttpMethod.values()) {
				routes.put(method, new TreeMap<>());
			}
			beforeGeneralFilters = new ArrayList<>();
			afterGeneralFilters = new ArrayList<>();
			beforeFilters = new TreeMap<>();
			afterFilters = new TreeMap<>();
			port = portNum.getAndIncrement();
			ipaddr = "0.0.0.0";
			fileLocation = "./www";
			threadNum = 10;
			isActive = false;
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
		
		public Map<HttpMethod, Map<Path, Route>> getRoutes() {
			return routes;
		}
		
		public Map<Path, Map<String, List<Filter>>> getBeforeFilters() {
			return beforeFilters;
		}
		
		public Map<Path, Map<String, List<Filter>>> getAfterFilters() {
			return afterFilters;
		}

		@Override
		public ServerSocket getServSocket() {
			return socket;
		}

		@Override
		public void putServSocket(ServerSocket socket) {
			this.socket = socket;
		}

		@Override
		public List<Filter> getGeneralBeforeFilters() {
			return beforeGeneralFilters;
		}

		@Override
		public List<Filter> getGeneralAfterFilters() {
			return afterGeneralFilters;
		}
		
	}
}
