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

	private final FileSystem fileSystem;

	public JarReader(File jar) throws IOException {
		this(jar.toPath());
	}

	public JarReader(Path jar) throws IOException {
		this(newFileSystem(URI.create("jar:file:" + jar.toUri().getPath()), jarProperties()));
	}

	public JarReader(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public FileSystem getFileSystem() {
		return fileSystem;
	}

	public void readJar(FileVisitor<Path> visitor) throws IOException {
		walkFileTree(fileSystem.getPath("/"), visitor);
	}

	private static Map<String, String> jarProperties() {
		Map<String, String> jarProperties = new HashMap<>();
		jarProperties.put("create", "false");
		jarProperties.put("encoding", "UTF-8");
		return jarProperties;
	}

}