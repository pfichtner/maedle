package com.pfichtner.github.maedle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

/**
 * The code of this class will be read and transformed/remapped by the
 * PluginWriter class.
 */
public class MaedlePluginTemplate implements Plugin<Project> {

	// value will be replaced by PluginWriter
	public static final String E_NAME = "theExtensionName";
	// value will be replaced by PluginWriter
	public static final String T_NAME = "theTaskName";

	@Override
	public void apply(Project project) {
		// E will be replaced by the concrete Extension class
		E e = project.getExtensions().create(E_NAME, E.class);
		Task task = project.task(T_NAME);
		// M will be replaced by the concrete transformed Mojo class (the transformed
		// Mojo class has a constructor accepting E)
		task.doLast(t -> new M(e).execute());
	}

}