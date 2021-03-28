package com.pfichtner.github.maedle.transform.uti.jar;

import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.walkFileTree;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JarReader {

	private Path jar;

	public JarReader(File jar) {
		this(jar.toPath());
	}

	public JarReader(Path jar) {
		this.jar = jar;
	}

	public void readJar(FileVisitor<Path> visitor) throws IOException {
		readJar(newFileSystem(URI.create("jar:file:" + jar.toUri().getPath()), jarProperties()), visitor);
	}

	private static void readJar(FileSystem jarfs, FileVisitor<Path> visitor) throws IOException {
		walkFileTree(jarfs.getPath("/"), visitor);
	}

	private static Map<String, String> jarProperties() {
		Map<String, String> jarProperties = new HashMap<>();
		jarProperties.put("create", "false");
		jarProperties.put("encoding", "UTF-8");
		return jarProperties;
	}

}