package com.pfichtner.github.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;
import static com.pfichtner.github.maedle.transform.loader.MojoLoader.transformedInstance;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.stefanbirkner.systemlambda.Statement;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

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
		Object transformedMojoInstance = transformedInstance(heapWatchMojo);
		Class<?> extensionClass = extensionClassOf(transformedMojoInstance);
//		
		ClassWriter classWriter = new ClassWriter(0);

		MojoData mojoData = mojoData(new ClassReader(asStream(GreeterMojoTest.class)));
		StripMojoTransformer mojoToGradleTransformer = new StripMojoTransformer(classWriter, extensionClass.getName(),
				mojoData);
		new ClassReader(asStream(GreeterMojoTest.class)).accept(mojoToGradleTransformer, 0);
//		new ClassReader(asStream(extensionClass)).accept(mojoToGradleTransformer, 0);

		assertThat(mojoData.isMojo()).isFalse();
//		assertEquals(bytes, classWriter3.toByteArray());
	}

	@Test
	void transformedMojoHasSameBehaviorLikeOriginalMojo() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedInstance(heapWatchMojo);
		haveSameSysouts(() -> executeMojo(heapWatchMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	// TODO create tests where Mojo fields are public AND where Mojo fields are
	// private and accessed by getters/setters
	void fieldInitializersAreCopiedFromMojoToExtensionClass() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		heapWatchMojo.greeter = "Stranger";
		Object transformedMojoInstance = transformedInstance(heapWatchMojo);
		haveSameSysouts(() -> executeMojo(heapWatchMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void exceptionsIsMapped() throws Exception {
		GreeterMojo heapWatchMojo = new GreeterMojo();
		heapWatchMojo.greeter = null;
		Object transformedMojoInstance = transformedInstance(heapWatchMojo);
		Throwable e1 = getExceptionThrown(() -> executeMojo(heapWatchMojo));
		Throwable e2 = getExceptionThrown(() -> executeMojo(transformedMojoInstance));
		assertAll( //
				() -> assertThat(e1).hasMessage(e2.getMessage()).isInstanceOf(MojoFailureException.class), //
				() -> assertThat(e2).hasMessage(e1.getMessage()).isInstanceOf(TaskExecutionException.class) //
		);
	}

	@Test
	void verifyExtensionClassHasNoMethods() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		Class<?> extensionClass = extensionClassOf(transformedInstance(greeterMojo));
		assertThat(stream(extensionClass.getDeclaredMethods()).map(Method::getName)).isEmpty();
	}

	@Test
	void verifyExtensionClassFieldsHaveNoMavenAnnotations() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		Class<?> extensionClass = extensionClassOf(transformedInstance(greeterMojo));
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
