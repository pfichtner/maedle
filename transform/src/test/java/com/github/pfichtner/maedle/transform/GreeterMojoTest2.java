package com.github.pfichtner.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.plugin.Mojo;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo2;
import com.github.pfichtner.maedle.transform.loader.MojoLoader;
import com.github.stefanbirkner.systemlambda.Statement;

public class GreeterMojoTest2 {

	@Test
	void transformedMojoHasSameBehaviorLikeOriginalMojo() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		Object transformedMojoInstance = transformedInstance(greeterMojo);
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void fieldInitializersAreCopiedFromMojoToExtensionClass() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		greeterMojo.setMessage("Message from JUnit");

		Object transformedMojoInstance = transformedInstance(greeterMojo);
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void verifyExtensionClassHasNoMethods() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		Class<?> extensionClass = extensionClassOf(transformedInstance(greeterMojo));
		assertThat(stream(extensionClass.getDeclaredMethods()).map(Method::getName)).isEmpty();
	}

	@Test
	void verifyMojoClassAndExtensionsClassFieldsHaveNoMavenAnnotations() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
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

	private static Object transformedInstance(Mojo mojo) throws Exception, IOException {
		return new MojoLoader(mojo).transformedInstance();
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

}
