package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.ResourceAddables.writeToDirectory;
import static com.github.pfichtner.maedle.transform.ResourceAddables.writeToJar;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.ensureDirectoryExists;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.walkFileTree;
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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
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

class CanTransformMavenMojoJarTest {

	private static final class CollectToMap extends SimpleFileVisitor<Path> {

		private final Map<String, byte[]> content = new HashMap<>();

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
			String key = path.toString();
			if (content.put(key, read(path)) != null) {
				throw new IllegalStateException("Duplicate entry for " + key);
			}
			return CONTINUE;
		}
	}

	@Test
	void canTransformMavenMojoJarToGradlePluginInDirectory(@TempDir File tmpDir) throws Exception {
		Class<GreeterMojo> mojo = GreeterMojo.class;
		addClass(tmpDir, nonMojoClass());
		addClass(tmpDir, mojo);

		Map<String, byte[]> beforeTransform = collectDirectory(tmpDir);

		walkFileTree(tmpDir.toPath(),
				new TransformMojoVisitor(FileSystems.getDefault(), writeToDirectory(tmpDir),
						ign -> new PluginInfo("com.github.pfichtner.maedle.some.target.packagename", "greeting"))
								.withoutCopy());

		Map<String, byte[]> afterTransform = collectDirectory(tmpDir);

		// TODO better asserts
		assertThat(afterTransform).hasSize(beforeTransform.size() + 4) //
				.containsKeys(beforeTransform.keySet().toArray(new String[beforeTransform.size()])) //
		;

	}

	private Map<String, byte[]> collectDirectory(File tmpDir) throws IOException {
		CollectToMap visitor = new CollectToMap();
		walkFileTree(tmpDir.toPath(), visitor);
		Map<String, byte[]> content = visitor.content;
		return content;
	}

	@Test
	void canTransformMavenMojoJarToGradlePluginInJar(@TempDir File tmpDir) throws Exception {
		Class<GreeterMojo> mojo = GreeterMojo.class;
		File inJar = new File(tmpDir, "in.jar");
		File outJar = new File(tmpDir, "out.jar");

		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.some.target.packagename", "greeting");
		try (JarModifier jarBuilder = new JarModifier(inJar, true)) {
			addClass(jarBuilder, nonMojoClass());
			addClass(jarBuilder, mojo);
		}

		// TODO when transforming: switchable "overwrite" flag for the jar
		transform(inJar, outJar, t -> pluginInfo);
		Set<String> classNamesOfInJar = collectJarContents(inJar).keySet();

		String pkgName = mojo.getPackage().getName();
		String internal = "/" + (pkgName + ".").replace('.', '/');

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

		verifyCanLoadClass(outJar, pkgName + ".GreeterMojoGradlePlugin");
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
		CollectToMap visitor = new CollectToMap();
		try (JarModifier jarReader = new JarModifier(jar, false)) {
			jarReader.readJar(visitor);
		}
		return visitor.content;
	}

	private static byte[] read(Path file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(file, baos);
		return baos.toByteArray();
	}

	private static Class<?> nonMojoClass() {
		return CanTransformMavenMojoJarTest.class;
	}

	private static void addClass(JarModifier jarWriter, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		jarWriter.add(asStream(clazz), clazz.getName().replace('.', '/') + ".class");
	}

	private static void addClass(File baseDir, Class<?> clazz)
			throws IOException, FileNotFoundException, URISyntaxException {
		File target = new File(baseDir, clazz.getName().replace('.', '/') + ".class");
		ensureDirectoryExists(target.getParentFile());
		writeFile(target, asStream(clazz));
	}

	private void transform(File jar, File outJar, Function<Type, PluginInfo> infoProvider) throws IOException {
		try (JarModifier reader = new JarModifier(jar, false); JarModifier writer = new JarModifier(outJar, true)) {
			reader.readJar(new TransformMojoVisitor(reader.getFileSystem(), writeToJar(writer), infoProvider));
		}
	}

}
