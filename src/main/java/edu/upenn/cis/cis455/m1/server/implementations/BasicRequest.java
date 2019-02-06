package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;
import edu.upenn.cis.cis455.util.InputUtil;

public class BasicRequest extends Request {

	final static Logger logger = LogManager.getLogger(BasicRequest.class);

	private HttpMethod method;
	private String url;
	private String protocol;
	private int port;
	private int contentLength;
	private Map<String, String> headers;
	private byte[] body;
	private String bodyStr;
	
	private static BasicRequest exceptionRequest;
    
    public BasicRequest() {};
	
    public static BasicRequest getBasicRequestExceptBody(String url, Map<String, String> headers,
            Map<String, List<String>> params) {
        // create request object
        BasicRequest request = new BasicRequest();
        // set request method
        String method = headers.get("Method").split(";")[0];
        request.method = Enum.valueOf(HttpMethod.class, method);
        // get version of the request
        String protocolVersion = headers.get("protocolVersion").split(";")[0];
        if ("HTTP/1.0".equals(protocolVersion) || "HTTP/1.1".equals(protocolVersion)) {
            request.protocol = protocolVersion;
        } else {
            throw new IllegalArgumentException("Unsupported Http protocol verison");
        }
        // check host header for http/1.1
        if (! headers.containsKey("host")) {
        	if (request.protocol.endsWith("1")) {
        		throw new HaltException(400, "the HTTP/1.1 request miss the host header");
        	} else {
        		headers.put("host", "~~");
        	}
        	
        }

        request.url = url;

        request.headers = new HashMap<String, String>(headers.size());
        // parse host header:
        // get the port number
        request.port = Integer.parseInt(headers.get("port"));
        
        // get the requested path from root
        String host = headers.get("host").split(";")[0];
        int start = url.indexOf(host);
        if (start != -1) {
        	start += host.length();
        } else {
        	start = 0;
        }
        int end = url.indexOf('?');
        if (end == -1)
            end = url.length();
        if (start > end) {
            throw new IllegalArgumentException("malformat url or host");
        }
        String rawPath = start == end ? "/" : url.substring(start, end);
        request.headers.put("pathinfo", Paths.get("/" + rawPath).normalize().toString());
        
        // put the rest of headers in the requst's header
        for (Map.Entry<String, String> ent : headers.entrySet()) {
            if (!request.headers.containsKey(ent.getKey())) {
                request.headers.put(ent.getKey(), ent.getValue().split(";")[0].trim());
            }
        }
        
        headers.putIfAbsent("content-type", "text/plain");
        if (headers.containsKey("content-length")) {
            request.contentLength = Integer.parseInt(headers.get("content-length"));
        } else {
        	request.contentLength = 0;
        }
        
        // check if is persistent connection
        if (request.protocol().equals("HTTP/1.0")) {
        	request.persistentConnection(false);
        } else if (request.headers.containsKey("connection")
    			&& request.headers.get("connection").toLowerCase().equals("close")) {
        	request.persistentConnection(false);
    	} else {
    		request.persistentConnection(true);
        }
        
        return request;
    }
    
    public static BasicRequest getRequestForException() {
    	if (exceptionRequest == null) {
    		BasicRequest r = new BasicRequest();
    		r.protocol = "HTTP/1.1";
    		r.headers = new HashMap<>();
    		r.headers.put("connection", "close");
    		exceptionRequest = r;
    	}
    	return exceptionRequest;
    }
    
    public void addBody(InputStream in) throws IOException {
    	if (this.headers.containsKey("transfer-encoding")
                && "chunked".equals(this.headers.get("transfer-encoding").toLowerCase())) {
            logger.info("parsing chunked req: " + this);
            parseChunkedBdoy(in);
        } else {
            logger.info("parsing normal req: " + this);
            parsePlainBody(in);
        }
    }
    
