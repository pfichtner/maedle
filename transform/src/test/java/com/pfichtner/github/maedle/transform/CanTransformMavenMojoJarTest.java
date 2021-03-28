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
import java.nio.file.PathMatcher;
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
		transform(addContentToJar(new File(tmpDir, "test.jar")));
	}

	private File addContentToJar(File jar) throws IOException, FileNotFoundException, URISyntaxException {
		try (JarBuilder jarBuilder = new JarBuilder(new FileOutputStream(jar))) {
			addClass(jarBuilder, nonMojoClass());
			addClass(jarBuilder, mojoClass());
		}
		return jar;
	}

	private static Class<CanTransformMavenMojoJarTest> nonMojoClass() {
		return CanTransformMavenMojoJarTest.class;
	}

	private static Class<GreeterMojo> mojoClass() {
		return GreeterMojo.class;
	}

	private void addClass(JarBuilder jarBuilder, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarBuilder.add(asFile(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private void transform(File jar) throws IOException {
		JarReader jarReader = new JarReader(jar);
		jarReader.readJar(visitor(jarReader.getFileSystem().getPathMatcher("glob:**.class")));
		fail("not implemented yet");
	}

	private SimpleFileVisitor<Path> visitor(PathMatcher matcher) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(file)) {
					TransformationParameters parameters = new TransformationParameters(read(file));
					if (parameters.getMojoData().isMojo()) {
						TransformationResult result = new TransformationResult(parameters);
						byte[] transformedMojo = result.getTransformedMojo();
						byte[] extension = result.getExtension();
						System.out.println(parameters.getMojoData().getMojoType().getClassName());
						System.out.println(transformedMojo.length);
						System.out.println(extension.length);
					}
				}
				return CONTINUE;
			}

			private byte[] read(Path file) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				copy(file, baos);
				byte[] byteArray = baos.toByteArray();
				return byteArray;
			}

		};
	}

}
