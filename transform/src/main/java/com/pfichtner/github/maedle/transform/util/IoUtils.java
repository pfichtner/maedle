package com.pfichtner.github.maedle.transform.util;

import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.classToPath;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public final class IoUtils {

	private IoUtils() {
		super();
	}

	public static byte[] toBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		copy(inputStream, buf);
		return buf.toByteArray();
	}

	public static void copy(InputStream inputStream, OutputStream buf) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(inputStream);
		for (int result = bis.read(); result != -1; result = bis.read()) {
			buf.write((byte) result);
		}
	}

	public static void writeFile(File destination, String content) throws IOException {
		try (BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
			output.write(content);
		}
	}

	public static void writeFile(File destination, byte[] content) throws IOException {
		try (InputStream is = new ByteArrayInputStream(content)) {
			writeFile(destination, is);
		}
	}

	public static void writeFile(File destination, InputStream is) throws IOException {
		try (OutputStream os = new FileOutputStream(destination)) {
			copy(is, os);
		}
	}

	public static File ensureDirectoryExists(File directory) {
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IllegalStateException("Cannot create " + directory);
		}
		return directory;
	}

	public static void addClass(File baseDir, Class<?> clazz) throws IOException {
		File file = new File(baseDir, classToPath(clazz));
		ensureDirectoryExists(file.getParentFile());
		writeFile(file, asStream(clazz));
	}

	public static Map<String, byte[]> directoryContent(File directory) throws IOException {
		Map<String, byte[]> content = new HashMap<>();
		walkFileTree(directory.toPath(), collectToMap(content));
		return content;
	}

	public static String stripBaseDir(File baseDir, String filename) {
		return filename.substring(baseDir.toString().length() + 1);
	}

	public static SimpleFileVisitor<Path> copyTree(File srcPath, File destPath) {
		return new CopyDirVisitor(srcPath.toPath(), destPath.toPath(), REPLACE_EXISTING);
	}

	private static class CopyDirVisitor extends SimpleFileVisitor<Path> {

		private final Path fromPath;
		private final Path toPath;
		private StandardCopyOption copyOption;

		public CopyDirVisitor(Path fromPath, Path toPath, StandardCopyOption copyOption) {
			this.fromPath = fromPath;
			this.toPath = toPath;
			this.copyOption = copyOption;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path targetPath = toPath.resolve(fromPath.relativize(dir));
			if (!Files.exists(targetPath)) {
				Files.createDirectory(targetPath);
			}
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
			return CONTINUE;
		}
	}

	public static SimpleFileVisitor<Path> collectToMap(Map<String, byte[]> content) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				String key = path.toString();
				if (content.put(key, read(path)) != null) {
					throw new IllegalStateException("Duplicate entry for " + key);
				}
				return CONTINUE;
			}

			private byte[] read(Path file) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Files.copy(file, baos);
				return baos.toByteArray();
			}
		};

	}

}
