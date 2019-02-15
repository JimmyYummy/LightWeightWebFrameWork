package edu.upenn.cis.cis455.m1.server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Iterator;

public class TimedPath implements Path {
	Path p;
	long time;
	
	public TimedPath(String path) {
		p = Paths.get("/" + path).normalize();
		time = Instant.now().toEpochMilli();
	}
	
	public TimedPath(Path p) {
		this.p = p;
		time = Instant.now().toEpochMilli();
	}
	
	public TimedPath(long time, Path p) {
		this.p = p;
		this.time = time;
	}

	@Override
	public FileSystem getFileSystem() {
		return p.getFileSystem();
	}

	@Override
	public boolean isAbsolute() {
		return p.isAbsolute();
	}

	@Override
	public Path getRoot() {
		return p.getRoot();
	}

	@Override
	public Path getFileName() {
		return p.getFileName();
	}

	@Override
	public Path getParent() {
		return p.getParent();
	}

	@Override
	public int getNameCount() {
		return p.getNameCount();
	}

	@Override
	public Path getName(int index) {
		return p.getName(index);
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		return p.subpath(beginIndex, endIndex);
	}

	@Override
	public boolean startsWith(Path other) {
		return p.startsWith(other);
	}

	@Override
	public boolean startsWith(String other) {
		return p.startsWith(other);
	}

	@Override
	public boolean endsWith(Path other) {
		return p.endsWith(other);
	}

	@Override
	public boolean endsWith(String other) {
		return p.endsWith(other);
	}

	@Override
	public Path normalize() {
		return new TimedPath(time, p.normalize());
	}

	@Override
	public Path resolve(Path other) {
		return p.resolve(other);
	}

	@Override
	public Path resolve(String other) {
		return p.resolve(other);
	}

	@Override
	public Path resolveSibling(Path other) {
		return p.resolveSibling(other);
	}

	@Override
	public Path resolveSibling(String other) {
		return p.resolveSibling(other);
	}

	@Override
	public Path relativize(Path other) {
		return p.relativize(other);
	}

	@Override
	public URI toUri() {
		return p.toUri();
	}

	@Override
	public Path toAbsolutePath() {
		return p.toAbsolutePath();
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return p.toRealPath(options);
	}

	@Override
	public File toFile() {
		return p.toFile();
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		return p.register(watcher, events, modifiers);
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		return p.register(watcher, events);
	}

	@Override
	public Iterator<Path> iterator() {
		return p.iterator();
	}

	@Override
	public int compareTo(Path other) {
		if (other instanceof TimedPath) {
			TimedPath to = (TimedPath) other;
			if (to.time > this.time) return -1;
			if (to.time == this.time) return 0;
		}
		return 1;
	}
	
	@Override
	public int hashCode() {
		return this.p.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof TimedPath)) return false;
		TimedPath to = (TimedPath) o;
		return this.p.equals(to.p);
	}

}
