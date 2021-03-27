package com.pfichtner.github.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.apache.maven.plugin.Mojo;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceClassVisitor;

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
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);
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

	@Test
	// TODO this should be done in a gradle project with the usage of testkit
	void canTransformHeapWatchMojo() throws Exception {
		// TODO Do not forget to write META-INF! ;-)
		GreeterMojo heapWatchMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedMojoInstance(heapWatchMojo);

		AsmClassLoader asmClassLoader = new AsmClassLoader(new URLClassLoader(urls(), getClass().getClassLoader()));

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

	private static Object executeMojo(Object mojo)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return mojo.getClass().getMethod("execute").invoke(mojo);
	}

	private static void haveSameSysouts(Statement statement1, Statement statement2) throws Exception {
		assertEquals(tapSystemOut(statement1), tapSystemOut(statement2));
	}

	private Object transformedMojoInstance(Mojo originalMojo) throws MalformedURLException, IOException,
			InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String extensionClassName = (originalMojo.getClass().getName() + "GradlePluginExtension").replace('.', '/');

		AsmClassLoader asmClassLoader = new AsmClassLoader(new URLClassLoader(urls(), getClass().getClassLoader()));

		ClassWriter classWriter;
		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		StripMojoTransformer stripMojoTransformer = new StripMojoTransformer(classWriter, extensionClassName);
		new ClassReader(asStream(originalMojo.getClass())).accept(trace(stripMojoTransformer), EXPAND_FRAMES);
		Class<?> mojoClass = loadClass(asmClassLoader, classWriter, originalMojo.getClass().getName());

		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		MojoToExtensionTransformer mojoToExtensionTransformer = new MojoToExtensionTransformer(trace(classWriter),
				extensionClassName, stripMojoTransformer.getFilteredFields());
		new ClassReader(asStream(originalMojo.getClass())).accept(mojoToExtensionTransformer, EXPAND_FRAMES);
		Class<?> extensionClass = loadClass(asmClassLoader, classWriter, extensionClassName);

		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private ClassVisitor trace(ClassVisitor classVisitor) {
		return new TraceClassVisitor(classVisitor, new PrintWriter(System.out));
	}

	private Class<?> loadClass(AsmClassLoader asmClassLoader, ClassWriter classWriter, String string) {
		return asmClassLoader.defineClass(classWriter.toByteArray(), string.replace('/', '.'));
	}

	private URL[] urls() throws MalformedURLException {
		String base = "/home/xck10h6/.m2/repository/";
		return Stream.of( //
//				"org/gradle/gradle-core/5.6.4/gradle-core-5.6.4.jar" //
				"org/gradle/gradle-core-api/5.6.4/gradle-core-api-5.6.4.jar" //
//				"org/gradle/gradle-logging/5.6.4/gradle-logging-5.6.4.jar", //
//				"org/codehaus/groovy/groovy/2.5.14/groovy-2.5.14.jar", //
//				"org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar" //
		).map(base::concat).map(File::new).map(File::toURI).map(GreeterMojoTest::toURL).toArray(URL[]::new);
	}

	private static URL toURL(URI uri) {
		try {
			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
