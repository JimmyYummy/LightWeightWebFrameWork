package edu.upenn.cis.cis455.m1.server.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpMethod;
import edu.upenn.cis.cis455.m1.server.interfaces.Request;

public class BasicRequest extends Request {

	final static Logger logger = LogManager.getLogger(BasicRequest.class);

	private HttpMethod method;
	private String url;
	private String protocol;
	private int port;
	private int contentLength;
	private Map<String, String> headers;
	private String body;
	
    private static Collection<String> requiredHeaders;
    private static BasicRequest exceptionRequest;

    static {
        requiredHeaders = new ArrayList<>();
        requiredHeaders.add("host");
        requiredHeaders.add("remote-addr");
		BasicRequest r = new BasicRequest();
		r.protocol = "HTTP/ 1.1";
		r.headers = new HashMap<>();
		r.headers.put("connection", "close");
		exceptionRequest = r;
    }
    
    private BasicRequest() {};
	
    public static BasicRequest getBasicRequestExceptBody(String url, Map<String, String> headers,
            Map<String, List<String>> params) {
        // check compulsory headers
        for (String header : requiredHeaders) {
            if (!headers.containsKey(header)) {
                throw new IllegalArgumentException("Missing header: " + header);
            }
        }
        // create request object
        BasicRequest request = new BasicRequest();
        // set request method
        String method = headers.get("Method").split(";")[0];
        request.method = HttpMethod.parse(method);
        // get version of the request
        String protocolVersion = headers.get("protocolVersion").split(";")[0];
        if ("HTTP/1.1".equals(protocolVersion) || "HTTP/1.2".equals(protocolVersion)) {
            request.protocol = protocolVersion;
        } else {
            throw new IllegalArgumentException("Unsupported Http protocol verison");
        }
        // get the URL of the request
        if (!isValidURL(url)) {
            throw new IllegalArgumentException("Illegal URL");
        }
        request.url = url;

        request.headers = new HashMap<String, String>(headers.size());
        // parse host header:
        // get the port number
        String host = headers.get("host").split(";")[0];
        int idx = host.lastIndexOf(':');
        if (idx != -1) {
            if (host.endsWith("/")) {
                request.port = Integer.parseInt(host.substring(idx + 1, host.length() - 1));
            } else {
                request.port = Integer.parseInt(host.substring(idx + 1));
            }

        } else {
        	request.port = 8080;
        }
        
        // get the requested path from root
        int start = url.lastIndexOf(host) + 1;
        int end = url.indexOf('?');
        if (end == -1)
            end = url.length();
        if (start >= end) {
            throw new IllegalArgumentException("malformat url or host");
        }
        request.headers.put("pathinfo", url.substring(start, end));
        // check if is persistent connection
        if (request.protocol().equals("HTTP/1.1") && !(request.headers().contains("connection")
                && request.headers("connection").toLowerCase().equals("close"))) {
            request.persistentConnection(true);
        }
        // put the rest of headers in the requst's header
        for (Map.Entry<String, String> ent : headers.entrySet()) {
            if (!request.headers.containsKey(ent.getKey())) {
                request.headers.put(ent.getKey(), ent.getValue().split(";")[0]);
            }
        }
        
        headers.putIfAbsent("content-type", "text/plain");
        if (headers.containsKey("content-length")) {
            request.contentLength = Integer.parseInt(headers.get("content-length"));
        } else {
        	request.contentLength = 0;
        }
        
        return request;
    }
    
    public static BasicRequest getRequestForException() {
    	return exceptionRequest;
    }

    private static boolean isValidURL(String url) {
        return true;
    }
    
    public void addBody(InputStream in) throws IOException {
    	if (this.headers.containsKey("transfer-encoding")
                && "chunked".equals(this.headers.get("transfer-encoding").toLowerCase())) {
            logger.info("parsing chunked req: " + this);
            this.body = parseChunkedEncodingBdoy(in);
        } else {
            logger.info("parsing normal req: " + this);
            String b = parsePlainBody(in);
            System.out.println(b);
            this.body = b;
        }
    }
    
    private static String parseChunkedEncodingBdoy(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        // skip the empty lines
        do {
            line = reader.readLine();
        } while (line != null && (line.equals("\n") || line.equals("\r\n")));
        // if the line is null, return empty body
        if (line == null)
            return "";

        // read the body
        char[] cbuf = new char[2048];
        while (true) {
            // get the length of the following chunk
            int length = Integer.parseInt(line.split(";")[0]);
            // finish reading if length is 0
            if (length == 0) {
                // skip all the footers
                while (line != null && !(line.equals("\n") || line.equals("\r\n"))) {
                    line = reader.readLine();
                }
                return sb.toString();
            }
            // append the chunk and read new line
            if (length > cbuf.length) {
                cbuf = new char[length];
            }
            reader.read(cbuf, 0, length);
            sb.append(cbuf, 0, length);
            line = reader.readLine();
        }
    }

    private static String parsePlainBody(InputStream in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
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
		return body;
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
		return "" + method + " " + url + protocol + "\n" + headers + "\n" + body;
	}

}
