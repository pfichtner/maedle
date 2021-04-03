package com.github.pfichtner.maedle.transform.util.jar;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JarModifier implements Closeable {

	private static final String CREATE = "create";

	private final FileSystem fileSystem;

	public JarModifier(File jar, boolean create) throws IOException {
		this(jar.toPath(), create);
	}

	public JarModifier(Path jar, boolean create) throws IOException {
		this(getFileSystem(jar, create));
	}

	private static FileSystem getFileSystem(Path jar, boolean create) throws IOException {
		URI uri = URI.create("jar:file:" + jar.toUri().getPath());
		try {
			return FileSystems.newFileSystem(uri, modify(defaultProperties(), CREATE, String.valueOf(create)));
		} catch (FileSystemAlreadyExistsException e) {
			// no way to use the API other than this :-/
			return FileSystems.getFileSystem(uri);
		}
	}

	public JarModifier(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public FileSystem getFileSystem() {
		return fileSystem;
	}

	public void readJar(FileVisitor<Path> visitor) throws IOException {
		walkFileTree(fileSystem.getPath("/"), visitor);
	}

	public void add(Path content, String path) throws IOException {
		copy(content, ensureParentExists(path), REPLACE_EXISTING);
	}

	public void add(byte[] content, String path) throws IOException {
		copy(new ByteArrayInputStream(content), ensureParentExists(path), REPLACE_EXISTING);
	}

	public void add(InputStream content, String path) throws IOException {
		copy(content, ensureParentExists(path), REPLACE_EXISTING);
	}

	private Path ensureParentExists(String path) throws IOException {
		Path pathInZip = fileSystem.getPath(path);
		Path parent = pathInZip.getParent();
		if (parent != null) {
			createDirectories(parent);
		}
		return pathInZip;
	}

	private static Map<String, String> defaultProperties() {
		Map<String, String> jarProperties = new HashMap<>();
		jarProperties.put(CREATE, "false");
		jarProperties.put("encoding", "UTF-8");
		return jarProperties;
	}

	private static Map<String, String> modify(Map<String, String> properties, String key, String value) {
		properties.put(key, value);
		return properties;
	}

	@Override
	public void close() throws IOException {
		fileSystem.close();
	}

}