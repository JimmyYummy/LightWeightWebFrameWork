package edu.upenn.cis.cis455.methodHandlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.HttpParsing;
import edu.upenn.cis.cis455.util.PathUtil;

public class GetRequestHandler extends BasicRequestHandler {

	protected final static Logger logger = LogManager.getLogger(GetRequestHandler.class);
	private Path rootPath;

	public GetRequestHandler(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Null context or server on crearting GET handler");
		}
		this.rootPath = Paths.get(context.getFileLocation()).normalize();
	}

	@Override
	public boolean handle(Request request, Response response) throws HaltException {
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

	private boolean fileFetchingHandle(Request request, Response response) throws HaltException {
		Path requestPath = Paths.get("./" + request.pathInfo()).normalize();
		if (!PathUtil.checkReadPermission(requestPath)) {
			throw new HaltException(403, "Permission Denied on the requested path.");
		}
		Path filePath = rootPath.resolve(requestPath);
		logger.info("requesting file on path: " + filePath);
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
			response.header("last-modified", DateTimeUtil.miliToDate(requestedFile.lastModified()));
			return true;
		} catch (IOException e) {
			logger.error("Error caught: FileIOException on GET - " + e.getMessage());
			throw new HaltException(500, "internal Error " + requestPath);
		}
	}

}
