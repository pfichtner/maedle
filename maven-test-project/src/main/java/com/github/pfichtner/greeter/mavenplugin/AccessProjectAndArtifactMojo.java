package com.github.pfichtner.greeter.mavenplugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

@Mojo(name = AccessProjectAndArtifactMojo.GOAL, defaultPhase = INITIALIZE)
public class AccessProjectAndArtifactMojo extends AbstractMojo {

	public static final String GOAL = "writeSomethingToJar";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject mavenProject;

	@Component
	private MavenProject mavenProjectViaComponent;

	@Parameter(defaultValue = "${helper}", required = true, readonly = true)
	private MavenProjectHelper projectHelper;

	@Component
	private MavenProjectHelper projectHelperViaComponent;

	public void execute() throws MojoExecutionException, MojoFailureException {
		System.out.println("Project (param): " + mavenProject);
		System.out.println("Project (component): " + mavenProjectViaComponent);
		System.out.println("Artifact is " + mavenProject.getArtifact());

		assert mavenProject == mavenProjectViaComponent;
		assert projectHelper == projectHelperViaComponent;
	}

}
