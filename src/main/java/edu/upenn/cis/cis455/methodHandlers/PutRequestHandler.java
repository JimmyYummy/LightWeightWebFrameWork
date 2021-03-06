package edu.upenn.cis.cis455.methodHandlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.interfaces.Context;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;
import edu.upenn.cis.cis455.m2.server.interfaces.Response;
import edu.upenn.cis.cis455.util.PathUtil;

public class PutRequestHandler extends BasicRequestHandler {

	protected final static Logger logger = LogManager.getLogger(PutRequestHandler.class);

	private Path rootPath;

	public PutRequestHandler(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Null context on crearting PUT handler");
		}
		this.rootPath = Paths.get(context.getFileLocation()).normalize();
	}

	@Override
	public boolean handle(Request request, Response response) throws HaltException {
		if (routerHandle(request, response)) {
			return true;
		}
		return createFile(request, response);
	}

	private boolean createFile(Request request, Response response) throws HaltException {
		Path requestPath = Paths.get("./" + request.pathInfo()).normalize();
		if (! PathUtil.checkWritePermission(requestPath)) {
			throw new HaltException(403, "Permission Denied on the requested path.");
		}
		Path filePath = rootPath.resolve(requestPath);
		logger.info("putting / posting file on path: " + filePath);
		File requestedFile = filePath.toFile();
		// Check whether the file exists
		if (requestedFile.exists()) {
			logger.warn("Overwriting the file on path: " + requestPath);
			// Check special conditions
			modificationHeaderCheck(request, requestedFile, requestPath);

		}
		
		// write the file
		byte[] bytes = request.body().getBytes();
		if (bytes == null) {
			bytes = new byte[0];
		}
		logger.debug("file content: " + new String(bytes));
		try { 
            // Initialize a pointer 
            // in file using OutputStream 
            OutputStream os = new FileOutputStream(requestedFile); 
            // Starts writing the bytes in it 
            os.write(bytes);
            os.flush();
            logger.info("file wrote: " + requestPath);
            // Close the file 
            os.close(); 
        } 
  
        catch (IOException e) { 
        	logger.error("Error caught: FileIOException on PUT/POST - " + e.getMessage());
			throw new HaltException(500, "internal Error " + requestPath);
        } 
		return true;
	}
}
