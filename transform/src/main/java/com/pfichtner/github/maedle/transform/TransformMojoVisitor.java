package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.append;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

import org.objectweb.asm.Type;

import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;

public class TransformMojoVisitor extends SimpleFileVisitor<Path> {

	private final PathMatcher matcher;
	private final ResourceAddable resourceAddable;
	private final Function<Type, PluginInfo> infoProvider;
	private boolean copy = true;

	public TransformMojoVisitor(FileSystem fileSystem, ResourceAddable resourceAddable,
			Function<Type, PluginInfo> infoProvider) {
		this.matcher = fileSystem.getPathMatcher("glob:**.class");
		this.resourceAddable = resourceAddable;
		this.infoProvider = infoProvider;
	}

	public TransformMojoVisitor withCopy(boolean copy) {
		this.copy = copy;
		return this;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		byte[] content = read(file);

		if (copy) {
			String path = file.toString();
			resourceAddable.add(content, path.startsWith("/") ? path.substring(1) : path);
		}

		if (matcher.matches(file)) {
			TransformationParameters parameters = fromMojo(content);
			if (parameters.getMojoData().isMojo()) {
				Type originalMojoType = parameters.getMojoClass();
				transformTo(resourceAddable, parameters.withMojoClass(append(originalMojoType, "Rewritten")),
						append(originalMojoType, "GradlePlugin"), getPluginInfo(originalMojoType));
			}
		}

		return CONTINUE;
	}

	private PluginInfo getPluginInfo(Type originalMojoType) {
		return infoProvider.apply(originalMojoType);
	}

	public static void transformTo(ResourceAddable addable, TransformationParameters parameters, Type pluginType,
			PluginInfo pluginInfo) throws IOException {
		TransformationResult result = new TransformationResult(parameters);
		Type mojoType = parameters.getMojoClass();

		addable.add(result.getTransformedMojo(), toPath(mojoType));
		addable.add(result.getExtension(), toPath(parameters.getExtensionClass()));

		addable.add(createPlugin(pluginType, parameters.getExtensionClass(), mojoType, taskName(parameters),
				pluginInfo.extensionName), toPath(pluginType));

		// TODO we should check if file already exists and append content if
		addable.add(("implementation-class=" + pluginType.getInternalName().replace('/', '.')).getBytes(),
				"META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties");
	}

	private static String toPath(Type type) {
		return type.getInternalName() + ".class";
	}

	private static String taskName(TransformationParameters parameters) {
		return String.valueOf(parameters.getMojoData().getMojoAnnotationValues().get("name"));
	}

	private static byte[] read(Path file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(file, baos);
		return baos.toByteArray();
	}
}