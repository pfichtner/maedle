package com.pfichtner.github.maedle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class MaedlePluginTemplate implements Plugin<Project> {

	private static final String EXTENSION = "theExtensionName";
	public static final String TASK = "theTaskName";

	@Override
	public void apply(Project project) {
		TransformedExtension extension = project.getExtensions().create(EXTENSION, TransformedExtension.class);
		Task task = project.task(TASK);
		task.doLast(t -> new TransformedMojo(extension).execute());
	}

}