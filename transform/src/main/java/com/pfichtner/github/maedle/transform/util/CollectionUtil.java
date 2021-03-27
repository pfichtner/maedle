package com.pfichtner.github.maedle.transform.util;

import static java.util.Collections.emptyList;

import java.util.List;

public final class CollectionUtil {

	private CollectionUtil() {
		super();
	}

	public static <T> List<T> nonNull(List<T> list) {
		return list == null ? emptyList() : list;
	}

}
