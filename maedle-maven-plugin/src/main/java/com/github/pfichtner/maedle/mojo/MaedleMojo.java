package com.github.pfichtner.maedle.mojo;

import static com.pfichtner.github.maedle.transform.ResourceAddables.writeToDirectory;
import static java.nio.file.Files.walkFileTree;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;
import com.pfichtner.github.maedle.transform.TransformMojoVisitor;

@Mojo(name = MaedleMojo.GOAL, defaultPhase = PROCESS_CLASSES)
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

	@Parameter(defaultValue = "${project.build.directory}/classes")
	private File classesDirectory;

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
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO project.isExecutionRoot();

		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw new MojoExecutionException("Cannot create output directory " + outputDirectory);
		}

		try {
			walkFileTree(classesDirectory.toPath(), new TransformMojoVisitor(FileSystems.getDefault(),
					writeToDirectory(outputDirectory), MaedleMojo::createPluginInfo).withCopy(false));
		} catch (IOException e) {
			throw new MojoFailureException("error reading " + classesDirectory, e);
		}

		Resource resource = new Resource();
		resource.setDirectory(outputDirectory.toString());
		project.getResources().add(resource);
	}

	private static PluginInfo createPluginInfo(Type type) {
		String className = type.getClassName();
		int lastSlashAt = className.lastIndexOf('/');
		return new PluginInfo(lastSlashAt >= 0 ? className.substring(0, lastSlashAt) : className, "extname");
	}

	public MavenProject getProject() {
		return project;
	}

}
