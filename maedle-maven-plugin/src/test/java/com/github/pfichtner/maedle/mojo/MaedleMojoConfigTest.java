package com.github.pfichtner.maedle.mojo;

import static com.pfichtner.github.maedle.transform.util.IoUtils.addClass;
import static com.pfichtner.github.maedle.transform.util.IoUtils.directoryContent;
import static com.pfichtner.github.maedle.transform.util.IoUtils.stripBaseDir;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.junit.Rule;
import org.junit.Test;

public class MaedleMojoConfigTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TestResources resources = new TestResources();

	private MaedleMojo sut;

	@Test
	// TODO This test replaces the integration test!
	public void testDefault() throws Exception {
		loadMojo("project-to-test");
		assertThat(sut.transformOnlyIfConfigured).isFalse();
		assertThat(sut.mappings).isNotEmpty();
		addToClasses(TestMojo1.class);
		execute();

		assertThat(relativeFileNames()).containsExactlyInAnyOrderElementsOf(asList( //
				"com/github/pfichtner/maedle/mojo/TestMojo1GradlePluginExtension.class", //
				"com/github/pfichtner/maedle/mojo/TestMojo1Rewritten.class", //
				"com/github/pfichtner/maedle/mojo/TestMojo1GradlePlugin.class", //
				"META-INF/gradle-plugins/my.example.gradle.extension.properties" //
		));
	}

	@Test
	public void transformWithDefaultMappingIfNotConfigured() throws Exception {
		loadMojo("transformWithDefaultMapping");
		assertThat(sut.transformOnlyIfConfigured).isFalse();
		assertThat(sut.mappings).isNullOrEmpty();
		addToClasses(TestMojo1.class);
		execute();
		assertThat(relativeFileNames()).containsExactlyInAnyOrderElementsOf(asList( //
				"com/github/pfichtner/maedle/mojo/TestMojo1GradlePluginExtension.class", //
				"com/github/pfichtner/maedle/mojo/TestMojo1GradlePlugin.class", //
				"com/github/pfichtner/maedle/mojo/TestMojo1Rewritten.class", //
				"META-INF/gradle-plugins/com.github.pfichtner.maedle.mojo.properties" //
		));
	}

	@Test
	public void doNotTransformIfNotConfigured() throws Exception {
		loadMojo("transformOnlyIfConfigured");
		assertThat(sut.transformOnlyIfConfigured).isTrue();
		addToClasses(TestMojo1.class);
		execute();
		assertThat(relativeFileNames()).isEmpty();
	}

	private void loadMojo(String directory) throws IOException, Exception, ComponentConfigurationException {
		sut = (MaedleMojo) this.rule.lookupConfiguredMojo(resources.getBasedir(directory), MaedleMojo.GOAL);
	}

	private void addToClasses(Class<?> clazz) throws IOException {
		addClass(sut.classesDirectory, clazz);
	}

	private void execute() {
		try {
			sut.execute();
		} catch (MojoExecutionException | MojoFailureException e) {
			throw new RuntimeException(e);
		}
	}

	private Stream<String> relativeFileNames() throws IOException {
		return relativeFileNames(sut.outputDirectory);
	}

	private static Stream<String> relativeFileNames(File outputDirectory) throws IOException {
		return directoryContent(outputDirectory).keySet().stream().map(f -> stripBaseDir(outputDirectory, f));
	}

}
