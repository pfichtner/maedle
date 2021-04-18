package com.github.pfichtner.maedle.mojo;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.pfichtner.maedle.mojo.MaedleMojo.Mapping;

public class MaedleMojoTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void failIfClassCannotBeLoaded() throws Exception {
		String clazz = "foo/bar/NonExistingClass";
		MaedleMojo sut = new MaedleMojo();
		sut.outputDirectory = temporaryFolder.getRoot();
		sut.classesDirectory = temporaryFolder.getRoot();
		sut.mappings = asList(new Mapping(clazz, "pluginId", "extension"));
		assertThatThrownBy(sut::execute).hasMessageContaining(clazz);
	}

}
