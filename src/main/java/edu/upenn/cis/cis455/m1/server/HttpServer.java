package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private HttpThreadPool pool;
	private HandlerResolver handlerResolver;
	private List<Context> contexts;
	
	public HttpServer() {
		logger.info("Creating the HttpServer");
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
		
		contexts.add(context);
		
		handlerResolver.addHandler(context);
		
		// create new listener thread
		Thread daemonThread = new Thread(()-> {
			ServerSocket socket = null;
			try {
				
				appCount.incrementAndGet();
				socket = new ServerSocket(context.getPort());
				context.putServSocket(socket);
				logger.info("Listening on port: " + context.getPort() 
				+ " with root: " + context.getFileLocation());
				while (context.isActive()) {
					Socket sc = socket.accept();
					taskQueue.offer(new HttpTask(sc, sc.getLocalPort()));
				}
			} catch (IOException e) {
				context.setUnactive();
				logger.error("Error caught:IOException on Server Listening (might be called by user)- " + e.getMessage());
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e2) {
						logger.catching(e2);
					}
				}
			} finally {
				if (appCount.decrementAndGet() == 0) {
					pool.closeAll();
					taskQueue.unactive();
					logger.info("Web Service Closed");
				}
				logger.info("app " + context + " is shut down");
				System.err.println("running apps: " + appCount.get());
			}
		});
		daemonThread.setName("Daemon Thread-Port:" + context.getPort());
		daemonThread.start();
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
        pool.closeThread(worker);
        
    }
    
    public void closeServer() {
    	for (Context context : contexts) {
    		closeApp(context);
    	}
    }
    
    public void closeApp(Context context) {
    	if (contexts.remove(context)) {
    		context.setUnactive();
    		try {
				ServerSocket  sc = context.getServSocket();
				sc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		logger.info("app is shutdonwn.");
    	} else {
    		logger.info("app already shutdown.");
//    		throw new IllegalArgumentException("context in not loaded into the server");
    	}
    }
    
    public Map<String, String> getThreadPoolInfo() {
    	return pool.getPoolInfo();
    }
    
    public class HttpThreadPool implements ThreadPool{
    	
    	private List<HttpWorker> workers;
    	
    	private HttpThreadPool(int num) {
    		logger.info("creating the threadpool");
    		workers = new ArrayList<>(num);
    		addThreads(num);
    		logger.info("HttpThreadPool running");
    	}
    	
		public synchronized Map<String, String> getPoolInfo() {
			Map<String, String> infos = new HashMap<>(workers.size());
			for (HttpWorker w : workers) {
				infos.put(w.toString(), w.isWorking() ? "handling URL" : "waiting");
			}
			
			return infos;
		}

		@Override
		public synchronized void addThread() {
			HttpWorker t = new HttpWorker(getServer());
    		workers.add(t);
    		t.start();
    		logger.info("created new worker, current pool size: " + workers.size());
		}

    	@Override
    	public void addThreads(int num) {
    		for (int i = 0; i < num; i++) {
    			addThread();
    		}
    	}
    	

		@Override
		public synchronized void closeThread(Thread t) {
			HttpWorker w = (HttpWorker) t;
			workers.remove(w);
			w.turnOffWorker();
			logger.info("deleted http worker, current pool size: " + workers.size());
		}

		@Override
		public int closeThreads(int n) {
			for (int i = 0; i < n; i++) {
				if (workers.size() == 0) return i;
				closeThread(workers.get(workers.size() - 1));
			}
			return n;
		}
				
    	@Override
    	public void closeAll() {
    		while (! workers.isEmpty()) {
    			closeThread(workers.get(workers.size() - 1));
    		}
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
