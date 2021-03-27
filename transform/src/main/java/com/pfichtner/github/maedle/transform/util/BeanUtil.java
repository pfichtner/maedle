package com.pfichtner.github.maedle.transform.util;

import static java.util.Arrays.stream;

import java.lang.reflect.Field;

public final class BeanUtil {

	private BeanUtil() {
		super();
	}

	public static Object copyAttributes(Object from, Object to) {
		for (Field field : to.getClass().getDeclaredFields()) {
			stream(from.getClass().getDeclaredFields()).filter(f -> f.getName().equals(field.getName())).findFirst()
					.ifPresent(f -> {
						try {
							field.set(to, f.get(from));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					});

		}
		return to;
	}

}
