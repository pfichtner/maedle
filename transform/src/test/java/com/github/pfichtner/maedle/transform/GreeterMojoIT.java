package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginInfo.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginInfo.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginInfo.transformMojoAndWriteJar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;

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

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greet", "greeting");
		createProjectSettingsFile(testProjectDir);
		String greeterText = "integration test";
		String messageText = "Test success!";
		createProjectBuildFile(testProjectDir, pluginInfo, createData(greeterText, messageText));
		File pluginJar = transformMojoAndWriteJar(testProjectDir, GreeterMojo.class, pluginInfo);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, pluginInfo.taskName);
			assertThat(stdOut) //
					.contains("> Task :" + pluginInfo.taskName) //
					.contains("Hello, " + greeterText) //
					.contains("I have a message for you: " + messageText) //
			;
		}

	}

	private Map<Object, Object> createData(String greeterText, String messageText) {
		Map<Object, Object> data = new HashMap<>();
		data.put("greeter", greeterText);
		data.put("message", messageText);
		return data;
	}

	private static Map<Object, Object> entries() {
		Map<Object, Object> data = new HashMap<>();
		data.put("greeter", "integration test");
		data.put("message", "Test success!");
		return data;
	}

}
