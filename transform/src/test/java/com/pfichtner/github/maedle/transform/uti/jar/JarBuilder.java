package com.pfichtner.github.maedle.transform.uti.jar;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.pfichtner.github.maedle.transform.util.IoUtils;

public class JarBuilder implements Closeable {

	private JarOutputStream target;

	public JarBuilder(OutputStream outputStream) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		target = new JarOutputStream(outputStream, manifest);
	}

	public void add(File source) throws IOException {
		add(source, source.getPath());
	}

	public void add(File source, String name) throws IOException, FileNotFoundException {
		if (source.isDirectory()) {
			if (!name.isEmpty()) {
				JarEntry entry = new JarEntry(name.endsWith("/") ? name : name + "/");
				entry.setTime(source.lastModified());
				target.putNextEntry(entry);
				closeEntry();
			}
			for (File nestedFile : source.listFiles()) {
				add(nestedFile);
			}
		} else {
			addEntry(source, name);
		}
	}

	private void addEntry(File source, String name) throws IOException, FileNotFoundException {
		JarEntry entry = new JarEntry(name);
		entry.setTime(source.lastModified());
		try (FileInputStream inputStream = new FileInputStream(source)) {
			addEntry(entry, inputStream);
		}
	}

	public void addEntry(JarEntry entry, InputStream inputStream) throws IOException, FileNotFoundException {
		target.putNextEntry(entry);
		writeToTarget(inputStream);
		closeEntry();
	}

	private void closeEntry() throws IOException {
		target.closeEntry();
	}

	private void writeToTarget(InputStream inputStream) throws IOException, FileNotFoundException {
		IoUtils.copy(inputStream, target);
	}

	@Override
	public void close() throws IOException {
		target.close();
	}

}
