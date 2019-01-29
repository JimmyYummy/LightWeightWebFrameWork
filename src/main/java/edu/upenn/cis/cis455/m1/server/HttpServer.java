package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.implementations.BasicResposne;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.m1.server.interfaces.ThreadPool;

/**
 * Stub for your HTTP server, which
 * listens on a ServerSocket and handles
 * requests
 */
public class HttpServer implements ThreadManager {
    final static Logger logger = LogManager.getLogger(HttpServer.class);
	
	private AtomicInteger appCount;
	HttpTaskQueue taskQueue;
	ThreadPool pool;
	
	public HttpServer() {
		logger.info("Creatign the HttpServer");
		taskQueue = new HttpTaskQueue();
		appCount = new AtomicInteger(0);
		pool = null;
	}
	
	public void start(Context context) {	
		if (pool == null) {
			
			pool = SOMETHING;
			new Thread (() -> {
				logger.info("Creating the thread pool");
				try {
					while (isActive()) {
						// TODO: if has available thread, do the task
					}
				} finally {
					//TODO: close all the stuff
					logger.info("Closing the thread pool");
				}
				
			}).start();;
		}
		
		new Thread(()-> {
			try {
				ServerSocket socket = new ServerSocket(context.getPort());
				appCount.incrementAndGet();
				logger.info("Listening on port: " + context.getPort());
				while (true) {
					Socket sc = socket.accept();
					taskQueue.offer(new HttpTask(sc));
				}
			} catch (IOException e) {
				logger.debug(e);
			} finally {
				appCount.decrementAndGet();
			}
		}).start();
	}
	
	
    @Override
    public HttpTaskQueue getRequestQueue() {
        return taskQueue;
    }
    

    @Override
    public boolean isActive() {
        return appCount.intValue() != 0;
    }

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
}
