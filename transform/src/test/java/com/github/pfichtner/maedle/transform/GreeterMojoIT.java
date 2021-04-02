package com.github.pfichtner.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static com.pfichtner.github.maedle.transform.util.IoUtils.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;

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
//	The plugin will automatically run at the end of your test task. 
//
//	You can also run it manually with the generateCucumberReports task.
//
//
//
//	def reportsPluginExtension = project.extensions.create('cucumberReports', ReportsPluginExtension)
//	Task reportTask = project.task('generateCucumberReports', type: CreateReportFilesTask) {
//	    description = "Creates cucumber html reports"
//	    group = "Cucumber reports"
//	    projectName = project.displayName
//	}
//
//	project.afterEvaluate {
//	    if (project.extensions.cucumberReports.testTasksFinalizedByReport) {
//	        project.tasks.withType(Test) { Test test -> test.finalizedBy(reportTask) }
//	    }
//	}
//	reportTask.onlyIf { !project.hasProperty('skipReports') }

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		TransformationParameters parameters = new TransformationParameters(toBytes(asStream(greeterMojo.getClass())));
		TransformationResult result = new TransformationResult(parameters);

		File settingsFile = new File(testProjectDir, "settings.gradle");
		writeFile(settingsFile, "rootProject.name = 'any-name'");

		String pluginId = "com.github.pfichtner.gradle.greeting";
		String taskName = "greet";
		String extensionName = "greeting";

		File buildFile = new File(testProjectDir, "build.gradle");
		String buildFileContent = "" //
				+ "plugins { id '" + pluginId + "' }\n" //
				+ extensionName + " {\n" //
				+ "		greeter = \"" + "integration test" + "\"\n" //
				+ "		message = \"" + "Test success!" + "\"\n" //
				+ "}\n" //
		;
		writeFile(buildFile, buildFileContent);

		File pluginJar = new File(testProjectDir, "plugin.jar");
		MojoData mojoData = parameters.getMojoData();
		try (JarWriter jarWriter = new JarWriter(new FileOutputStream(pluginJar), false)) {
			jarWriter.addEntry(new JarEntry(toPath(mojoData.getMojoType())),
					new ByteArrayInputStream(result.getTransformedMojo()));
			jarWriter.addEntry(new JarEntry(toPath(parameters.getExtensionClass())),
					new ByteArrayInputStream(result.getExtension()));

			String mojoType = mojoData.getMojoType().getInternalName();
			String extensionType = parameters.getExtensionClass().getInternalName();
			String pluginType = mojoType + "GradlePlugin";

			byte[] pluginBytes = createPlugin(pluginType, extensionType, mojoType, taskName, extensionName);
			jarWriter.addEntry(new JarEntry(pluginType + ".class"), new ByteArrayInputStream(pluginBytes));

			jarWriter.addEntry(new JarEntry("META-INF/gradle-plugins/" + pluginId + ".properties"),
					new ByteArrayInputStream(("implementation-class=" + pluginType.replace('/', '.')).getBytes()));
		}

		try (ToolingAPI toolingAPI = new ToolingAPI(testProjectDir.getAbsolutePath())) {
			String stdOut = toolingAPI.executeTask(pluginJar, "greet");
			assertThat(stdOut) //
					.contains("> Task :greet") //
					.contains("Hello, integration test") //
					.contains("I have a message for You: Test success!") //
			;
		}

	}

	private String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

}
