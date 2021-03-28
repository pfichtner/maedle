package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_FAILURE_EXCEPTION;
import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;
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

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

public class MojoTransformer {

	private Class<?> mojoClass;

	public MojoTransformer(Class<?> mojoClass) {
		this.mojoClass = mojoClass;
	}

	/**
	 * Based on the passed Mojo an new Mojo class is created. The noarg constructor
	 * gets migrated to a constructor having an <code>Extension</code> parameter.
	 * The Extension-Class parameter gets created as well.
	 * 
	 * @param mojo
	 * 
	 * @param originalMojo
	 * @return
	 * @throws Exception
	 */
	public Object transformedInstance(Mojo mojo) throws Exception {
		TransformationParameters parameters = new TransformationParameters(toBytes(asStream(mojoClass)),
				mojoClass.getName());
		return load(mojo, parameters, new TransformationResult(parameters));
	}

	private static Object load(Mojo originalMojo, TransformationParameters parameters, TransformationResult result)
			throws Exception {
		AsmClassLoader asmClassLoader = new AsmClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> mojoClass = loadClass(asmClassLoader, parameters.mojoClassName, result.transformedMojo);
		Class<?> extensionClass = loadClass(asmClassLoader, parameters.extensionClassName, result.extension);
		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static class TransformationParameters {
		public TransformationParameters(byte[] mojo, String mojoClassName) {
			this.mojo = mojo;
			this.mojoData = mojoData(new ClassReader(mojo));
			this.mojoClassName = mojoClassName;
			this.extensionClassName = mojoClassName + "GradlePluginExtension";
		}

		public final byte[] mojo;
		public final MojoData mojoData;
		public final String mojoClassName;
		public final String extensionClassName;
	}

	private static class TransformationResult {
		private final TransformationParameters parameters;
		private final byte[] transformedMojo;
		private final byte[] extension;

		public TransformationResult(TransformationParameters parameters) throws IOException {
			this.parameters = parameters;
			this.transformedMojo = mojo();
			this.extension = extension();
		}

		private byte[] mojo() throws IOException {
			ClassWriter cw = newClassWriter();
			read(new StripMojoTransformer(cw, parameters.extensionClassName.replace('.', '/'), parameters.mojoData)
					.withRemapper(exceptionRemapper()));
			return cw.toByteArray();
		}

		private byte[] extension() throws IOException {
			ClassWriter cw = newClassWriter();
			read(new MojoToExtensionTransformer(cw, parameters.extensionClassName.replace('.', '/'),
					parameters.mojoData));
			return cw.toByteArray();
		}

		private void read(ClassVisitor cv) throws IOException {
			new ClassReader(parameters.mojo).accept(trace(cv), EXPAND_FRAMES);
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
