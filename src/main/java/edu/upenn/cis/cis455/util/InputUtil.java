package edu.upenn.cis.cis455.util;

import java.io.IOException;
import java.io.InputStream;

import edu.upenn.cis.cis455.exceptions.HaltException;

public abstract class InputUtil {
	
	public static int skipBlankLines(InputStream in) throws IOException {
		in.mark(200);
		int skipBytes = 0;
		while (true) {
			int b = in.read();
			if (b == -1) {
				throw new HaltException(400, "Unexpected end of the request");
			}
			if (b == 10 || b == 13) {
				skipBytes++;
			} else {
				break;
			}
		}
		in.reset();
		in.skip(skipBytes);
		return skipBytes;
	}
	
	public static boolean reachedEndOfStream(InputStream in) throws IOException {
		in.mark(200);
		boolean reachedEnd = (in.read() == -1);
		in.reset();
		return reachedEnd;
	}
}
