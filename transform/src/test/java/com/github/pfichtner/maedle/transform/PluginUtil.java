package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.transform.util.jar.JarModifier;
import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public final class PluginUtil {

	private PluginUtil() {
		super();
	}

	public static File createProjectSettingsFile(File baseDir) throws IOException {
		File settingsFile = new File(baseDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");
		return settingsFile;
	}

	public static File transformMojoAndWriteJar(File baseDir, Class<? extends Mojo> mojoClass, PluginInfo pluginInfo)
			throws IOException, FileNotFoundException {
		File pluginJar = createTempFile("plugin-", ".jar", baseDir);
		pluginJar.delete();
		TransformationParameters parameters = fromMojo(toBytes(asStream(mojoClass)));
		TransformationResult result = new TransformationResult(parameters);
		try (JarModifier jarWriter = new JarModifier(pluginJar, true)) {
			Type mojoType = parameters.getMojoClass();

			jarWriter.add(stream(result.getTransformedMojo()), newJarEntry(mojoType));
			jarWriter.add(stream(result.getExtension()), newJarEntry(parameters.getExtensionClass()));

			Type pluginType = Type.getObjectType(mojoType.getInternalName() + "GradlePlugin");
			jarWriter.add(stream(createPlugin(pluginType, parameters.getExtensionClass(), mojoType, pluginInfo.taskName,
					pluginInfo.extensionName)), newJarEntry(pluginType));

			jarWriter.add(("implementation-class=" + pluginType.getInternalName().replace('/', '.')).getBytes(),
					"META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties");
		}
		return pluginJar;
	}

	private static InputStream stream(byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}

	private static String newJarEntry(Type type) {
		return toPath(type);
	}

	private static String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

	public static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo, Map<Object, Object> entries)
			throws IOException {
		File buildFile = new File(testProjectDir, "build.gradle");
		writeFile(buildFile, buildFileContent(pluginInfo, entries).stream().collect(joining("\n")));
		return buildFile;
	}

	public static List<String> buildFileContent(PluginInfo pluginInfo, Map<Object, Object> entries) {
		List<String> lines = new ArrayList<>();
		lines.add(format("plugins { id '%s' }", pluginInfo.pluginId));
		lines.add(format("%s {", pluginInfo.extensionName));
		entries.forEach((k, v) -> lines.add(format("	" + k + " = \"%s\"", v)));
		lines.add(format("}"));
		return lines;
	}

}
