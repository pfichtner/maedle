package com.github.pfichtner.maedle.transform.util.jar;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;

import java.io.IOException;

import org.objectweb.asm.Type;

import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public class ToJarAdder {

	private JarModifier jarWriter;

	public ToJarAdder(JarModifier jarWriter) {
		this.jarWriter = jarWriter;
	}

	public void add(TransformationParameters parameters, Type pluginType, PluginInfo pluginInfo) throws IOException {
		TransformationResult result = new TransformationResult(parameters);
		Type mojoType = parameters.getMojoClass();

		add(result.getTransformedMojo(), toPath(mojoType));
		add(result.getExtension(), toPath(parameters.getExtensionClass()));

		add(createPlugin(pluginType, parameters.getExtensionClass(), mojoType, pluginInfo.taskName,
				pluginInfo.extensionName), toPath(pluginType));

		// TODO we should check if file already exists and append content if
		add(("implementation-class=" + pluginType.getInternalName().replace('/', '.')).getBytes(),
				"META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties");
	}

	public void add(byte[] content, String path) throws IOException {
		jarWriter.add(content, path);
	}

	private static String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

}
