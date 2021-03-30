package com.github.pfichtner.maedle.transform.loader;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.*;
import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import com.pfichtner.github.maedle.transform.TransformationParameters;
import com.pfichtner.github.maedle.transform.TransformationResult;

public final class MojoLoader {

	private MojoLoader() {
		super();
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
	public static Object transformedInstance(Mojo mojo) throws Exception {
		TransformationParameters parameters = new TransformationParameters(toBytes(asStream(mojo.getClass())));
		parameters.setExceptionRemapper(new Remapper() {
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
		return load(mojo, parameters, new TransformationResult(parameters));
	}

	private static Object load(Mojo originalMojo, TransformationParameters parameters, TransformationResult result)
			throws Exception {
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
