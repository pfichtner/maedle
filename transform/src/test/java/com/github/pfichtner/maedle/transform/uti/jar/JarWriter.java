package com.github.pfichtner.maedle.transform.uti.jar;

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

public class JarWriter implements Closeable {

	private JarOutputStream target;

	public JarWriter(OutputStream outputStream, boolean writeManifest) throws IOException {
		target = writeManifest //
				? new JarOutputStream(outputStream) //
				: new JarOutputStream(outputStream, createManifest());
	}

	private Manifest createManifest() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		return manifest;
	}

	public void add(File source) throws IOException {
		add(source, source.getPath());
	}

	public void add(File source, String name) throws IOException, FileNotFoundException {
		if (source.isDirectory()) {
			if (!name.isEmpty()) {
				JarEntry entry = new JarEntry(name.endsWith("/") ? name : name + "/");
				entry.setTime(source.lastModified());
				putToTarget(entry);
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
		putToTarget(entry);
		writeToTarget(inputStream);
		closeEntry();
	}

	private void putToTarget(JarEntry entry) throws IOException {
		target.putNextEntry(entry);
	}

	private void writeToTarget(InputStream inputStream) throws IOException, FileNotFoundException {
		IoUtils.copy(inputStream, target);
	}

	private void closeEntry() throws IOException {
		target.closeEntry();
	}

	@Override
	public void close() throws IOException {
		target.close();
	}

}
