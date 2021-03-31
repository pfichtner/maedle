package com.github.pfichtner.maedle.transform;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.constructor;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.github.pfichtner.greeter.mavenplugin.GreeterMojo2;
import com.github.pfichtner.maedle.transform.loader.MojoLoader;
import com.github.stefanbirkner.systemlambda.Statement;

public class GreeterMojoTest2 {

	@Test
	void transformedMojoHasSameBehaviorLikeOriginalMojo() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		Object transformedMojoInstance = new MojoLoader(greeterMojo).transformedInstance();
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void fieldInitializersAreCopiedFromMojoToExtensionClass() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		greeterMojo.setMessage("Message from JUnit");

		Object transformedMojoInstance = new MojoLoader(greeterMojo).transformedInstance();
		haveSameSysouts(() -> executeMojo(greeterMojo), () -> executeMojo(transformedMojoInstance));
	}

	@Test
	void verifyExtensionClassHasSameMethodsLikeMojo() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		Class<?> extensionClass = extensionClassOf(new MojoLoader(greeterMojo).transformedInstance());
		assertThat(stream(extensionClass.getDeclaredMethods()).map(Method::getName))
				.containsExactlyInAnyOrderElementsOf(stream(greeterMojo.getClass().getDeclaredMethods())
						.map(Method::getName).filter(n -> !n.equals("execute")).collect(toList()));
	}

	@Test
	void verifyExtensionClassFieldsHaveNoMavenAnnotations() throws Exception {
		GreeterMojo2 greeterMojo = new GreeterMojo2();
		Class<?> extensionClass = extensionClassOf(new MojoLoader(greeterMojo).transformedInstance());
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
