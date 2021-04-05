package com.pfichtner.github.maedle.mojo;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.pfichtner.maedle.transform.util.jar.ToResourceTransformer;

@Mojo(name = MaedleMojo.GOAL, defaultPhase = GENERATE_RESOURCES)
public class MaedleMojo extends AbstractMojo {

	public static final String GOAL = "transform";

//	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	@Component
	private MavenProject project;

	/**
	 * The directory where processed resources will be placed for packaging.
	 */
	@Parameter(defaultValue = "${project.build.directory}/maedle-transformed-classes")
	private File outputDirectory;

//	@Component
//	private MavenProjectHelper projectHelper;
//
//	@Component(role = Archiver.class, hint = "jar")
//	private JarArchiver jarArchiver;
//
//
//	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
//	private File outputDirectory;
//
//	@Parameter(defaultValue = "${project.build.finalName}", alias = "jarName", required = true)
//	private String finalName;

	@Override
	public void execute() throws MojoExecutionException {
		// TODO project.isExecutionRoot();

		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw new MojoExecutionException("Cannot create output directory " + outputDirectory);
		}

		
		

		Resource resource = new Resource();
		resource.setDirectory("/tmp/foo");
		project.getResources().add(resource);

	}

	public MavenProject getProject() {
		return project;
	}

}
