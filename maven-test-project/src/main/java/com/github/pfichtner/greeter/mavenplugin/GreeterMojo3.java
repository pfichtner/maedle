package com.github.pfichtner.greeter.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = GreeterMojo3.GOAL)
public class GreeterMojo3 extends AbstractMojo {

	public static final String GOAL = "greet3";

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().warn("Warn log statement");
	}

}
