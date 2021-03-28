package com.pfichtner.github.maedle.transform.uti.jar;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
				newEntry(source, name.endsWith("/") ? name : name + "/");
				closeEntry();
			}
			for (File nestedFile : source.listFiles()) {
				add(nestedFile);
			}
		} else {
			newEntry(source, name);
			writeToTarget(source);
			closeEntry();
		}
	}

	private void newEntry(File source, String name) throws IOException {
		JarEntry entry = new JarEntry(name);
		entry.setTime(source.lastModified());
		target.putNextEntry(entry);
	}

	private void closeEntry() throws IOException {
		target.closeEntry();
	}

	private void writeToTarget(File source) throws IOException, FileNotFoundException {
		try (FileInputStream inputStream = new FileInputStream(source)) {
			IoUtils.copy(inputStream, target);
		}
	}

	@Override
	public void close() throws IOException {
		target.close();
	}

}
