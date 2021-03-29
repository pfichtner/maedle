package com.pfichtner.github.maedle.transform;

import java.util.function.Predicate;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public final class Constants {

	public static final Type MOJO_ANNOTATION = Type.getObjectType("org/apache/maven/plugins/annotations/Mojo");

	public static final Type MAVEN_MOJO_FAILURE_EXCEPTION = Type
			.getObjectType("org/apache/maven/plugin/MojoFailureException");

	public static final Type MAVEN_MOJO_EXECUTION_EXCEPTION = Type
			.getObjectType("org/apache/maven/plugin/MojoExecutionException");

	public static final Predicate<? super AnnotationNode> isMojoAnnotation = a -> MOJO_ANNOTATION
			.equals(Type.getType(a.desc));

	public static final Type mavenParameterAnnotation = Type
			.getObjectType("org/apache/maven/plugins/annotations/Parameter");

	private Constants() {
		super();
	}

	public static boolean isMavenException(Type internalType) {
		return MAVEN_MOJO_FAILURE_EXCEPTION.equals(internalType) || MAVEN_MOJO_EXECUTION_EXCEPTION.equals(internalType);
	}

}
