package edu.upenn.cis.cis455.methodHandlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpServer;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.HttpParsing;
import edu.upenn.cis.cis455.util.PathUtil;

public class GetRequestHandler extends BasicRequestHandler {

	protected final static Logger logger = LogManager.getLogger(GetRequestHandler.class);

	private static Path shutdown = Paths.get("/shutdown").normalize();
	private static Path control = Paths.get("/control").normalize();
	private static Path errorLogPath = Paths.get("error.log");

	private Context context;
	private HttpServer server;
	private Path rootPath;

	public GetRequestHandler(Context context, HttpServer server) {
		if (context == null || server == null) {
			throw new IllegalArgumentException("Null context or server on crearting GET handler");
		}
		this.context = context;
		this.server = server;
		this.rootPath = Paths.get(context.getFileLocation()).normalize();
	}

	@Override
	public boolean handle(Request request, Response response) throws HaltException {
		// get the path of the request
		Path requestPath = Paths.get(request.pathInfo()).normalize();
		// check special URL here
		if (specialURlHandle(requestPath, request, response)) {
			logger.info("reqeust " + request + " caught on special URL");
			return true;
		}
		// check the routes here
		if (routerHandle(request, response)) {
			logger.info("reqeust " + request + " caught on route");
			return true;
		}
		// try to return the file if exist, or raise an exception
		if (fileFetchingHandle(request, response)) {
			logger.info("reqeust " + request + " caught on file Path");
			return true;
		}
		logger.info("reqeust " + request + " uncaught");
		return false;
	}

	private boolean specialURlHandle(Path reqPath, Request req, Response res) {
		if (shutdown.equals(reqPath)) {
			server.closeApp(context);
			res.body("The server is shut down.");
			return true;
		}
		if (control.equals(reqPath)) {
			handleControlRequest(req, res);
			return true;
		}
		return false;
	}

	private void handleControlRequest(Request req, Response res) {
		res.type("text/html");
		StringBuilder sb = new StringBuilder();
		Map<String, String> infos = server.getThreadPoolInfo();
		// start of doc
		sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Sample File</title>\n</head>\n"
				+ "<body>\n<h1>Welcome</h1>\n<ul>\n");
		// ThreadPool Monitor
		sb.append("<li>Thread Pool:\n" + "	<ul>\n");
		for (Map.Entry<String, String> threadInfo : infos.entrySet()) {
			sb.append(String.format("<li>%s: %s</li>\n", threadInfo.getKey(), threadInfo.getValue()));
		}
		// Shutdown URL
		sb.append("	</ul>\n" + "</li>\n" + "<li><a href=\"/shutdown\">Shut down</a></li>\n");
		// Error log
		File errorLogFile = errorLogPath.toFile();
		if (errorLogFile.exists() && errorLogFile.isFile() && errorLogFile.canRead()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(errorLogFile));
				sb.append("<li>Error Log:\n" + "	<ul>\n");
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} catch (IOException e) {
				logger.error("error on reading error log");
				e.printStackTrace();
			} finally {

			}
		}
		// end of doc
		sb.append("</ul>\n</body>\n</html>");
		res.body(sb.toString());
		return;
	}

	private boolean fileFetchingHandle(Request request, Response response) throws HaltException {
		Path requestPath = Paths.get("./" + request.pathInfo()).normalize();
		if (PathUtil.checkPermission(requestPath)) {
			throw new HaltException(403, "Permission Denied on the requested path.");
		}
		Path filePath = rootPath.resolve(requestPath);
		logger.info("requesting file on paht: " + filePath);
		File requestedFile = filePath.toFile();
		// Check whether the file exists
		if (!requestedFile.exists() || !requestedFile.isFile()) {
			throw new HaltException(404, "Not Found " + requestPath);
		}
		// Check special headers
		modificationHeaderCheck(request, requestedFile, requestPath);

		// try return the file
		try {
			byte[] allBytes = Files.readAllBytes(filePath);
			response.bodyRaw(allBytes);
			response.type(HttpParsing.getMimeType(request.pathInfo()));
			response.status(200);
			return true;
		} catch (IOException e) {
			logger.error("Error caught: FileIOException on GET - " + e.getMessage());
			throw new HaltException(500, "internal Error " + requestPath);
		}
	}

}
