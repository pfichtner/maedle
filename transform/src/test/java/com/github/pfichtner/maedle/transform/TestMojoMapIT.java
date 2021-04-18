package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.transformMojoAndWriteJar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.TestMojoMap;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class TestMojoMapIT {

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greeting");
		createProjectSettingsFile(testProjectDir);
		createProjectBuildFile(testProjectDir, pluginInfo, configuration2());
		File pluginJar = transformMojoAndWriteJar(TestMojoMap.class, testProjectDir, pluginInfo);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, TestMojoMap.GOAL);
			assertThat(stdOut).contains("one * 42 = 42").contains("two * 42 = 84").contains("three * 42 = 126");
			assertThat(stdOut).contains("prime");
		}
	}

	private String configuration2() {
		String c1 = "data  = [one: 1, two: 2, three: 3]";
		String c2 = "data2 = [one: { number=1 isPrime=false }, two: { number=2 isPrime=true }, three: { number=3 isPrime=false }]";
		return c1 + "\n" + c2;
	}

}
