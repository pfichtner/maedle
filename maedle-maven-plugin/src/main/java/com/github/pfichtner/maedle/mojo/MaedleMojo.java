package com.github.pfichtner.maedle.mojo;

import static com.pfichtner.github.maedle.transform.ResourceAddables.writeToDirectory;
import static com.pfichtner.github.maedle.transform.util.IoUtils.copyTree;
import static java.nio.file.Files.walkFileTree;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.function.Function;

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
import com.pfichtner.github.maedle.transform.ResourceAddable;
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
	public File outputDirectory;

	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	public File classesDirectory;

	@Parameter(name = "mappings")
	public List<PluginInfoProvider.Mapping> mappings;

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
		if (classesDirectory.exists()) {
			// TODO project.isExecutionRoot();
			if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
				throw new MojoExecutionException("Cannot create output directory " + outputDirectory);
			}
			transform();
		}
	}

	private void transform() throws MojoFailureException {
		TransformMojoVisitor visitor = new TransformMojoVisitor(FileSystems.getDefault(),
				combine(log(), writeToDirectory(outputDirectory)), pluginInfoProvider()).withCopy(false);
		try {
			walkFileTree(classesDirectory.toPath(), visitor);
			walkFileTree(outputDirectory.toPath(), copyTree(outputDirectory, classesDirectory));
		} catch (IOException e) {
			throw new MojoFailureException("error reading " + classesDirectory, e);
		}

//		Resource resource = resource();
//		getLog().info("Adding resource " + resource);
//		project.addResource(resource);
	}

	public Function<Type, PluginInfo> pluginInfoProvider() {
		return new PluginInfoProvider(mappings).pluginFunction();
	}

	private Resource resource() {
		Resource resource = new Resource();
		resource.setDirectory(outputDirectory.toString());
		return resource;
	}

	private ResourceAddable log() {
		return (content, path) -> getLog().info("Adding " + path + " to " + outputDirectory);
	}

	private static ResourceAddable combine(ResourceAddable addable1, ResourceAddable addable2) {
		return (content, path) -> {
			addable1.add(content, path);
			addable2.add(content, path);
		};
	}

	public MavenProject getProject() {
		return project;
	}

}
