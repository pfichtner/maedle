package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.transform.uti.jar.JarWriter;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public class PluginInfo {

	public String pluginId;
	public String taskName;
	public String extensionName;

	public PluginInfo(String pluginId, String taskName, String extensionName) {
		this.pluginId = pluginId;
		this.taskName = taskName;
		this.extensionName = extensionName;
	}

	static File createProjectSettingsFile(File baseDir) throws IOException {
		File settingsFile = new File(baseDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");
		return settingsFile;
	}

	static File transformMojoAndWriteJar(File baseDir, Class<? extends Mojo> mojoClass, PluginInfo pluginInfo)
			throws IOException, FileNotFoundException {
		File pluginJar = new File(baseDir, "plugin.jar");
		TransformationParameters parameters = TransformationParameters.fromMojo(toBytes(asStream(mojoClass)));
		TransformationResult result = new TransformationResult(parameters);
		MojoData mojoData = parameters.getMojoData();
		try (JarWriter jarWriter = new JarWriter(new FileOutputStream(pluginJar), false)) {
			jarWriter.addEntry(new JarEntry(toPath(mojoData.getMojoType())),
					new ByteArrayInputStream(result.getTransformedMojo()));
			jarWriter.addEntry(new JarEntry(toPath(parameters.getExtensionClass())),
					new ByteArrayInputStream(result.getExtension()));

			String mojoType = mojoData.getMojoType().getInternalName();
			String extensionType = parameters.getExtensionClass().getInternalName();
			String pluginType = mojoType + "GradlePlugin";

			byte[] pluginBytes = createPlugin(pluginType, extensionType, mojoType, pluginInfo.taskName,
					pluginInfo.extensionName);
			jarWriter.addEntry(new JarEntry(pluginType + ".class"), new ByteArrayInputStream(pluginBytes));

			jarWriter.addEntry(new JarEntry("META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties"),
					new ByteArrayInputStream(("implementation-class=" + pluginType.replace('/', '.')).getBytes()));
		}
		return pluginJar;
	}

	static String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

	static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo, Map<Object, Object> entries)
			throws IOException {
		File buildFile = new File(testProjectDir, "build.gradle");
		writeFile(buildFile, buildFileContent(pluginInfo, entries).stream().collect(joining("\n")));
		return buildFile;
	}

	static List<String> buildFileContent(PluginInfo pluginInfo, Map<Object, Object> entries) {
		List<String> l = new ArrayList<>();
		l.add(format("plugins { id '%s' }", pluginInfo.pluginId));
		l.add(format("%s {", pluginInfo.extensionName));
		for (Entry<Object, Object> entry : entries.entrySet()) {
			l.add(format("	" + entry.getKey() + " = \"%s\"", entry.getValue()));
		}
		l.add(format("}"));
		return l;

	}

}
