package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.ClassUtils.asFile;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.uti.jar.JarBuilder;
import com.pfichtner.github.maedle.transform.uti.jar.JarReader;

class CanTransformMavenMojoJarTest {

	@Test
	void canTransformMavenMojoJarToGradlePlugin(@TempDir File tmpDir) throws Exception {
		File jarFile = new File(tmpDir, "test.jar");
		transform(addContentToJar(jarFile), new ByteArrayOutputStream());

		// we COULD assert if the plugin is written correct
		// we COULD assert if META-INF was written
		// better: spawn process and let it run!

		// TODO the real mojo class already was loaded by the thread classloader :-/
		try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() })) {
			Class<?> mojo = urlClassLoader.loadClass("com.github.pfichtner.greeter.mavenplugin.GreeterMojo");
			Arrays.stream(mojo.getDeclaredConstructors()).forEach(System.out::println);
			Arrays.stream(mojo.getDeclaredMethods()).forEach(System.out::println);
		}

		// when transforming: switchable "overwrite" flag for the jar

		try (JarReader jarReader = new JarReader(jarFile)) {
			jarReader.readJar(new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
					System.out.println(path);
					return CONTINUE;
				}
			});
		}

		fail("not implemented yet");
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

	private void transform(File jar, OutputStream outputStream) throws IOException {
		try (JarReader jarReader = new JarReader(jar)) {
			try (JarBuilder jarWriter = new JarBuilder(outputStream)) {
				jarReader.readJar(visitor(jarReader.getFileSystem().getPathMatcher("glob:**.class"), jarWriter));
			}
		}
	}

	private SimpleFileVisitor<Path> visitor(PathMatcher matcher, JarBuilder jarWriter) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				byte[] bytes = read(path);
				if (matcher.matches(path)) {
					writeTransformed(jarWriter, bytes);
				} else {
					String fileName = path.getFileName().toString();
					jarWriter.addEntry(new JarEntry(fileName), new ByteArrayInputStream(bytes));
				}
				return CONTINUE;
			}

			private void writeTransformed(JarBuilder jarWriter, byte[] bytes)
					throws IOException, FileNotFoundException {
				TransformationParameters parameters = new TransformationParameters(bytes);
				MojoData mojoData = parameters.getMojoData();
				if (mojoData.isMojo()) {
					TransformationResult result = new TransformationResult(parameters);
					jarWriter.addEntry(new JarEntry(parameters.getExtensionClass().getClassName()),
							new ByteArrayInputStream(result.getTransformedMojo()));
					jarWriter.addEntry(new JarEntry(mojoData.getMojoType().getClassName()),
							new ByteArrayInputStream(result.getExtension()));
					// TODO Add META-INF entry here
				}
			}

			private byte[] read(Path file) throws IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				copy(file, baos);
				return baos.toByteArray();
			}

		};
	}

}
