package com.github.pfichtner.maedle.transform;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultBuildLauncher;

public class GradleTestKit implements Closeable {

	private final GradleConnector connector;
	private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

	public GradleTestKit(String projectDir) {
		this(projectDir, "6.8.3");
	}

	public GradleTestKit(String projectDir, String version) {
		connector = GradleConnector.newConnector();
		connector.useGradleVersion(version);
		connector.forProjectDirectory(new File(projectDir));
	}

	public String executeTask(File pluginJar, String... tasks) {
		try (ProjectConnection connection = connector.connect()) {
			withInjectedClassPath(pluginJar, connection.newBuild().setStandardOutput(stdout).forTasks(tasks)).run();
			return stdout.toString();
		}
	}

	private BuildLauncher withInjectedClassPath(File pluginJar, BuildLauncher launcher) {
		// howto do via API?
		((DefaultBuildLauncher) launcher).withInjectedClassPath(DefaultClassPath.of(pluginJar));
		return launcher;
	}

	@Override
	public void close() throws IOException {
		connector.disconnect();
	}

}