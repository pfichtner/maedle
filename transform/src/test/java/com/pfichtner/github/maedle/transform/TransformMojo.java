package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.PrintWriter;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

public class TransformMojo {

	/**
	 * Based on the passed Mojo an new Mojo class is created. The noarg constructor
	 * gets migrated to a constructor having an <code>Extension</code> parameter.
	 * The Extension-Class parameter gets created as well.
	 * 
	 * @param originalMojo
	 * @return
	 * @throws Exception
	 */
	public static Object transformedMojoInstance(Mojo originalMojo) throws Exception {
		String extensionClassName = (originalMojo.getClass().getName() + "GradlePluginExtension").replace('.', '/');

		AsmClassLoader asmClassLoader = new AsmClassLoader(Thread.currentThread().getContextClassLoader());

		ClassWriter classWriter;
		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		StripMojoTransformer stripMojoTransformer = new StripMojoTransformer(classWriter, extensionClassName);
		new ClassReader(asStream(originalMojo.getClass())).accept(trace(stripMojoTransformer), EXPAND_FRAMES);
		Class<?> mojoClass = loadClass(asmClassLoader, classWriter, originalMojo.getClass().getName());

		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		MojoToExtensionTransformer mojoToExtensionTransformer = new MojoToExtensionTransformer(trace(classWriter),
				extensionClassName, stripMojoTransformer.getFilteredFields());
		new ClassReader(asStream(originalMojo.getClass())).accept(mojoToExtensionTransformer, EXPAND_FRAMES);
		Class<?> extensionClass = loadClass(asmClassLoader, classWriter, extensionClassName);

		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static ClassVisitor trace(ClassVisitor classVisitor) {
		return new TraceClassVisitor(classVisitor, new PrintWriter(System.out));
	}

	private static Class<?> loadClass(AsmClassLoader asmClassLoader, ClassWriter classWriter, String string) {
		return asmClassLoader.defineClass(classWriter.toByteArray(), string.replace('/', '.'));
	}

}
