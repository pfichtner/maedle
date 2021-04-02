package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.maedle.transform.util.jar.JarModifier;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

class CanTransformMavenMojoJarTest {

	@Test
	void canTransformMavenMojoJarToGradlePlugin(@TempDir File tmpDir) throws Exception {
		File inJar = new File(tmpDir, "in.jar");
		File outJar = new File(tmpDir, "out.jar");
		// TODO when transforming: switchable "overwrite" flag for the jar
		transform(fillJar(inJar), outJar);

		String pkgName = "com.github.pfichtner.greeter.mavenplugin.";
		String internal = "/" + pkgName.replace('.', '/');
		assertThat(collectJarContents(outJar)).hasSize(5).containsKeys( //
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
		try (JarModifier jarReader = new JarModifier(transformedJarFile, false)) {
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
		try (JarModifier jarBuilder = new JarModifier(jar, true)) {
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

	private void addClass(JarModifier jarWriter, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarWriter.add(asStream(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private void transform(File jar, File outJar) throws IOException {
		try (JarModifier jarReader = new JarModifier(jar, false)) {
			try (JarModifier jarWriter = new JarModifier(outJar, true)) {
				jarReader.readJar(visitor(jarReader.getFileSystem().getPathMatcher("glob:**.class"), jarWriter));
			}
		}
	}

	private SimpleFileVisitor<Path> visitor(PathMatcher matcher, JarModifier jarWriter) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				byte[] content = read(file);
				boolean transformed = false;
				if (matcher.matches(file)) {
					// TODO use OO -> JaEntry[] entries getTransformer().transform(...)
					TransformationParameters parameters = fromMojo(content);
					MojoData mojoData = parameters.getMojoData();
					parameters = parameters
							.withMojoClass(Type.getObjectType(mojoData.getMojoType().getInternalName() + "Rewritten"));
					if (mojoData.isMojo()) {
						writeTransformed(jarWriter, parameters, mojoData);
						transformed = true;
					}
				}
				if (!transformed) {
					String path = file.toString();
					jarWriter.add(content, path.startsWith("/") ? path.substring(1) : path);
				}
				return CONTINUE;
			}

			private void writeTransformed(JarModifier jarWriter, TransformationParameters parameters, MojoData mojoData)
					throws IOException, FileNotFoundException {
				TransformationResult result = new TransformationResult(parameters);
				// entry.setTime(path.);
				jarWriter.add(result.getTransformedMojo(), toPath(parameters.getMojoClass()));
				jarWriter.add(result.getExtension(), toPath(parameters.getExtensionClass()));

				String mojoType = mojoData.getMojoType().getInternalName();
				String extensionType = parameters.getExtensionClass().getInternalName();
				String pluginType = mojoType + "GradlePlugin";

				jarWriter.add(createPlugin(pluginType, extensionType, mojoType, "greet", "greeting"),
						pluginType + ".class");

				// TODO pluginId?
				String pluginId = "com.github.pfichtner.gradle.greeting";
				jarWriter.add(
						new ByteArrayInputStream(("implementation-class=" + pluginType.replace('/', '.')).getBytes()),
						("META-INF/gradle-plugins/" + pluginId + ".properties"));
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
