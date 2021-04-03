package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.transformMojoAndWriteJar;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo3;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class GreeterMojo3IT {

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greeting");
		createProjectSettingsFile(testProjectDir);
		createProjectBuildFile(testProjectDir, pluginInfo, emptyMap());
		File pluginJar = transformMojoAndWriteJar(GreeterMojo3.class, testProjectDir, pluginInfo);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, GreeterMojo3.GOAL);
			assertThat(stdOut).contains("Warn log statement");
		}
	}

}
