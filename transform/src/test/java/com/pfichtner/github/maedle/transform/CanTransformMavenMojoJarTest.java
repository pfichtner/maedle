package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.ClassUtils.asFile;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.pfichtner.github.maedle.transform.uti.jar.JarBuilder;
import com.pfichtner.github.maedle.transform.uti.jar.JarReader;

class CanTransformMavenMojoJarTest {

	@Test
	void testName(@TempDir File tmpDir) throws Exception {
		File jar = new File(tmpDir, "test.jar");
		try (JarBuilder jarBuilder = new JarBuilder(new FileOutputStream(jar))) {
			addClass(jarBuilder, GreeterMojo.class);
		}
		transform(jar);
	}

	private void addClass(JarBuilder jarBuilder, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarBuilder.add(asFile(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private void transform(File jar) throws IOException {
		new JarReader(jar).readJar(visitor());
		fail("not implemented yet");
	}

	private SimpleFileVisitor<Path> visitor() {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				byte[] bytes = content(file);
				System.out.println(file + " has length " + bytes.length);
				return CONTINUE;
			}

			private byte[] content(Path file) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				copy(file, baos);
				return baos.toByteArray();
			}

		};
	}

}
