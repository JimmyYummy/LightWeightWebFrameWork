package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.ServiceFactory;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;

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
		handlerResolver = ServiceFactory.getHandlerResolver();
		contexts = new ArrayList<>();
	}
	
	public void start(Context context) {	
		if (pool == null) {
			pool = ServiceFactory.getThreadPool(context.getThreadNum());
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
}
