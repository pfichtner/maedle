package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.ClassUtils.asFile;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.maedle.transform.uti.jar.JarReader;
import com.github.pfichtner.maedle.transform.uti.jar.JarWriter;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

class CanTransformMavenMojoJarTest {

	@Test
	void canTransformMavenMojoJarToGradlePlugin(@TempDir File tmpDir) throws Exception {
		File inJar = new File(tmpDir, "in.jar");
		File outJar = new File(tmpDir, "out.jar");
		File creatInJar = fillJar(inJar);
		try (OutputStream outputStream = new FileOutputStream(outJar)) {
			transform(creatInJar, outputStream);
		}

		// TODO when transforming: switchable "overwrite" flag for the jar

		assertThat(collectJarContents(outJar)).hasSize(4).containsKeys( //
				"/META-INF/MANIFEST.MF", //
				"/com/github/pfichtner/greeter/mavenplugin/GreeterMojo.class",
				"/com/github/pfichtner/greeter/mavenplugin/GreeterMojoGradlePluginExtension.class", //
				"/com/github/pfichtner/maedle/transform/CanTransformMavenMojoJarTest.class" //
		);

		// we COULD assert if the plugin is written correct
		// we COULD assert if META-INF was written
		// better: spawn process and let it run!

		try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { outJar.toURI().toURL() })) {
			String mojoClassname = "com.github.pfichtner.greeter.mavenplugin.GreeterMojo";
			// TODO the real mojo class already was loaded by the thread classloader :-/
			Class<?> mojo = urlClassLoader.loadClass(mojoClassname);
			Arrays.stream(mojo.getDeclaredConstructors()).forEach(System.out::println);
			Arrays.stream(mojo.getDeclaredMethods()).forEach(System.out::println);
			System.out.println("+++");
			Class<?> extension = urlClassLoader.loadClass(mojoClassname + "GradlePluginExtension");
			Arrays.stream(extension.getDeclaredConstructors()).forEach(System.out::println);
			Arrays.stream(extension.getDeclaredMethods()).forEach(System.out::println);
			System.out.println("+++");
		}

		fail("not implemented yet");
	}

	private Map<String, byte[]> collectJarContents(File transformedJarFile) throws IOException {
		Map<String, byte[]> content = new HashMap<>();
		try (JarReader jarReader = new JarReader(transformedJarFile)) {
			jarReader.readJar(new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
					String key = path.toString();
					if (content.put(key, read(path)) != null) {
						throw new IllegalStateException("Duplicate entry for " + key);
					}
					return CONTINUE;
				}
			});
		}
		return content;
	}

	private File fillJar(File jar) throws IOException, FileNotFoundException, URISyntaxException {
		try (JarWriter jarBuilder = new JarWriter(new FileOutputStream(jar), true)) {
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

	private void addClass(JarWriter jarBuilder, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarBuilder.add(asFile(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private void transform(File jar, OutputStream outputStream) throws IOException {
		try (JarReader jarReader = new JarReader(jar)) {
			try (JarWriter jarWriter = new JarWriter(outputStream, false)) {
				jarReader.readJar(visitor(jarReader.getFileSystem().getPathMatcher("glob:**.class"), jarWriter));
			}
		}
	}
	
	private static SimpleFileVisitor<Path> visitor(PathMatcher matcher, JarWriter jarWriter) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				byte[] content = read(file);
				boolean transformed = false;
				if (matcher.matches(file)) {
					// TODO use OO -> JaEntry[] entries getTransformer().transform(...)
					TransformationParameters parameters = new TransformationParameters(content);
					MojoData mojoData = parameters.getMojoData();
					if (mojoData.isMojo()) {
						writeTransformed(jarWriter, parameters, mojoData);
						transformed = true;
					}
				}
				if (!transformed) {
					String path = file.toString();
					JarEntry entry = new JarEntry(path.startsWith("/") ? path.substring(1) : path);
					// entry.setTime(path.);
					jarWriter.addEntry(entry, new ByteArrayInputStream(content));
				}
				return CONTINUE;
			}

			private void writeTransformed(JarWriter jarWriter, TransformationParameters parameters, MojoData mojoData)
					throws IOException, FileNotFoundException {
				TransformationResult result = new TransformationResult(parameters);
				// entry.setTime(path.);
				jarWriter.addEntry(new JarEntry(toPath(mojoData.getMojoType())),
						new ByteArrayInputStream(result.getTransformedMojo()));
				jarWriter.addEntry(new JarEntry(toPath(parameters.getExtensionClass())),
						new ByteArrayInputStream(result.getExtension()));
				// TODO Add META-INF entry here
			}

			private String toPath(Type type) {
				return type.getInternalName() + ".class";
			}

		};
	}

	private static byte[] read(Path file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(file, baos);
		return baos.toByteArray();
	}
}
