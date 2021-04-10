package com.github.pfichtner.maedle.mojo;

import static com.pfichtner.github.maedle.transform.util.CollectionUtil.functionForMapWithProvider;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class PluginInfoProvider {

	private final List<Mapping> mappings;

	public static class Mapping {

		public String id;
		public String pluginId;
		public String extension;

		public Mapping() {
			super();
		}

		public Mapping(Class<?> mojoClass, String pluginId, String extension) {
			this(mojoClass.getName().replace('.', '/'), pluginId, extension);
		}

		public Mapping(String id, String pluginId, String extension) {
			this.id = id;
			this.pluginId = pluginId;
			this.extension = extension;
		}

	}

	public PluginInfoProvider(Mapping... mappings) {
		this.mappings = asList(mappings);
	}

	public PluginInfoProvider(List<Mapping> mappings) {
		this.mappings = new ArrayList<>(mappings);
	}

	public Function<Type, PluginInfo> pluginFunction() {
		return functionForMapWithProvider(getMappings(), PluginInfoProvider::defaultPluginInfo);
	}

	private static PluginInfo defaultPluginInfo(Type type) {
		String className = type.getClassName();
		int lastDotAt = className.lastIndexOf('.');
		return new PluginInfo(lastDotAt >= 0 ? className.substring(0, lastDotAt) : className, "extname");
	}

	private Map<Type, PluginInfo> getMappings() {
		return nonNull(mappings).stream()
				.collect(toMap(m -> Type.getObjectType(m.id), m -> new PluginInfo(m.pluginId, m.extension)));
	}

}
