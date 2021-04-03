package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.append;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.maedle.transform.util.jar.JarModifier;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;
import com.github.pfichtner.maedle.transform.util.jar.ToJarAdder;
import com.pfichtner.github.maedle.transform.TransformationParameters;

class CanTransformMavenMojoJarTest {

	@Test
	void canTransformMavenMojoJarToGradlePlugin(@TempDir File tmpDir) throws Exception {
		File inJar = new File(tmpDir, "in.jar");
		File outJar = new File(tmpDir, "out.jar");

		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greeting");

		// TODO when transforming: switchable "overwrite" flag for the jar
		transform(fillJar(inJar), outJar, t -> pluginInfo);
		Set<String> classNamesOfInJar = collectJarContents(inJar).keySet();

		String pkgName = "com.github.pfichtner.greeter.mavenplugin.";
		String internal = "/" + pkgName.replace('.', '/');

		List<String> filenamesAdded = asList( //
				"/META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties", //
				internal + "GreeterMojoGradlePlugin.class", //
				internal + "GreeterMojoRewritten.class", //
				internal + "GreeterMojoGradlePluginExtension.class" //
		);

		assertThat(collectJarContents(outJar)).hasSize(classNamesOfInJar.size() + filenamesAdded.size()) //
				.containsKeys(classNamesOfInJar.toArray(new String[classNamesOfInJar.size()])) //
				.containsKeys(filenamesAdded.toArray(new String[filenamesAdded.size()])) //
		;

		verifyCanLoadClass(outJar, pkgName + "GreeterMojoGradlePlugin");
		verifyTransformed(tmpDir, outJar, pluginInfo);
	}

	private static void verifyCanLoadClass(File outJar, String name) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException, MalformedURLException {
		try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { outJar.toURI().toURL() })) {
			urlClassLoader.loadClass(name).newInstance();
		}
	}

	private static void verifyTransformed(File tmpDir, File pluginJar, PluginInfo pluginInfo)
			throws IOException, FileNotFoundException {
		File testProjectDir = Files.createTempDirectory(tmpDir.toPath(), "gradle-build-").toFile();

		createProjectSettingsFile(testProjectDir);
		String greeter = "JAR Transformer";
		String message = "JAR is working";
		String taskName = GreeterMojo.GOAL;
		createProjectBuildFile(testProjectDir, pluginInfo,
				new GreeterMessageBuilder().withGreeter(greeter).withMessage(message).build());
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, taskName);
			assertThat(stdOut) //
					.contains("> Task :" + taskName) //
					.contains("Hello, " + greeter) //
					.contains("I have a message for you: " + message) //
			;
		}
	}

	private static Map<String, byte[]> collectJarContents(File jar) throws IOException {
		Map<String, byte[]> content = new HashMap<>();
		try (JarModifier jarReader = new JarModifier(jar, false)) {
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

	private static File fillJar(File jar) throws IOException, FileNotFoundException, URISyntaxException {
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

	private static void addClass(JarModifier jarWriter, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarWriter.add(asStream(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private void transform(File jar, File outJar, Function<Type, PluginInfo> infoProvider) throws IOException {
		try (JarModifier reader = new JarModifier(jar, false)) {
			try (JarModifier writer = new JarModifier(outJar, true)) {
				reader.readJar(visitor(reader.getFileSystem().getPathMatcher("glob:**.class"), new ToJarAdder(writer),
						infoProvider));
			}
		}
	}

	private SimpleFileVisitor<Path> visitor(PathMatcher matcher, ToJarAdder adder,
			Function<Type, PluginInfo> infoProvider) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				byte[] content = read(file);

				String path = file.toString();
				adder.add(content, path.startsWith("/") ? path.substring(1) : path);

				if (matcher.matches(file)) {
					TransformationParameters parameters = fromMojo(content);
					if (parameters.getMojoData().isMojo()) {
						Type originalMojoType = parameters.getMojoClass();
						adder.add(parameters.withMojoClass(append(originalMojoType, "Rewritten")),
								append(originalMojoType, "GradlePlugin"), infoProvider.apply(originalMojoType));
					}
				}
				return CONTINUE;
			}

		};
	}

	private static byte[] read(Path file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(file, baos);
		return baos.toByteArray();
	}
}
