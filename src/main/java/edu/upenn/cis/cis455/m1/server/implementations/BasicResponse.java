package edu.upenn.cis.cis455.m1.server.implementations;

import edu.upenn.cis.cis455.m1.server.interfaces.Response;
import edu.upenn.cis.cis455.util.HttpParsing;

public class BasicResposne extends Response {
	private String protocolVersion;
	
	public BasicResposne() {
		statusCode = -1;
	}

	@Override
	public String getHeaders() {
		if (protocolVersion == null) {
			throw new IllegalStateException("Undefined protocol version");
		}
		return String.format("%s %d %s", protocolVersion, statusCode, HttpParsing.explainStatus(statusCode));
	}
	
	public void setVersion(String protocolVersion) {
		if ("HTTP/1.1".equals(protocolVersion) || "HTTP/1.0".equals(protocolVersion)) {
			this.protocolVersion = protocolVersion;
		} else {
			throw new IllegalArgumentException("Illegal version: " + protocolVersion);
		}
	}

}
