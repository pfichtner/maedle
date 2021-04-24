package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.transformMojoAndWriteJar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

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
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greeting");
		createProjectSettingsFile(testProjectDir);
		String greeter = "integration test";
		String message = "Test success!";
		String taskName = GreeterMojo.GOAL;
		createProjectBuildFile(testProjectDir, pluginInfo,
				new GreeterMessageBuilder().withGreeter(greeter).withMessage(message).build());
		File pluginJar = transformMojoAndWriteJar(testProjectDir, pluginInfo, GreeterMojo.class);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, taskName);
			assertThat(stdOut) //
					.contains("> Task :" + taskName) //
					.contains("Hello, " + greeter) //
					.contains("I have a message for you: " + message) //
			;
		}
	}

}
