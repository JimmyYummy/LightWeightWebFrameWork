package edu.upenn.cis.cis455.m1.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.HttpParsing;

/**
 * Handles marshalling between HTTP Requests and Responses
 */
public class HttpIoHandler {
	final static Logger logger = LogManager.getLogger(HttpIoHandler.class);

	/**
     * Sends an exception back, in the form of an HTTP response code and message.  Returns true
     * if we are supposed to keep the connection open (for persistent connections).
     */
    public static boolean sendException(Socket socket, Request request, HaltException except) {
    	BufferedWriter writer = null;
    	boolean keepOpen = false;
			try {
				//get writer
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				// write initial line
				logger.debug(request);
				String firstLine = String.format("%d %s %s\r\n\r\n", 
						except.statusCode(), HttpParsing.explainStatus(except.statusCode()), request.protocol());
				writer.append(firstLine);
				// append headers
				writer.append("Server: CIS-550/JingWang\r\n");
				writer.append(String.format("Last-Modified: %s", DateTimeUtil.getDate()));
				writer.append(String.format("Content-Length: ", except.body().length()));
				if (request.protocol().equals("HTTP/1.1")) {
					writer.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
					keepOpen = true;
				}
				if (request.headers("connection").toLowerCase().equals("close")) {
					writer.append("Connection: close\r\n");
					keepOpen = false;
				}
				writer.append("\r\n");
				// write body
				writer.append(except.body());
				writer.append("\r\n");
				writer.flush();
			} catch (IOException e) {
				logger.error(e);
			} 
			logger.info("socekt: " + socket + " keeps open? " + keepOpen);
			return keepOpen;

    }

	/**
     * Sends data back.   Returns true if we are supposed to keep the connection open (for 
     * persistent connections).
     */
    public static boolean sendResponse(Socket socket, Request request, Response response) {
    	BufferedWriter writer = null;
			try {
				// get the writer
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//				writer = new StringBuilder();
				// write the initial line
				String firstLine = String.format("%s %d %s\r\n\r\n", 
						request.protocol(), response.status(), HttpParsing.explainStatus(response.status()));
				writer.append(firstLine);
				// write the headers
				writer.append("Server: CIS-550/JingWang\r\n");
				if (request.protocol().equals("HTTP/1.1")) {
					writer.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
				}
				if (response.status() != 100 && ! request.persistentConnection()) {
					writer.append("Connection: close\r\n");
				}
				writer.append(String.format("Content-Length: %d\r\n", response.body().length()));
				writer.append(String.format("Content-Type: %s\r\n", response.type()));
				writer.append(response.getHeaders());
				writer.append("\r\n");
				writer.append("\r\n");
				// write the body
				writer.append(response.body());
				writer.append("\r\n");
				writer.flush();
			}
			catch (IOException e) {
				logger.error(e);
			} 
			if (response.status() == 100) {
				logger.info("socket: " + socket + " keeps open? true (100 response)");
				return false;
			} else {
				logger.info("socket: " + socket + " keeps open? " + request.persistentConnection());
				return request.persistentConnection();
			}
			
    }
    

}
