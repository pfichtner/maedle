package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginInfo.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginInfo.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginInfo.transformMojoAndWriteJar;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo3;

public class GreeterMojo3IT {

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greet", "greeting");
		createProjectSettingsFile(testProjectDir);
		createProjectBuildFile(testProjectDir, pluginInfo, emptyMap());
		File pluginJar = transformMojoAndWriteJar(testProjectDir, GreeterMojo3.class, pluginInfo);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, pluginInfo.taskName);
			assertThat(stdOut).contains("Warn log statement");
		}

	}

}
