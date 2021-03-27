package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformMojo.transformedMojoInstance;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import com.github.pfichtner.heapwatch.mavenplugin.GreeterMojo;

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
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);

		AsmClassLoader asmClassLoader = new AsmClassLoader(new URLClassLoader(urls(), transformedMojoInstance.getClass().getClassLoader()));

		String extensionType = Type.getInternalName(typeOfSingleArgConstructor(transformedMojoInstance));
		String mojoType = Type.getInternalName(transformedMojoInstance.getClass());

		String pluginType = (heapWatchMojo.getClass().getName() + "GradlePlugin").replace('.', '/');
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

	private URL[] urls() throws MalformedURLException {
		String base = "/home/xck10h6/.m2/repository/";
		return Stream.of( //
//				"org/gradle/gradle-core/5.6.4/gradle-core-5.6.4.jar" //
				"org/gradle/gradle-core-api/5.6.4/gradle-core-api-5.6.4.jar" //
//				"org/gradle/gradle-logging/5.6.4/gradle-logging-5.6.4.jar", //
//				"org/codehaus/groovy/groovy/2.5.14/groovy-2.5.14.jar", //
//				"org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar" //
		).map(base::concat).map(File::new).map(File::toURI).map(GreeterMojoIT::toURL).toArray(URL[]::new);
	}

	private static URL toURL(URI uri) {
		try {
			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
