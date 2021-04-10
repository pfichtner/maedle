package com.github.pfichtner.maedle.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;
import org.objectweb.asm.Type;

public class MaedleMojoConfigTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TestResources resources = new TestResources();

	@Test
	public void canParse() throws Exception {
		File pom = new File(resources.getBasedir("project-to-test"), "pom.xml");
		assertThat(pom).exists();
		assertThat((MaedleMojo) this.rule.lookupMojo(MaedleMojo.GOAL, pom)).isNotNull().satisfies(mojo -> {
			assertThat(mojo.pluginInfoProvider().apply(Type.getType(TestMojo1.class))).isNotNull()
					.satisfies(m -> assertThat(m).satisfies(e -> {
						assertThat(e.pluginId).isEqualTo("com.github.pfichtner.heapwatch.extension");
						assertThat(e.extensionName).isEqualTo("heapwatch");
					}));
		});

	}

}
