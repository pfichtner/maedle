package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_FAILURE_EXCEPTION;
import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
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
		String originalMojoClassName = originalMojo.getClass().getName();
		TransformationResult result = new TransformationResult(toBytes(asStream(originalMojo.getClass())),
				originalMojoClassName, originalMojoClassName + "GradlePluginExtension");
		return load(originalMojo, result);
	}

	private static Object load(Mojo originalMojo, TransformationResult result) throws Exception {
		AsmClassLoader asmClassLoader = new AsmClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> mojoClass = loadClass(asmClassLoader, result.originalMojoClassName, result.transformedMojo);
		Class<?> extensionClass = loadClass(asmClassLoader, result.extensionClassName, result.extension);
		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static class TransformationResult {

		private final byte[] mojoClass;
		private final String originalMojoClassName;
		private final String extensionClassName;
		private StripMojoTransformer stripMojoTransformer;
		private final byte[] transformedMojo;
		private final byte[] extension;

		public TransformationResult(byte[] mojoClass, String originalMojoClassName, String extensionClassName)
				throws IOException {
			this.mojoClass = mojoClass;
			this.originalMojoClassName = originalMojoClassName;
			this.extensionClassName = extensionClassName;
			this.transformedMojo = mojo();
			this.extension = extension();

		}

		private byte[] mojo() throws IOException {
			ClassWriter cw = newClassWriter();
			stripMojoTransformer = new StripMojoTransformer(cw, extensionClassName.replace('.', '/'))
					.withRemapper(exceptionRemapper());
			read(stripMojoTransformer);
			return cw.toByteArray();
		}

		private byte[] extension() throws IOException {
			ClassWriter cw = newClassWriter();
			MojoToExtensionTransformer mojoToExtensionTransformer = new MojoToExtensionTransformer(cw,
					extensionClassName.replace('.', '/'), stripMojoTransformer.getFilteredFields());
			read(mojoToExtensionTransformer);
			return cw.toByteArray();
		}

		private void read(ClassVisitor cv) throws IOException {
			new ClassReader(mojoClass).accept(trace(cv), EXPAND_FRAMES);
		}

		private ClassWriter newClassWriter() {
			return new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		}

	}

	private static Remapper exceptionRemapper() {
		return new Remapper() {
			@Override
			public String map(String internalName) {
				Type type = org.objectweb.asm.Type.getType(TaskExecutionException.class);
				if (MAVEN_MOJO_FAILURE_EXCEPTION.equals(internalName)) {
					return type.getInternalName();
				} else if (MAVEN_MOJO_EXECUTION_EXCEPTION.equals(internalName)) {
					return type.getInternalName();
				} else {
					return internalName;
				}
			}
		};
	}

	private static ClassVisitor trace(ClassVisitor classVisitor) {
		return new TraceClassVisitor(classVisitor, new PrintWriter(System.out));
	}

	private static Class<?> loadClass(AsmClassLoader asmClassLoader, String string, byte[] byteArray) {
		return asmClassLoader.defineClass(byteArray, string.replace('/', '.'));
	}

}
