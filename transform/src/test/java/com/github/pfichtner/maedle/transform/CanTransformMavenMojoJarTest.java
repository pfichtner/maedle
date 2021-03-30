package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

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
			// TODO when transforming: switchable "overwrite" flag for the jar
			transform(creatInJar, outputStream);
		}

		String pkgName = "com.github.pfichtner.greeter.mavenplugin.";
		String internal = "/" + pkgName.replace('.', '/');
		assertThat(collectJarContents(outJar)).hasSize(6).containsKeys( //
				"/META-INF/MANIFEST.MF", //
				"/META-INF/gradle-plugins/com.github.pfichtner.gradle.greeting.properties", //
				internal + "GreeterMojoGradlePlugin.class", //
				internal + "GreeterMojoRewritten.class", //
				internal + "GreeterMojoGradlePluginExtension.class", //
				"/com/github/pfichtner/maedle/transform/CanTransformMavenMojoJarTest.class" //
		);

		try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { outJar.toURI().toURL() })) {
			// we COULD assert if the plugin is written correct
			// we COULD assert if META-INF was written
			// better: spawn process and let it run!
			// TODO this should be done in a gradle project with the usage of testkit
			urlClassLoader.loadClass(pkgName + "GreeterMojoGradlePlugin").newInstance();
		}

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

	private static Class<?> nonMojoClass() {
		return CanTransformMavenMojoJarTest.class;
	}

	private static Class<?> mojoClass() {
		return GreeterMojo.class;
	}

	private void addClass(JarWriter jarBuilder, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarBuilder.addEntry(new JarEntry(clazz.getName().replace('.', '/') + ".class"), asStream(clazz));
	}

	private void transform(File jar, OutputStream outputStream) throws IOException {
		try (JarReader jarReader = new JarReader(jar)) {
			try (JarWriter jarWriter = new JarWriter(outputStream, false)) {
				jarReader.readJar(visitor(jarReader.getFileSystem().getPathMatcher("glob:**.class"), jarWriter));
			}
		}
	}

	private SimpleFileVisitor<Path> visitor(PathMatcher matcher, JarWriter jarWriter) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				byte[] content = read(file);
				boolean transformed = false;
				if (matcher.matches(file)) {
					// TODO use OO -> JaEntry[] entries getTransformer().transform(...)
					TransformationParameters parameters = new TransformationParameters(content);
					MojoData mojoData = parameters.getMojoData();
					addMojoRemapping(parameters, mojoData);
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
				jarWriter.addEntry(new JarEntry(toPath(mojoData.getMojoType(), "Rewritten")),
						new ByteArrayInputStream(result.getTransformedMojo()));
				jarWriter.addEntry(new JarEntry(toPath(parameters.getExtensionClass())),
						new ByteArrayInputStream(result.getExtension()));

				String mojoType = mojoData.getMojoType().getInternalName();
				String extensionType = parameters.getExtensionClass().getInternalName();
				String pluginType = mojoType + "GradlePlugin";

				byte[] pluginBytes = createPlugin(pluginType, extensionType, mojoType, "greet", "greeting");
				jarWriter.addEntry(new JarEntry(pluginType + ".class"), new ByteArrayInputStream(pluginBytes));

				// TODO name?
				String n = "com.github.pfichtner.gradle.greeting";
				jarWriter.addEntry(new JarEntry("META-INF/gradle-plugins/" + n + ".properties"),
						new ByteArrayInputStream("implementation-class=pluginType.replace('/', '.')".getBytes()));
			}

			private String toPath(Type type) {
				return type.getInternalName() + ".class";
			}

			private String toPath(Type type, String append) {
				return type.getInternalName() + append + ".class";
			}

		};
	}

	private void addMojoRemapping(TransformationParameters parameters, MojoData mojoData) {
		Remapper remapper = parameters.getRemapper();
		parameters.setRemapper(new Remapper() {
			@Override
			public String map(String internalName) {
				if (mojoData.getMojoType().equals(Type.getObjectType(internalName))) {
					return internalName + "Rewritten";
				} else if (remapper == null) {
					return internalName;
				} else {
					return remapper.map(internalName);
				}
			}
		});
	}

	private static byte[] read(Path file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(file, baos);
		return baos.toByteArray();
	}
}
