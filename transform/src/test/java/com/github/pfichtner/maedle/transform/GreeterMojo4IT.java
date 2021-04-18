package com.github.pfichtner.maedle.transform;

import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectBuildFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.createProjectSettingsFile;
import static com.github.pfichtner.maedle.transform.PluginUtil.transformMojoAndWriteJar;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo4;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class GreeterMojo4IT {

	@Test
	void canTransformHeapWatchMojo(@TempDir File testProjectDir) throws Exception {
		PluginInfo pluginInfo = new PluginInfo("com.github.pfichtner.maedle.mojotogradle", "greeting");
		createProjectSettingsFile(testProjectDir);
		createProjectBuildFile(testProjectDir, pluginInfo, configuration());
		File pluginJar = transformMojoAndWriteJar(GreeterMojo4.class, testProjectDir, pluginInfo);
		try (GradleTestKit testKit = new GradleTestKit(testProjectDir.getAbsolutePath())) {
			String stdOut = testKit.executeTask(pluginJar, GreeterMojo4.GOAL);
			assertThat(stdOut).contains("one * 42 = 42").contains("two * 42 = 84").contains("three * 42 = 126");
		}
	}

	private Map<Object, Object> configuration() {
		Map<Object, Object> data = new HashMap<>();
		data.put("data", level1());
		return data;
	}

	private Map<Object, Object> level1() {
		Map<Object, Object> map = new HashMap<>();
		map.put("one", 1);
		map.put("two", 2);
		map.put("three", 3);
		return map;
	}

//	private Map<Object, Object> level1() {
//		Map<Object, Object> map = new HashMap<>();
//		map.put("a", level2(1, "one"));
//		map.put("b", level2(2, "two"));
//		return map;
//	}

//	private Map<Object, Object> level2(int number, String text) {
//		Map<Object, Object> map = new HashMap<>();
//		map.put("number", number);
//		map.put("text", text);
//		return map;
//	}

}
