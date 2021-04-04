package com.pfichtner.github.maedle.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

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
//			assertThat(mojo.getProject()).isNotNull();
		});

	}

}
