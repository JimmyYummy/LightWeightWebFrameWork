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

	public static final Request initialRequest;

	private HttpMethod method;
	private String url;
	private String protocol;
	private Map<String, String> headers;
	private String body;

	static {
		initialRequest = new BasicRequest();

	}

	private BasicRequest() {

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
		return Integer.parseInt(headers.get("port"));
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
		return Integer.parseInt(headers.get("content-length"));
	}

	@Override
	public String headers(String name) {
		return headers.get(name);
	}

	@Override
	public Set<String> headers() {
		return headers.keySet();
	}

	public static class BasicRequestFactory {

		private static Collection<String> requiredHeaders;

		static {
			requiredHeaders = new ArrayList<>();
			requiredHeaders.add("host");
			requiredHeaders.add("useragent");
			requiredHeaders.add("content-type");
			requiredHeaders.add("content-length");
			requiredHeaders.add("remote-addr");
		}

		public static Request getBasicRequest(String uri, InputStream in, Map<String, String> headers,
				Map<String, List<String>> parms) throws HaltException {
			BasicRequest request = getBasicRequestExceptBody(headers.get("Method"), uri, headers.get("protocolVersion"),
					headers, parms);
			try {
				if (headers.containsKey("transfer-encoding")
						&& "chunked".equals(headers.get("transfer-encoding").toLowerCase())) {
					request.body = parseChunkedEncodingBdoy(in);
				} else {
					request.body = parsePlainBody(in);
				}
			} catch (Exception e) {
				throw new HaltException("Server: bad body parsing");
			}
			return request;
		}

		private static String parseChunkedEncodingBdoy(InputStream in) throws IOException {
			StringBuilder sb = new StringBuilder();
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			while (true) {
				line = reader.readLine();
				if (Integer.parseInt(line.split(";")[0]) == 0) {
					while (!line.equals("\n") || line.equals("\r\n")) {
						line = reader.readLine();
					}
					return sb.toString();
				}
				sb.append(reader.readLine());
			}
		}

		private static String parsePlainBody(InputStream in) throws IOException {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = new BufferedReader(new InputStreamReader(in)).readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}

		private static BasicRequest getBasicRequestExceptBody(String method, String url, String protocolVersion,
				Map<String, String> headers, Map<String, List<String>> params) {
			BasicRequest request = new BasicRequest();
			request.method = HttpMethod.parse(method);
			if ("HTTP/1.1".equals(protocolVersion) || "HTTP/1.2".equals(protocolVersion)) {
				request.protocol = protocolVersion;
			} else {
				throw new IllegalArgumentException("Unsupported Http protocol verison");
			}
			if (!isValidURL(url)) {
				throw new IllegalArgumentException("Illegal URL");
			}
			request.url = url;
			for (String header : requiredHeaders) {
				if (!headers.containsKey(header)) {
					throw new IllegalArgumentException("Missing header: " + header);
				}
			}
			request.headers = new HashMap<String, String>(headers);
			String port = "80";
			String host = headers.get("host");
			int idx = host.lastIndexOf(':');
			if (idx != -1) {
				port = host.substring(idx + 1);
			}
			request.headers.put("port", port);

			int start = url.lastIndexOf(host) + 1;
			int end = url.indexOf('?');
			if (end == -1)
				end = url.length();
			if (start >= end) {
				throw new IllegalArgumentException("malformat url or host");
			}
			String pathInfo = url.substring(start, end);
			request.headers.put("pathinfo", pathInfo);
			return request;
		}

		private static boolean isValidURL(String url) {
			return false;
		}

	}

}
