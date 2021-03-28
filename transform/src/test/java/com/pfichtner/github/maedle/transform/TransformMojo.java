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

public class TransformMojo {

	private Mojo originalMojo;

	public TransformMojo(Mojo originalMojo) {
		this.originalMojo = originalMojo;
	}

	/**
	 * Based on the passed Mojo an new Mojo class is created. The noarg constructor
	 * gets migrated to a constructor having an <code>Extension</code> parameter.
	 * The Extension-Class parameter gets created as well.
	 * 
	 * @param originalMojo
	 * @return
	 * @throws Exception
	 */
	public Object transformMojoInstance() throws Exception {
		byte[] mojoClass = toBytes(asStream(originalMojo.getClass()));
		TransformationResult result = transform(mojoClass);
		return load(originalMojo, result);
	}

	public TransformationResult transform(byte[] mojo) throws IOException {
		TransformationParameters parameters = new TransformationParameters();
		parameters.mojo = mojo;
		parameters.mojoData = mojoData(new ClassReader(mojo));
		parameters.originalMojoClassName = originalMojo.getClass().getName();
		parameters.extensionClassName = parameters.originalMojoClassName + "GradlePluginExtension";
		byte[] transformedMojo = mojo(parameters);
		byte[] extension = extension(parameters);
		return new TransformationResult(parameters, transformedMojo, extension);
	}

	private static Object load(Mojo originalMojo, TransformationResult result) throws Exception {
		AsmClassLoader asmClassLoader = new AsmClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> mojoClass = loadClass(asmClassLoader, result.parameters.originalMojoClassName, result.transformedMojo);
		Class<?> extensionClass = loadClass(asmClassLoader, result.parameters.extensionClassName, result.extension);
		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static class TransformationParameters {
		public byte[] mojo;
		public MojoData mojoData;
		public String originalMojoClassName;
		public String extensionClassName;
	}

	private static class TransformationResult {
		private final TransformationParameters parameters;
		private final byte[] transformedMojo;
		private final byte[] extension;

		public TransformationResult(TransformationParameters parameters, byte[] transformedMojo2, byte[] extension2)
				throws IOException {
			this.parameters = parameters;
			this.transformedMojo = transformedMojo2;
			this.extension = extension2;
		}

	}

	private byte[] mojo(TransformationParameters parameters) throws IOException {
		ClassWriter cw = newClassWriter();
		read(parameters,
				new StripMojoTransformer(cw, parameters.extensionClassName.replace('.', '/'), parameters.mojoData)
						.withRemapper(exceptionRemapper()));
		return cw.toByteArray();
	}

	private byte[] extension(TransformationParameters parameters) throws IOException {
		ClassWriter cw = newClassWriter();
		read(parameters, new MojoToExtensionTransformer(cw, parameters.extensionClassName.replace('.', '/'),
				parameters.mojoData));
		return cw.toByteArray();
	}

	private void read(TransformationParameters parameters, ClassVisitor cv) throws IOException {
		new ClassReader(parameters.mojo).accept(trace(cv), EXPAND_FRAMES);
	}

	private ClassWriter newClassWriter() {
		return new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
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
