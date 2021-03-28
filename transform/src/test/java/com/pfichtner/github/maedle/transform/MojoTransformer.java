package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_FAILURE_EXCEPTION;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static com.pfichtner.github.maedle.transform.util.IoUtils.toBytes;

import java.io.IOException;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

public class MojoTransformer {

	public static TransformationParameters transform(Class<? extends Mojo> mojoClass) throws IOException {
		TransformationParameters parameters = new TransformationParameters(toBytes(asStream(mojoClass)));
		parameters.setExceptionRemapper(new Remapper() {
			@Override
			public String map(String internalName) {
				Type type = Type.getType(com.pfichtner.github.maedle.transform.TaskExecutionException.class);
				if (MAVEN_MOJO_FAILURE_EXCEPTION.equals(internalName)) {
					return type.getInternalName();
				} else if (MAVEN_MOJO_EXECUTION_EXCEPTION.equals(internalName)) {
					return type.getInternalName();
				} else {
					return internalName;
				}
			}
		});
		return parameters;
	}

}
