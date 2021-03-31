package com.github.pfichtner.greeter.mavenplugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = AccessProjectAndArtifactMojo.GOAL, defaultPhase = INITIALIZE)
public class AccessProjectAndArtifactMojo extends AbstractMojo {

	public static final String GOAL = "writeSomethingToJar";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject mavenProject;

	public void execute() throws MojoExecutionException, MojoFailureException {
		System.out.println("Project: " + mavenProject);
		System.out.println("Artifact is " + mavenProject.getArtifact());
	}

}
