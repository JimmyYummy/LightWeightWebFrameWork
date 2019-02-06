package edu.upenn.cis.cis455.methodHandlers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.DateTimeUtil;
import edu.upenn.cis.cis455.util.PathUtil;

public class DeleteRequestHandler extends BasicRequestHandler {
	protected final static Logger logger = LogManager.getLogger(PutRequestHandler.class);

	private Path rootPath;

	public DeleteRequestHandler(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Null context on crearting DELETE handler");
		}
		this.rootPath = Paths.get(context.getFileLocation()).normalize();
	}

	@Override
	public boolean handle(Request request, Response response) throws HaltException {
		if (routerHandle(request, response)) {
			return true;
		}
		return deleteFile(request, response);
	}
	
	private boolean deleteFile(Request request, Response response) {
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
		modificationHeaderCheck(request, requestedFile, requestPath);

		return requestedFile.delete();
	}
}
