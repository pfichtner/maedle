package com.pfichtner.github.maedle.transform.util;

import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static java.util.Arrays.stream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Predicate;

public final class ClassUtils {

	private ClassUtils() {
		super();
	}

	public static InputStream asStream(Class<?> clazz) throws IOException {
		return new ByteArrayInputStream(toBytes(clazz.getClassLoader().getResourceAsStream(pathToClass(clazz))));
	}

	public static File asFile(Class<?> clazz) throws IOException, URISyntaxException {
		URL resource = clazz.getClassLoader().getResource(pathToClass(clazz));
		return resource == null ? null : new File(resource.toURI());
	}

	private static String pathToClass(Class<?> clazz) {
		return clazz.getName().replace('.', '/') + ".class";
	}

	public static Constructor<?> constructor(Class<?> clazz, Predicate<? super Constructor<?>> predicate) {
		return stream(clazz.getConstructors()).filter(predicate).findFirst()
				.orElseThrow(() -> new IllegalStateException("no constructor " + predicate));
	}

}
