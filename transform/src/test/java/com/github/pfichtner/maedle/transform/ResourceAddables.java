package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.IoUtils.ensureDirectoryExists;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;

import java.io.File;

import com.github.pfichtner.maedle.transform.util.jar.JarModifier;

public final class ResourceAddables {

	private ResourceAddables() {
		super();
	}

	public static ResourceAddable writeToDirectory(File baseDir) {
		return (content, path) -> {
			File file = new File(baseDir, path);
			ensureDirectoryExists(file.getParentFile());
			writeFile(file, content);
		};
	}

	public static ResourceAddable writeToJar(JarModifier writer) {
		return (content, path) -> writer.add(content, path);
	}

}