package edu.upenn.cis.cis455.m1.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.ServiceFactory;
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
    	if (except == null) {
    		except = new HaltException(500, "Unknown Error");
    	}
    	if (request == null) {
    		request = ServiceFactory.getRequestForException();
    	}
			try {
				//get writer
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				// write initial line
				logger.debug(request);
				String firstLine = String.format("%s %d %s\r\n", 
						request.protocol(), except.statusCode(), HttpParsing.explainStatus(except.statusCode()));
				writer.append(firstLine);
				// append headers
				writer.append("Server: CIS-550/JingWang\r\n");
				writer.append(String.format("Last-Modified: %s\r\n", DateTimeUtil.getDate()));
				int bodyLength = except.body() == null ? 0 : except.body().length();
				writer.append(String.format("Content-Length: %d\r\n", bodyLength));
				if (request.protocol().equals("HTTP/1.1")) {
					writer.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
					keepOpen = true;
				}
				if (! request.persistentConnection()) {
					writer.append("Connection: close\r\n");
					keepOpen = false;
				}
				writer.append("\r\n");
				// write body
				writer.append(except.body());
				writer.append("\r\n");
				writer.flush();
			} catch (IOException e) {
				logger.error("Error caught: IOException on Sending Exception - " + e.getMessage());
				return false;
			} 
			logger.info("socekt: " + socket + " keeps open? " + keepOpen);
			return keepOpen;

    }

	/**
     * Sends data back.   Returns true if we are supposed to keep the connection open (for 
     * persistent connections).
     */
    public static boolean sendResponse(Socket socket, Request request, Response response) {
    	// filter the chunked req for chunked res
    	if (request.headers().contains("response-transfer-encoding")
    			&& "chunked".equals(request.headers("response-transfer-encoding").toLowerCase())) {
    		return sendChunkedResponse(socket, request, response);
    	} else {
    		return sendPlainResponse(socket, request, response);
    	}
			
    }

	private static boolean sendChunkedResponse(Socket socket, Request request, Response response) {
		BufferedWriter writer = null;
		try {
			// get the writer
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//			//debug
//			writer = new StringBuilder();
			// write the initial line
			String firstLine = String.format("%s %d %s\r\n", 
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
			writer.append("Transfer-Encoding: chunked\r\n");
			writer.append(String.format("Content-Type: %s\r\n", response.type()));
			writer.append(response.getHeaders());
			writer.append("\r\n");
			// write the body
			ByteArrayInputStream bodyStream = new ByteArrayInputStream(response.bodyRaw());
			int size = -1;
			byte[] b = new byte[255];
			while ((size = bodyStream.read(b)) != -1) {
				writer.append(String.valueOf(size));
				writer.append("\r\n");
				writer.append(new String(b, 0, size));
				writer.append("\r\n");
			}
			// end of transfer
			writer.append("0\r\n\r\n");
			writer.flush();
		}
		catch (IOException e) {
			logger.error("Error caught: IOException on Sending Chunked Response - " + e.getMessage());
			return false;
		} 
		if (response.status() == 100) {
			logger.info("socket: " + socket + " keeps open? true (100 response)");
			return true;
		} else {
			logger.info("socket: " + socket + " keeps open? " + request.persistentConnection());
			return request.persistentConnection();
		}
	}
	
	private static boolean sendPlainResponse(Socket socket, Request request, Response response) {
    	BufferedWriter writer = null;
		try {
			// get the writer
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//			//debug
//			writer = new StringBuilder();
			// write the initial line
			String firstLine = String.format("%s %d %s\r\n", 
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
			// write the body
			writer.append(response.body());
			writer.append("\r\n");
			writer.flush();
		}
		catch (IOException e) {
			logger.error("Error caught: IOException on Sending Noraml Response" + e.getMessage());
			return false;
		} 
		if (response.status() == 100) {
			logger.info("socket: " + socket + " keeps open? true (100 response)");
			return true;
		} else {
			logger.info("socket: " + socket + " keeps open? " + request.persistentConnection());
			return request.persistentConnection();
		}
	}
    
    public static boolean sendContinueResponse(Socket socket) {
    	BufferedWriter writer = null;
		try {
			// get the writer
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//			writer = new StringBuilder();
			// write the initial line
			writer.append("HTTP/1.1 100 Continue\r\n\r\n");
			writer.flush();
		} catch (IOException e) {
			logger.error("Error caught: IOException on Sending 100 Response" + e.getMessage());
			return false;
		}
		return true;
    }
    

}
