package com.pfichtner.github.maedle.transform.util;

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

}
