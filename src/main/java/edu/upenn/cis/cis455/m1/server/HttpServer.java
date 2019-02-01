package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.selector.ContextSelector;

import edu.upenn.cis.cis455.m1.server.implementations.GeneralRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.HttpRequestHandler;
import edu.upenn.cis.cis455.m1.server.interfaces.ThreadPool;

/**
 * Stub for your HTTP server, which
 * listens on a ServerSocket and handles
 * requests
 */
public class HttpServer implements ThreadManager {
    final static Logger logger = LogManager.getLogger(HttpServer.class);
	
	private AtomicInteger appCount;
	private HttpTaskQueue taskQueue;
	private ThreadPool pool;
	private HandlerResolver handlerResolver;
	private Collection<Context> contexts;
	
	public HttpServer() {
		logger.info("Creatign the HttpServer");
		taskQueue = new HttpTaskQueue();
		appCount = new AtomicInteger(0);
		pool = null;
		handlerResolver = new HandlerResolver();
		contexts = new ArrayList<>();
	}
	
	public void start(Context context) {	
		if (pool == null) {
			pool = new HttpThreadPool(context.getThreadNum());
		}
		
		handlerResolver.addHandler(context);
		
		// create new listener thread
		new Thread(()-> {
			ServerSocket socket = null;
			try {
				socket = new ServerSocket(context.getPort());
				appCount.incrementAndGet();
				logger.info("Listening on port: " + context.getPort());
				while (true) {
					Socket sc = socket.accept();
					taskQueue.offer(new HttpTask(sc));
				}
			} catch (IOException e) {
				logger.error(e);
			} finally {
				appCount.decrementAndGet();
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						logger.error(e);
					}
				}
			}
		}).start();
	}
	
	public HandlerResolver getHandlerResolver() {
		return handlerResolver;
	}
	
	private HttpServer getServer() {
		return this;
	}
	
    @Override
    public HttpTaskQueue getRequestQueue() {
        return taskQueue;
    }

    @Override
    public boolean isActive() {
        return appCount.intValue() != 0;
    }

    // the following three methods are not used in my architecture, 
    // thus no actually implemented
    @Override
    public void start(HttpWorker worker) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void done(HttpWorker worker) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void error(HttpWorker worker) {
        // TODO Auto-generated method stub
        
    }
    
    public void closeServer() {
    	pool.closeAll();
    	for (Context context : contexts) {
    		closeApp(context);
    	}
    }
    
    public void closeApp(Context context) {
    	if (contexts.remove(context)) {
    		context.setUnactive();
    		appCount.decrementAndGet();
    	} else {
    		throw new IllegalArgumentException("context in not loaded into the server");
    	}
    }
    
    public class HttpThreadPool implements ThreadPool{
    	
    	private List<HttpWorker> workers;
    	
    	private HttpThreadPool(int num) {
    		logger.info("creating the threadpool");
    		workers = new ArrayList<>(num);
    		addThreads(num);
    		logger.info("HttpThreadPool running");
    	}
    	
		@Override
		public void addThread() {
			HttpWorker t = new HttpWorker(getServer());
    		workers.add(t);
    		t.start();
    		logger.info("created new worker, current pool size: " + workers.size());
		}

    	@Override
    	public void addThreads(int num) {
    		for (int i = 0; i < num; i++);
    		addThread();
    	}
    	

		@Override
		public void closeThread(Thread t) {
			HttpWorker w = (HttpWorker) t;
			if (workers.contains(w)) {
				w.turnOffWorker();
			} else {
				throw new IllegalArgumentException("No a thread in the pool");
			}
			logger.info("deleted http worker, current pool size: " + workers.size());
		}

		@Override
		public int closeThreads(int n) {
			for (int i = 0; i < n; i++) {
				if (workers.size() == 0) return i;
				workers.remove(workers.size() - 1).turnOffWorker();
			}
			return n;
		}
				
    	@Override
    	public void closeAll() {
    		for (HttpWorker w: workers) {
    			w.turnOffWorker();
    		}
    		workers.clear();
    		logger.info("All the workers are turned off");
    	}
    }
    
    class HandlerResolver {
    	private Map<Integer, HttpRequestHandler> handlerMap;
    	
    	private HandlerResolver() {
    		handlerMap = new HashMap<>();
    	}

		private void addHandler(Context context) {
			if (handlerMap.containsKey(context.getPort())) {
				throw new IllegalArgumentException("Port already in use");
			}
			handlerMap.put(context.getPort(), new GeneralRequestHandler(context, getServer()));
			logger.info("new handler added for new application on port " + context.getPort());
		}
		
		/**
		 * return the request handle on the port if exists,
		 * else return null
		 */
		public HttpRequestHandler getHandler(int port) {
			return handlerMap.getOrDefault(port, null);
		}
    }
}
