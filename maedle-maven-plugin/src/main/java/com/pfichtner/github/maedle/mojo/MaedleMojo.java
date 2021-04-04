package com.pfichtner.github.maedle.mojo;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

@Mojo(name = MaedleMojo.GOAL, defaultPhase = LifecyclePhase.PACKAGE)
public class MaedleMojo extends AbstractMojo {

	public static final String GOAL = "transform";

//	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	@Component
	private MavenProject project;

	@Override
	public void execute() {
		System.out.println("*****");
		File basedir = project.getBasedir();
		System.out.println("*****");
		System.out.println("basedir " + basedir);
		System.out.println("*****");
		project.getResources().forEach(r -> System.out.println("\tresource " + r));
		// TODO project.isExecutionRoot();
	}

	private void copyFile(Resource resource, File srcFile, File destFile) throws IOException {
		// TODO verify destFile parent exists
//		IoUtils.copy(null, null);

	}
	
	public MavenProject getProject() {
		return project;
	}
	
}
