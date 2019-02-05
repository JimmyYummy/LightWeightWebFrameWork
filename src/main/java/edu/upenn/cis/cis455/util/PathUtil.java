package edu.upenn.cis.cis455.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public abstract class PathUtil {
	private static Path unimatcher = Paths.get("*");
	
	public static boolean checkPermission(Path requestPath) {
		return requestPath.startsWith("etc/passwd");
	}

	public static boolean checkPathMatch(Path webPath, Path requestPath) {
		return checkPathMatch(0, 0, webPath, requestPath, new HashSet<>());
	}

	private static boolean checkPathMatch(int fIdx, int rIdx, Path fPath, Path rPath, Set<Path> params) {
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
		String eleStr = fPathEle.toString();
		if (eleStr.startsWith(":")) {
			if (eleStr.length() == 1) {
				throw new IllegalArgumentException("Bad filter / router path: \":\" without name");
			}
			if (params.contains(fPathEle)) {
				throw new IllegalArgumentException("Bad filter / router path: duplicate parameter");
			}
			params.add(fPathEle);
			if (checkPathMatch(fIdx + 1, rIdx + 1, fPath, rPath, params)) {
				return true;
			} else {
				params.remove(fPathEle);
				return false;
			}
		} else if (! fPathEle.equals(rPath.getName(rIdx))) {
			return false;
		}
		return checkPathMatch(fIdx + 1, rIdx + 1, fPath, rPath, params);
	}
}
