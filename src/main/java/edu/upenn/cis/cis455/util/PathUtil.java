package edu.upenn.cis.cis455.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m2.server.interfaces.Request;

public abstract class PathUtil {
	private static Path unimatcher = Paths.get("*");
	
	public static boolean checkReadPermission(Path requestPath) {
		return true;//requestPath.toFile().canRead();
	}
	
	public static boolean checkWritePermission(Path requestPath) {
		return true;//requestPath.toFile().canWrite();
	}

	public static boolean checkPathMatch(Path webPath, Path requestPath, Request req) {
		
		return checkPathMatch(0, 0, webPath, requestPath, req.params());
	}

	private static boolean checkPathMatch(int fIdx, int rIdx, Path fPath, Path rPath, Map<String, String> params) {
		if (fIdx == fPath.getNameCount() && rIdx == rPath.getNameCount())
			return true;
		if (fIdx == fPath.getNameCount() || rIdx == rPath.getNameCount())
			return false;
		Path fPathEle = fPath.getName(fIdx);
		if (fPathEle.equals(unimatcher)) {
			for (int i = rIdx + 1; i <= rPath.getNameCount(); i++) {
				if (checkPathMatch(fIdx + 1, i, fPath, rPath, params))
					return true;
			}
			return false;
		}
		String fEleStr = fPathEle.toString();
		if (fEleStr.startsWith(":")) {
			if (fEleStr.length() == 1) {
				throw new HaltException(400, "Bad filter / router path: \":\" without name");
			}
			if (params.containsKey(fEleStr)) {
				throw new HaltException(400, "Bad filter / router path: duplicate route parameter");
			}
			params.put(fEleStr, rPath.getName(rIdx).toString());
			if (checkPathMatch(fIdx + 1, rIdx + 1, fPath, rPath, params)) {
				return true;
			} else {
				params.remove(fEleStr);
				return false;
			}
		} else if (! fPathEle.equals(rPath.getName(rIdx))) {
			return false;
		}
		return checkPathMatch(fIdx + 1, rIdx + 1, fPath, rPath, params);
	}
}
