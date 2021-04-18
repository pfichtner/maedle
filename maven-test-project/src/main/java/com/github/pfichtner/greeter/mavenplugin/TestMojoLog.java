package com.github.pfichtner.greeter.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = TestMojoLog.GOAL)
public class TestMojoLog extends AbstractMojo {

	public static final String GOAL = "testMojoLog";

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().warn("Log warn written by " + TestMojoLog.class.getSimpleName());
	}

}
