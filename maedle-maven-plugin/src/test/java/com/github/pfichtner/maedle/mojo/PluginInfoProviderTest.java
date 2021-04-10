package com.github.pfichtner.maedle.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.mojo.PluginInfoProvider.Mapping;
import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class PluginInfoProviderTest {

	Mapping mapping1 = new Mapping(TestMojo1.class, "pluginId1", "extension1");
	Mapping mapping2 = new Mapping("com/github/pfichtner/maedle/mojo/TestMojo2", "pluginId2", "extension2");
	PluginInfoProvider sut = new PluginInfoProvider(mapping1, mapping2);

	@Test
	public void viaClass() {
		assertPluginForTypeIs(Type.getType(TestMojo1.class), mapping1);
	}

	@Test
	public void viaString() {
		assertPluginForTypeIs(Type.getObjectType("com/github/pfichtner/maedle/mojo/TestMojo2"), mapping2);
	}

	@Test
	public void defaultMapping() {
		String pkg = "non/existing/mapping/for";
		String type = pkg + "/" + "Class";
		assertPluginForTypeIs(Type.getObjectType(type), new Mapping(type, pkg.replace('/', '.'), "extname"));
	}

	private void assertPluginForTypeIs(Type type, Mapping mapping) {
		assertThat(get(type)).isNotNull().satisfies(p -> {
			SoftAssertions softly = new SoftAssertions();
			softly.assertThat(p.pluginId).isEqualTo(mapping.pluginId);
			softly.assertThat(p.extensionName).isEqualTo(mapping.extension);
			softly.assertAll();
		});

	}

	private PluginInfo get(Type type) {
		return sut.pluginFunction().apply(type);
	}
}
