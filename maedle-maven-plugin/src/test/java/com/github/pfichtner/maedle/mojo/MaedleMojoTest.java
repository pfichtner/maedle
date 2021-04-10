package com.github.pfichtner.maedle.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.objectweb.asm.Type;

public class MaedleMojoTest {

	@Test
	public void withoutMappingTheDefaultPluginInfoIsNotNull() {
		MaedleMojo maedleMojo = new MaedleMojo();
		assertThat(maedleMojo.pluginInfoProvider().apply(Type.getObjectType("any/Class"))).isNotNull();
	}

}
