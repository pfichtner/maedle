package com.pfichtner.github.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.TransformMojo.transformedMojoInstance;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.github.pfichtner.heapwatch.mavenplugin.GreeterMojo;
import com.github.stefanbirkner.systemlambda.Statement;

public class GreeterMojoTest {

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

	// TODO error/failure
//     * @throws MojoExecutionException if an unexpected problem occurs.
//     * Throwing this exception causes a "BUILD ERROR" message to be displayed.
//     * @throws MojoFailureException if an expected problem (such as a compilation failure) occurs.
//     * Throwing this exception causes a "BUILD FAILURE" message to be displayed.

	// TODO MyMojo extends AbstractMojo works, what about MyMojo extends
	// MyAbstractMojo, MyAbstractMojo extends AbstractMojo

	@Test
	void worksForNonMojoClasses() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = TransformMojo.transformedMojoInstance(heapWatchMojo);
		Class<?> extensionClass = typeOfSingleArgConstructor(transformedMojoInstance);
//		
		ClassWriter classWriter = new ClassWriter(0);
		StripMojoTransformer mojoToGradleTransformer = new StripMojoTransformer(classWriter, extensionClass.getName());
		new ClassReader(asStream(GreeterMojoTest.class)).accept(mojoToGradleTransformer, 0);
//		new ClassReader(asStream(extensionClass)).accept(mojoToGradleTransformer, 0);

		assertFalse(mojoToGradleTransformer.isMojo());
//		assertEquals(bytes, classWriter3.toByteArray());
	}

	@Test
	void transformedMojoHasSameBehaviorLikeOriginalMojo() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);
		haveSameSysouts(() -> executeMojo(heapWatchMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	// TODO create tests where Mojo fields are public AND where Mojo fields are
	// private and accessed by getters/setters
	void fieldInitializersAreCopiedFromMojoToExtensionClass() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		heapWatchMojo.greeter = "Stranger";
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);
		haveSameSysouts(() -> executeMojo(heapWatchMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void verifyExtensionClassHasNoMethods() throws Exception {
		Class<?> extensionClass = typeOfSingleArgConstructor(transformedMojoInstance(new GreeterMojo()));
		assertEquals(emptyList(), stream(extensionClass.getDeclaredMethods()).map(Method::getName).collect(toList()));
	}

	private Class<?> typeOfSingleArgConstructor(Object transformedMojoInstance) {
		return constructor(transformedMojoInstance.getClass(), c -> c.getParameterCount() == 1).getParameters()[0]
				.getType();
	}

	private static Object executeMojo(Object mojo)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return mojo.getClass().getMethod("execute").invoke(mojo);
	}

	private static void haveSameSysouts(Statement statement1, Statement statement2) throws Exception {
		assertEquals(tapSystemOut(statement1), tapSystemOut(statement2));
	}

}
