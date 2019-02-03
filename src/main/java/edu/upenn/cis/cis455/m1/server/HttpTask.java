package edu.upenn.cis.cis455.m1.server;

import java.net.Socket;

public class HttpTask {
    private Socket requestSocket;
    private int port;
    
    public HttpTask(Socket socket, int port) {
        requestSocket = socket;
        this.port = port;
    }
    
    public Socket getSocket() {
        return requestSocket;
    }
    
    public int getPort() {
    	return port;
    }
    
    @Override
    public String toString() {
    	return "task on " + requestSocket;
    }
}
