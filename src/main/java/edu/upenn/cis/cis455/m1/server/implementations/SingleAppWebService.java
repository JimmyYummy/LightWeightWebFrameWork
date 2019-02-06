package edu.upenn.cis.cis455.m1.server.implementations;
import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.handlers.Filter;
import edu.upenn.cis.cis455.handlers.Route;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m2.server.interfaces.WebService;

import java.net.ServerSocket;
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
		
	protected final AppContext context;
	/**
	 * 
	 */
	public SingleAppWebService() {
		context = new AppContext();
	}

	/* (non-Javadoc)
	 * @see edu.upenn.cis.cis455.m1.server.interfaces.WebService#start()
	 */
	@Override
	public void start() {
		if (context.isActive()) {
			throw new IllegalStateException("The service is already running");
		}
		context.setRunning();
		if (basicServer == null) {
			basicServer = ServiceFactory.getHttpServer();
		}
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
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.GET).put(Paths.get(path).normalize(), route);
	}
	
	@Override
	public void post(String path, Route route) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.POST).put(Paths.get(path).normalize(), route);
		
	}

	@Override
	public void put(String path, Route route) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.PUT).put(Paths.get(path).normalize(), route);
		
	}

	@Override
	public void delete(String path, Route route) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.DELETE).put(Paths.get(path).normalize(), route);
		
	}

	@Override
	public void head(String path, Route route) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.HEAD).put(Paths.get(path).normalize(), route);
		
	}

	@Override
	public void options(String path, Route route) {
		if (! context.isActive()) {
			this.awaitInitialization();
		}
		this.context.routes.get(HttpMethod.OPTIONS).put(Paths.get(path).normalize(), route);
		
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
	
	public void before(Filter filter) {
		if (context.isActive()) {
			this.awaitInitialization();
		}
		context.beforeGeneralFilters.add(filter);
	}

    /**
     * Add filters that get called after a request
     */
    public void after(Filter filter) {
		if (context.isActive()) {
			this.awaitInitialization();
		}
    	context.afterGeneralFilters.add(filter);
    }
    /**
     * Add filters that get called before a request
     */
    public void before(String path, String acceptType, Filter filter) {
		if (context.isActive()) {
			this.awaitInitialization();
		}
    	
    	Map<Path, Map<String, List<Filter>>> filters = context.beforeFilters;
    	Path normaledPath = Paths.get(path).normalize();
    	if (! filters.containsKey(normaledPath)) {
    		filters.put(normaledPath, new HashMap<>());
    	}
    	Map<String, List<Filter>> typeFilterMap = filters.get(normaledPath);
    	if (! typeFilterMap.containsKey(acceptType)) {
    		typeFilterMap.put(acceptType, new ArrayList<>());
    	}
    	typeFilterMap.get(acceptType).add(filter);
    }
    /**
     * Add filters that get called after a request
     */
    public void after(String path, String acceptType, Filter filter) {
		if (context.isActive()) {
			this.awaitInitialization();
		}
    	
    	Map<Path, Map<String, List<Filter>>> filters = context.afterFilters;
    	Path normaledPath = Paths.get(path).normalize();
    	if (! filters.containsKey(normaledPath)) {
    		filters.put(normaledPath, new HashMap<>());
    	}
    	Map<String, List<Filter>> typeFilterMap = filters.get(normaledPath);
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
				routes.put(method, new HashMap<>());
			}
			beforeGeneralFilters = new ArrayList<>();
			afterGeneralFilters = new ArrayList<>();
			beforeFilters = new HashMap<>();
			afterFilters = new HashMap<>();
			
			port = 8080;
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
		
		protected void setRunning() {
			this.isActive = true;
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
