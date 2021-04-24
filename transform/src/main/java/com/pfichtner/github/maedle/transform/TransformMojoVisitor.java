package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.PluginWriter.createPlugin;
import static com.pfichtner.github.maedle.transform.TransformationParameters.fromMojo;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.append;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.InnerClassNode;

import com.github.pfichtner.maedle.transform.util.jar.PluginInfo;
import com.pfichtner.github.maedle.transform.util.AsmUtil;

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
				PluginInfo pluginInfo = getPluginInfo(originalMojoType);
				if (pluginInfo != null) {
					transformTo(file, resourceAddable, parameters, originalMojoType, pluginInfo);
				}
			}
		}

		return CONTINUE;
	}

	private PluginInfo getPluginInfo(Type originalMojoType) {
		return infoProvider.apply(originalMojoType);
	}

	private static void transformTo(Path file, ResourceAddable resourceAddable, TransformationParameters parametersArg,
			Type originalMojoType, PluginInfo pluginInfo) throws IOException {
		TransformationParameters parameters = parametersArg.withMojoClass(append(originalMojoType, "Rewritten"));
		Type pluginType = append(originalMojoType, "GradlePlugin");
		TransformationResult result = new TransformationResult(parameters);
		Type mojoType = parameters.getMojoClass();

		List<InnerClassNode> innerClasses = parameters.getMojoData().getInnerClasses().collect(toList());

		resourceAddable.add(result.getTransformedMojo(), toPath(mojoType));
		for (InnerClassNode innerClassNode : innerClasses) {
			readInnerClass(resourceAddable, file, innerClassNode, parameters.getMojoClass());
		}

		resourceAddable.add(result.getExtension(), toPath(parameters.getExtensionClass()));
		for (InnerClassNode innerClassNode : innerClasses) {
			readInnerClass(resourceAddable, file, innerClassNode, parameters.getExtensionClass());
		}

		resourceAddable.add(createPlugin(pluginType, parameters.getExtensionClass(), mojoType, taskName(parameters),
				pluginInfo.extensionName), toPath(pluginType));

		// TODO we should check if file already exists and append content if
		resourceAddable.add(("implementation-class=" + pluginType.getInternalName().replace('/', '.')).getBytes(),
				"META-INF/gradle-plugins/" + pluginInfo.pluginId + ".properties");
	}

	private static void readInnerClass(ResourceAddable resourceAddable, Path file, InnerClassNode innerClassNode,
			Type newOuter) throws IOException {
		Path directory = file.subpath(0, file.getNameCount() - 1);
		Path oldInner = directory
				.resolve(simpleName(innerClassNode.outerName) + "$" + innerClassNode.innerName + ".class");
		byte[] transformed = transform(read(oldInner), Type.getObjectType(innerClassNode.outerName), newOuter);
		resourceAddable.add(transformed,
				directory.resolve(simpleName(newOuter.getInternalName()) + "$" + innerClassNode.innerName + ".class")
						.toString());
	}

	private static String simpleName(String name) {
		int lastSlash = name.lastIndexOf('/');
		return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
	}

	private static byte[] transform(byte[] classContent, Type oldOuter, Type newOuter) {
		ClassWriter cw = new ClassWriter(0);
		ClassReader cr = new ClassReader(classContent);
		cr.accept(new ClassRemapper(cw, new Remapper() {
			@Override
			public String mapType(String internalName) {
				return AsmUtil.mapType(oldOuter, newOuter, internalName);
			}
		}), 0);
		return cw.toByteArray();
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