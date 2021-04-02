package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;

import org.apache.maven.plugin.Mojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.maedle.transform.uti.jar.JarWriter;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public class GreeterMojoIT {

	// TODO not so generic Plugin code
	// The plugin will automatically run at the end of your test task.
	//
	// You can also run it manually with the generateCucumberReports task.
	//
	//
	//
	// def reportsPluginExtension = project.extensions.create('cucumberReports',
	// ReportsPluginExtension)
	// Task reportTask = project.task('generateCucumberReports', type:
	// CreateReportFilesTask) {
	// description = "Creates cucumber html reports"
	// group = "Cucumber reports"
	// projectName = project.displayName
	// }
	//
	// project.afterEvaluate {
	// if (project.extensions.cucumberReports.testTasksFinalizedByReport) {
	// project.tasks.withType(Test) { Test test -> test.finalizedBy(reportTask) }
	// }
	// }
	// reportTask.onlyIf { !project.hasProperty('skipReports') }

	public static class PluginInfo {

		public String pluginId;
		public String taskName;
		public String extensionName;

		public PluginInfo(String pluginId, String taskName, String extensionName) {
			this.pluginId = pluginId;
			this.taskName = taskName;
			this.extensionName = extensionName;
		}
	}

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.greetingtest", "greet", "greeting");
		createProjectSettingsFile(testProjectDir);
		createProjectBuildFile(testProjectDir, pluginInfo);
		File pluginJar = transformMojoAndWriteJar(testProjectDir, GreeterMojo.class, pluginInfo);
		try (ToolingAPI toolingAPI = new ToolingAPI(testProjectDir.getAbsolutePath())) {
			String stdOut = toolingAPI.executeTask(pluginJar, pluginInfo.taskName);
			assertThat(stdOut) //
					.contains("> Task :" + pluginInfo.taskName) //
					.contains("Hello, integration test") //
					.contains("I have a message for you: Test success!") //
			;
		}

	}

	private File createProjectSettingsFile(File baseDir) throws IOException {
		File settingsFile = new File(baseDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");
		return settingsFile;
	}

	private File transformMojoAndWriteJar(File baseDir, Class<? extends Mojo> mojoClass, PluginInfo pluginInfo)
			throws IOException, FileNotFoundException {
		File pluginJar = new File(baseDir, "plugin.jar");
		TransformationParameters parameters = new TransformationParameters(toBytes(asStream(mojoClass)));
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

	private static File createProjectBuildFile(File testProjectDir, PluginInfo pluginInfo) throws IOException {
		File buildFile = new File(testProjectDir, "build.gradle");
		writeFile(buildFile, buildFileContent(pluginInfo).stream().collect(joining("\n")));
		return buildFile;
	}

	private static List<String> buildFileContent(PluginInfo pluginInfo) {
		return asList( //
				format("plugins { id '%s' }", pluginInfo.pluginId), //
				format("%s {", pluginInfo.extensionName), //
				format("	greeter = \"%s\"", "integration test"), //
				format("	message = \"%s\"", "Test success!"), //
				format("}") //
		);
	}

	private String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

}
