package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformMojo.transformedMojoInstance;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;

public class GreeterMojoIT {

	// TODO not so generic Plugin code
//	The plugin will automatically run at the end of your test task. 
//
//	You can also run it manually with the generateCucumberReports task.
//
//
//
//	def reportsPluginExtension = project.extensions.create('cucumberReports', ReportsPluginExtension)
//	Task reportTask = project.task('generateCucumberReports', type: CreateReportFilesTask) {
//	    description = "Creates cucumber html reports"
//	    group = "Cucumber reports"
//	    projectName = project.displayName
//	}
//
//	project.afterEvaluate {
//	    if (project.extensions.cucumberReports.testTasksFinalizedByReport) {
//	        project.tasks.withType(Test) { Test test -> test.finalizedBy(reportTask) }
//	    }
//	}
//	reportTask.onlyIf { !project.hasProperty('skipReports') }

	@Test
	// TODO this should be done in a gradle project with the usage of testkit
	void canTransformHeapWatchMojo() throws Exception {
		// TODO Do not forget to write META-INF! ;-)
		GreeterMojo greeterMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedMojoInstance(greeterMojo);

		AsmClassLoader asmClassLoader = new AsmClassLoader(transformedMojoInstance.getClass().getClassLoader());

		String extensionType = Type.getInternalName(typeOfSingleArgConstructor(transformedMojoInstance));
		String mojoType = Type.getInternalName(transformedMojoInstance.getClass());

		String pluginType = (greeterMojo.getClass().getName() + "GradlePlugin").replace('.', '/');
		Class<?> pluginClass = asmClassLoader.defineClass(
				createPlugin(pluginType, extensionType, mojoType, "greet", "greeting"), pluginType.replace('/', '.'));
		Object plugin = pluginClass.newInstance();
		Class<?> projectClass = (Class<?>) asmClassLoader.loadClass("org.gradle.api.Project");

		Method applyMethod = plugin.getClass().getMethod("apply", projectClass);
		// TODO we need a mosquito mock here: project.getExtensions().create("greeting",
		// GreetingPluginExtension.class);
//			apply.invoke(plugin, new Object[] { null });

	}

	private Class<?> typeOfSingleArgConstructor(Object transformedMojoInstance) {
		return constructor(transformedMojoInstance.getClass(), c -> c.getParameterCount() == 1).getParameters()[0]
				.getType();
	}

}