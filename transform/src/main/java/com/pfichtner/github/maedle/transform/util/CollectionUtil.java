package com.pfichtner.github.maedle.transform.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CollectionUtil {

	private CollectionUtil() {
		super();
	}

	public static <T> List<T> nonNull(List<T> list) {
		return list == null ? emptyList() : list;
	}

	public static <T> Set<T> nonNull(Set<T> set) {
		return set == null ? emptySet() : set;
	}

	public static <K, V> Map<K, V> nonNull(Map<K, V> map) {
		return map == null ? emptyMap() : map;
	}

	public static <K, V> Function<K, V> functionForMapWithDefault(Map<K, V> map, V defValue) {
		return functionForMapWithProvider(map, (ign) -> defValue);
	}

	public static <K, V> Function<K, V> functionForMapWithProvider(Map<K, V> map, Function<K, V> defValue) {
		return key -> {
			V value = map.get(key);
			return value == null ? defValue.apply(key) : value;
		};
	}

	@SafeVarargs
	public static <T> List<T> combine(List<T>... lists) {
		return Arrays.stream(lists).flatMap(Collection::stream).collect(toList());
	}

}
