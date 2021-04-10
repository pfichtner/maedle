package com.github.pfichtner.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo;
import com.github.pfichtner.greeter.mavenplugin.GreeterMojo2;
import com.github.pfichtner.maedle.transform.loader.MojoLoader;
import com.github.stefanbirkner.systemlambda.Statement;
import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.StripMojoTransformer;

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
		GreeterMojo greeterMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedInstance(greeterMojo);
		Class<?> extensionClass = extensionClassOf(transformedMojoInstance);
//		
		ClassWriter classWriter = new ClassWriter(0);

		MojoData mojoData = mojoData(new ClassReader(asStream(GreeterMojoTest.class)));
		StripMojoTransformer mojoToGradleTransformer = new StripMojoTransformer(classWriter, mojoData.getMojoType(),
				Type.getType(extensionClass), mojoData);
		new ClassReader(asStream(GreeterMojoTest.class)).accept(mojoToGradleTransformer, 0);
//		new ClassReader(asStream(extensionClass)).accept(mojoToGradleTransformer, 0);

		assertThat(mojoData.isMojo()).isFalse();
//		assertEquals(bytes, classWriter3.toByteArray());
	}

	@Test
	void transformedMojoHasSameBehaviorLikeOriginalMojo() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		Object transformedMojoInstance = transformedInstance(greeterMojo);
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	// TODO create tests where Mojo fields are public AND where Mojo fields are
	// private and accessed by getters/setters
	void fieldInitializersAreCopiedFromMojoToExtensionClass() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		greeterMojo.greeter = "Stranger";
		Object transformedMojoInstance = transformedInstance(greeterMojo);
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void verifyMojoClassHasNoFields() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		assertThat(stream(transformedInstance(greeterMojo).getClass().getFields())
				.filter(p -> !isStatic(p.getModifiers()))).isEmpty();
	}

	@Test
	void exceptionsIsMapped() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		greeterMojo.greeter = null;
		Object transformedMojoInstance = transformedInstance(greeterMojo);
		Throwable e1 = getExceptionThrown(() -> executeMojo(greeterMojo));
		Throwable e2 = getExceptionThrown(() -> executeMojo(transformedMojoInstance));
		assertAll( //
				() -> assertThat(e1).hasMessage(e2.getMessage()).isInstanceOf(MojoFailureException.class), //
				() -> assertThat(e2).hasMessage(e1.getMessage()).isInstanceOf(TaskExecutionException.class) //
		);
	}

	@Test
	void verifyExtensionClassHasNoMethods() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		Object transformedInstance = transformedInstance(greeterMojo);
		assertThat(stream(extensionClassOf(transformedInstance).getDeclaredMethods()).map(Method::getName)).isEmpty();
	}

	@Test
	void verifyMojoClassAndExtensionsClassFieldsHaveNoMavenAnnotations() throws Exception {
		GreeterMojo greeterMojo = new GreeterMojo();
		MojoLoader mojoLoader = new MojoLoader(greeterMojo);
		ClassNode transformedMojoNode = mojoLoader.transformedMojoNode();
		assertEmpty(nonNull(transformedMojoNode.visibleAnnotations));
		assertEmpty(nonNull(transformedMojoNode.invisibleAnnotations));

		ClassNode extensionNode = mojoLoader.extensionNode();
		assertEmpty(fieldAnnos(extensionNode, f -> f.visibleAnnotations));
		assertEmpty(fieldAnnos(extensionNode, f -> f.invisibleAnnotations));
	}

	private Stream<AnnotationNode> fieldAnnos(ClassNode extensionNode,
			Function<FieldNode, List<AnnotationNode>> function) {
		return extensionNode.fields.stream().map(function).filter(Objects::nonNull).flatMap(Collection::stream);
	}

	private void assertEmpty(List<AnnotationNode> list) {
		assertEmpty(list.stream());
	}

	private void assertEmpty(Stream<AnnotationNode> stream) {
		assertThat(stream.map(n -> n.desc)).isEmpty();
	}

	private static Object transformedInstance(Mojo mojo) throws IOException {
		return new MojoLoader(mojo).transformedInstance();
	}

	private static Class<?> extensionClassOf(Object transformedMojoInstance) {
		return typeOfSingleArgConstructor(transformedMojoInstance);
	}

	private static Class<?> typeOfSingleArgConstructor(Object transformedMojoInstance) {
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
