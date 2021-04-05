package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.append;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.io.File.createTempFile;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.Mojo;

import com.github.pfichtner.maedle.transform.util.jar.JarModifier;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;
import com.pfichtner.github.maedle.transform.TransformationParameters;

public final class PluginUtil {

	private PluginUtil() {
		super();
	}

	public static File createProjectSettingsFile(File baseDir) throws IOException {
		File settingsFile = new File(baseDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");
		return settingsFile;
	}

	public static File transformMojoAndWriteJar(Class<? extends Mojo> mojoClass, File baseDir, PluginInfo pluginInfo)
			throws IOException, FileNotFoundException {
		File pluginJar = createTempFile("plugin-", ".jar", baseDir);
		pluginJar.delete();
		try (JarModifier jarWriter = new JarModifier(pluginJar, true)) {
			TransformationParameters parameters = fromMojo(toBytes(asStream(mojoClass)));
			TransformMojoVisitor.transformTo((content, path) -> jarWriter.add(content, path), parameters,
					append(parameters.getMojoClass(), "GradlePlugin"), pluginInfo);
		}
		return pluginJar;
	}

	public static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo, Map<Object, Object> entries)
			throws IOException {
		File buildFile = new File(testProjectDir, "build.gradle");
		writeFile(buildFile, buildFileContent(pluginInfo, entries).stream().collect(joining("\n")));
		return buildFile;
	}

	public static List<String> buildFileContent(PluginInfo pluginInfo, Map<Object, Object> entries) {
		List<String> lines = new ArrayList<>();
		lines.add("plugins { id '" + pluginInfo.pluginId + "' }");
		lines.add(pluginInfo.extensionName + " {");
		entries.forEach((k, v) -> lines.add("	" + k + " = \"" + v + "\""));
		lines.add("}");
		return lines;
	}

}
