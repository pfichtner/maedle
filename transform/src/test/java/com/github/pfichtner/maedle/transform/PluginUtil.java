package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.ResourceAddables.writeToJar;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.classToPath;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.pfichtner.maedle.transform.util.jar.JarModifier;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;
import com.pfichtner.github.maedle.transform.TransformMojoVisitor;

public final class PluginUtil {

	private PluginUtil() {
		super();
	}

	public static File createProjectSettingsFile(File baseDir) throws IOException {
		File settingsFile = new File(baseDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");
		return settingsFile;
	}

	public static File transformMojoAndWriteJar(File baseDir, PluginInfo pluginInfo, Class<?>... classes)
			throws IOException {
		File pluginJar = createTempFile("plugin-", ".jar", baseDir);
		pluginJar.delete();
		fillJar(pluginJar, classes);
		try (JarModifier modifier = new JarModifier(pluginJar, true)) {
			modifier.readJar(new TransformMojoVisitor(modifier.getFileSystem(), writeToJar(modifier), ign -> pluginInfo));
		}
		return pluginJar;
	}

	private static void fillJar(File pluginJar, Class<?>... classes) throws IOException {
		try (JarModifier modifier = new JarModifier(pluginJar, true)) {
			for (Class<?> clazz : classes) {
				modifier.add(asStream(clazz), classToPath(clazz));
			}
		}
	}

	@Deprecated
	public static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo, Map<Object, Object> entries)
			throws IOException {
		return createProjectBuildFile(testProjectDir, pluginInfo, toText(entries));
	}

	public static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo, String config)
			throws IOException {
		File buildFile = new File(testProjectDir, "build.gradle");
		writeFile(buildFile, buildFileContent(pluginInfo, config).stream().collect(joining("\n")));
		return buildFile;
	}

	private static List<String> buildFileContent(PluginInfo pluginInfo, String config) {
		List<String> lines = new ArrayList<>();
		lines.add("plugins { id '" + pluginInfo.pluginId + "' }");
		lines.add(pluginInfo.extensionName + " {");
		lines.add(config);
		lines.add("}");
		return lines;
	}

	private static String toText(Map<Object, Object> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<Object, Object> entry : map.entrySet()) {
			sb.append(value(entry) + "\n");
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static String value(Entry<Object, Object> entry) {
		Object value = entry.getValue();
		if (value instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>) value;
			return entry.getKey() + " = [" + //
					map.entrySet().stream().map(PluginUtil::formatEntry).collect(joining(", ")) //
					+ "]";
//			return entry.getKey() + " {\n" + toText(map) + "}";
		}
		return entry.getKey() + " = \"" + value + "\"";
	}

	@SuppressWarnings("unchecked")
	private static String formatEntry(Entry<Object, Object> e) {
		Object value = e.getValue();
		if (value instanceof Map) {
			return toText((Map<Object, Object>) value);
		}
		return format(value instanceof Number ? "%s: %s" : "%s: '%s'", e.getKey(), value);
	}

}
