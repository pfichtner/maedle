package com.pfichtner.github.maedle.transform.uti.jar;

import static java.nio.file.Files.walkFileTree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JarReader implements Closeable {

	private final FileSystem fileSystem;

	public JarReader(File jar) throws IOException {
		this(jar.toPath());
	}

	public JarReader(Path jar) throws IOException {
		this(getFileSystem(jar));
	}

	private static FileSystem getFileSystem(Path jar) throws IOException {
		URI uri = URI.create("jar:file:" + jar.toUri().getPath());
		try {
			return FileSystems.newFileSystem(uri, jarProperties());
		} catch (FileSystemAlreadyExistsException e) {
			// no way to use the API other than this :-/
			return FileSystems.getFileSystem(uri);
		}
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

	@Override
	public void close() throws IOException {
		fileSystem.close();
	}

}