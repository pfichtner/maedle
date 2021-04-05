package com.github.pfichtner.maedle.transform.util.jar;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;

import java.io.IOException;

import org.objectweb.asm.Type;

import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

@Deprecated
public class ToResourceTransformer {

	public static interface Resource {
		void add(byte[] content, String path) throws IOException;
	}

	private final Resource resource;

	public ToResourceTransformer(Resource resource) {
		this.resource = resource;
	}

	public void add(TransformationParameters parameters, Type pluginType, PluginInfo pluginInfo) throws IOException {
		TransformationResult result = new TransformationResult(parameters);
		Type mojoType = parameters.getMojoClass();

		add(result.getTransformedMojo(), toPath(mojoType));
		add(result.getExtension(), toPath(parameters.getExtensionClass()));

		add(createPlugin(pluginType, parameters.getExtensionClass(), mojoType, taskName(parameters),
				pluginInfo.extensionName), toPath(pluginType));

		// TODO we should check if file already exists and append content if
		add(("implementation-class=" + pluginType.getInternalName().replace('/', '.')).getBytes(),
				"META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties");
	}

	private String taskName(TransformationParameters parameters) {
		return String.valueOf(parameters.getMojoData().getMojoAnnotationValues().get("name"));
	}

	public void add(byte[] content, String path) throws IOException {
		resource.add(content, path);
	}

	private static String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

}