    //TODO enhance it
    private void parseChunkedBdoy(InputStream in) throws IOException {
    	// skip the blank lines
    	InputUtil.skipBlankLines(in);
    	
    	//get the body
    	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    	byte[] buf = new byte[512];
    	while (true) {
    		int chunkLength = readChunkLength(in);
    		if (chunkLength == 0) {
    			break;
    		}
    		if (chunkLength > buf.length) {
    			buf = new byte[chunkLength];
    		}
    		int rlen = 0;
    		while (rlen < chunkLength) {
    			rlen += in.read(buf, rlen, chunkLength - rlen);
    		}
    		bytes.write(buf, 0, rlen);
    		InputUtil.skipBlankLines(in);
    	}
    	body = bytes.toByteArray();
    	bytes.close();
    	skipFooters(in);
    }

	private void skipFooters(InputStream in) throws IOException {
		//skip the footers
    	//0. no footer:
    	in.mark(200);
    	byte[] b = new byte[200];
    	int size = in.read(b, 0, 2);
    	if (size == 1) {
    		if (b[0] == '\n') {
    			return;
    		}
    	} else if (size == 2) {
    		if (b[0] == '\r' && b[1] == '\n') {
    			return;
    		} else if (b[0] == '\n') {
    			in.reset();
    			in.skip(1);
    			return;
    		}
    	}
    	in.reset();
    	in.mark(200);
    	// 2. footer to long: reject
    	// 3. footer not yet transmitted completely
    	int rlen = 0;
    	int splitbyte = 0;
    	while (rlen < b.length) {
        	size = in.read(b, rlen, b.length - rlen);
        	// different cases:
        	// 1. input closed
        	if (size == -1) {
        		return;
        	}
        	rlen += size;
    		splitbyte = findBlankLine(b, rlen);
    		if (splitbyte != 0) break;
    	}
    	if (splitbyte == 0) {
    		throw new HaltException(400, "Request footer to long or malformated");
    	}
    	in.reset();
    	in.skip(splitbyte);
		
	}

	private int readChunkLength(InputStream in) throws IOException {
    	in.mark(200);
    	byte[] b = new byte[200];
    	int rlen = in.read(b);
    	int splitbyte = findLineBreak(b, rlen);
    	if (splitbyte == 0) {
    		throw new HaltException(400, "Request too long or malformated");
    	}
    	in.reset();
    	in.skip(splitbyte);
    	return Integer.parseInt(new String(b, 0, splitbyte).split(";")[0].trim(), 16);
    }
    
    static int findLineBreak(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 1 < rlen) {

            // RFC2616
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n') {
                return splitbyte + 2;
            }

            // tolerance
            if (buf[splitbyte] == '\n') {
                return splitbyte + 1;
            }
            splitbyte++;
        }
        return 0;
    }
    
    static int findBlankLine(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 1 < rlen) {

            // RFC2616
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' 
            		&& splitbyte + 3 < rlen 
            		&& buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }

            // tolerance
            if (buf[splitbyte] == '\n' && buf[splitbyte + 1] == '\n') {
                return splitbyte + 2;
            }
            splitbyte++;
        }
        return 0;
    }

    private void parsePlainBody(InputStream in) throws IOException {
    	body = new byte[contentLength];
    	if (contentLength != 0) {
    		InputUtil.skipBlankLines(in);
    	}
    	in.read(body);
    }

	@Override
	public String requestMethod() {
		return method.toString();
	}

	@Override
	public String host() {
		return headers.get("host");
	}

	@Override
	public String userAgent() {
		return headers.get("useragent");
	}

	@Override
	public int port() {
		return port;
	}

	@Override
	public String pathInfo() {
		return headers.get("pathinfo");
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public String uri() {
		return url;
	}

	@Override
	public String protocol() {
		return protocol;
	}

	@Override
	public String contentType() {
		return headers.get("content-type");
	}

	@Override
	public String ip() {
		return headers.get("remote-addr");
	}

	@Override
	public String body() {
		if (bodyStr == null) {
			if (body == null) {
				return null;
			}
			bodyStr = new String(body);
		}
		 return bodyStr;
	}

	@Override
	public int contentLength() {
		return contentLength;
	}

	@Override
	public String headers(String name) {
		return headers.get(name);
	}

	@Override
	public Set<String> headers() {
		return headers.keySet();
	}

	@Override
	public String toString() {
		return "" + method + " " + url + " " + protocol + " persisit: " + persistentConnection() + " port: " + port + "\n" + headers + "\n" + body();
	}

}
