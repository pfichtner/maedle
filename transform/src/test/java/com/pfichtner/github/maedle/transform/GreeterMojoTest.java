package com.pfichtner.github.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.TransformMojo.transformedMojoInstance;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.packageName;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
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
//     * @throws MojoExecutionException -> catched exception, wrap it using MojoExecutionException 
//     * @throws MojoFailureException if the build should be broken by the plugin itself

	// TODO MyMojo extends AbstractMojo works, what about MyMojo extends
	// MyAbstractMojo, MyAbstractMojo extends AbstractMojo

	@Test
	void worksForNonMojoClasses() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);
		Class<?> extensionClass = extensionClassOf(transformedMojoInstance);
//		
		ClassWriter classWriter = new ClassWriter(0);
		StripMojoTransformer mojoToGradleTransformer = new StripMojoTransformer(classWriter, extensionClass.getName());
		new ClassReader(asStream(GreeterMojoTest.class)).accept(mojoToGradleTransformer, 0);
//		new ClassReader(asStream(extensionClass)).accept(mojoToGradleTransformer, 0);

		assertThat(mojoToGradleTransformer.isMojo()).isFalse();
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
	void exceptionsIsMapped() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		heapWatchMojo.greeter = null;
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);
		Throwable e1 = getExceptionThrown(() -> executeMojo(heapWatchMojo));
		Throwable e2 = getExceptionThrown(() -> executeMojo(transformedMojoInstance));

		assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
		assertThat(packageName(e1)).startsWith("org.apache.maven");
		assertThat(packageName(e2)).doesNotStartWith("org.apache.maven");
	}

	@Test
	void verifyExtensionClassHasNoMethods() throws Exception {
		Class<?> extensionClass = extensionClassOf(transformedMojoInstance(new GreeterMojo()));
		assertThat(stream(extensionClass.getDeclaredMethods()).map(Method::getName)).isEmpty();
	}

	@Test
	void verifyExtensionClassFieldsHaveNoMavenAnnotations() throws Exception {
		Class<?> extensionClass = extensionClassOf(transformedMojoInstance(new GreeterMojo()));
		assertThat(stream(extensionClass.getDeclaredFields()).map(f -> stream(f.getAnnotations())).flatMap(identity()))
				.isEmpty();
	}

	private Class<?> extensionClassOf(Object transformedMojoInstance) {
		return typeOfSingleArgConstructor(transformedMojoInstance);
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
		assertThat(tapSystemOut(statement1)).isEqualTo(tapSystemOut(statement2));
	}

	private static Throwable getExceptionThrown(Statement statement) {
		return assertThrows(InvocationTargetException.class, () -> statement.execute()).getTargetException();
	}

}
