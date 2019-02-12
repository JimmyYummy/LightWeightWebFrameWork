package edu.upenn.cis.cis455.m1.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
	 * Sends an exception back, in the form of an HTTP response code and message.
	 * Returns true if we are supposed to keep the connection open (for persistent
	 * connections).
	 */
	public static boolean sendException(Socket socket, Request request, HaltException except) {
		BufferedWriter writer = null;
		boolean keepOpen = false;
		if (except == null) {
			except = new HaltException(500, "Unknown Error");
		}
		if (request == null) {
			request = ServiceFactory.getRequestForException(1);
		}
		try {
			// get writer
			OutputStream out = socket.getOutputStream();
			StringBuilder sb = new StringBuilder();
			// write initial line
			logger.debug("Request is:" + request);
			String firstLine = String.format("%s %d %s\r\n", request.protocol(), except.statusCode(),
					HttpParsing.explainStatus(except.statusCode()));
			sb.append(firstLine);
			// append headers
			sb.append("Server: CIS-550/JingWang\r\n");
			sb.append(String.format("Last-Modified: %s\r\n", DateTimeUtil.getDate()));
			int bodyLength = except.body() == null ? 0 : except.body().length();
			sb.append(String.format("Content-Length: %d\r\n", bodyLength));
			if (request.protocol().equals("HTTP/1.1")) {
				sb.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
				keepOpen = true;
			}
			if (!request.persistentConnection()) {
				sb.append("Connection: close\r\n");
				keepOpen = false;
			}
			sb.append("\r\n");
			// write body
			sb.append(except.body());
			sb.append("\r\n");
			out.write(sb.toString().getBytes());
			out.flush();
		} catch (IOException e) {
			logger.error("Error caught: IOException on Sending Exception - " + e.getMessage());
			return false;
		}
		logger.info("socekt: " + socket + " keeps open? " + keepOpen);
		return keepOpen;

	}

	/**
	 * Sends data back. Returns true if we are supposed to keep the connection open
	 * (for persistent connections).
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
		try {
			// get the writer
			OutputStream out = socket.getOutputStream();
			StringBuilder sb = new StringBuilder();
			// write the initial line
			String firstLine = String.format("%s %d %s\r\n", request.protocol(), response.status(),
					HttpParsing.explainStatus(response.status()));
			sb.append(firstLine);
			// write the headers
			sb.append("Server: CIS-550/JingWang\r\n");
			if (request.protocol().equals("HTTP/1.1")) {
				sb.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
			}
			if (response.status() != 100 && !request.persistentConnection()) {
				sb.append("Connection: close\r\n");
			}
			sb.append("Transfer-Encoding: chunked\r\n");
			sb.append(String.format("Content-Type: %s\r\n", response.type()));
			sb.append(response.getHeaders());
			sb.append("\r\n");
			out.write(sb.toString().getBytes());
			// write the body
			ByteArrayInputStream bodyStream = new ByteArrayInputStream(response.bodyRaw());
			int size = -1;
			byte[] b = new byte[255];
			while ((size = bodyStream.read(b)) != -1) {
				out.write(Integer.toString(size, 16).getBytes());
				out.write("\r\n".getBytes());
				out.write(b, 0, size);
				out.write("\r\n".getBytes());
			}
			// end of transfer
			out.write("0\r\n\r\n".getBytes());
			out.flush();
		} catch (IOException e) {
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
		try {
			// get the OutputStream
			OutputStream out = socket.getOutputStream();
			StringBuilder sb = new StringBuilder();
			// write the initial line
			String firstLine = String.format("%s %d %s\r\n", request.protocol(), response.status(),
					HttpParsing.explainStatus(response.status()));
			sb.append(firstLine);
			// write the headers
			sb.append("Server: CIS-550/JingWang\r\n");
			if (request.protocol().equals("HTTP/1.1")) {
				sb.append(String.format("Date: %s\r\n", DateTimeUtil.getDate()));
			}
			if (response.status() != 100 && !request.persistentConnection()) {
				sb.append("Connection: close\r\n");
			}
			sb.append(String.format("Content-Length: %d\r\n", response.bodyRaw().length));
			sb.append(String.format("Content-Type: %s\r\n", response.type()));
			sb.append(response.getHeaders());
			sb.append("\r\n");
			out.write(sb.toString().getBytes());
			// write the body
			out.write(response.bodyRaw());
			out.write("\r\n".getBytes());
			out.flush();
		} catch (IOException e) {
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
		try {
			// get the writer
			OutputStream out = socket.getOutputStream();
//			writer = new StringBuilder();
			// write the initial line
			out.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
			out.flush();
		} catch (IOException e) {
			logger.error("Error caught: IOException on Sending 100 Response" + e.getMessage());
			return false;
		}
		return true;
	}

}
