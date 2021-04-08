package com.github.pfichtner.maedle.transform.loader;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.*;
import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public final class MojoLoader {

	private final Mojo mojo;
	private TransformationParameters parameters;

	public MojoLoader(Mojo mojo) throws IOException {
		this.mojo = mojo;
		this.parameters = TransformationParameters.fromMojo(toBytes(asStream(mojo.getClass())));
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
	public Object transformedInstance() throws IOException {
		try {
			return load(mojo, parameters, transform());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public TransformationResult transform() throws IOException {
		parameters.withRemapper(new Remapper() {
			@Override
			public String map(String internalName) {
				Type type = Type.getType(com.github.pfichtner.maedle.transform.TaskExecutionException.class);
				if (MAVEN_MOJO_FAILURE_EXCEPTION.equals(Type.getObjectType(internalName))) {
					return type.getInternalName();
				} else if (MAVEN_MOJO_EXECUTION_EXCEPTION.equals(Type.getObjectType(internalName))) {
					return type.getInternalName();
				} else {
					return internalName;
				}
			}
		});
		return new TransformationResult(parameters);
	}

	public ClassNode transformedMojoNode() throws IOException {
		return node(transform().getTransformedMojo());
	}

	public ClassNode extensionNode() throws IOException {
		return node(transform().getExtension());
	}

	private ClassNode node(byte[] bytes) {
		ClassReader cr = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		cr.accept(node, 0);
		return node;
	}

	private static Object load(Mojo originalMojo, TransformationParameters parameters, TransformationResult result)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		AsmClassLoader asmClassLoader = new AsmClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> mojoClass = loadClass(asmClassLoader, parameters.getMojoData().getMojoType(),
				result.getTransformedMojo());
		Class<?> extensionClass = loadClass(asmClassLoader, parameters.getExtensionClass(), result.getExtension());
		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static Class<?> loadClass(AsmClassLoader asmClassLoader, Type type, byte[] byteArray) {
		return asmClassLoader.defineClass(byteArray, type.getClassName());
	}

}
