package com.pfichtner.github.maedle.transform;

import java.util.function.Predicate;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public final class Constants {

	public static final Type mojoAnnotation = Type.getObjectType("org/apache/maven/plugins/annotations/Mojo");

	public static final Predicate<? super AnnotationNode> isMojoAnnotation = a -> mojoAnnotation.equals(Type.getType(a.desc));

	public static final Type mavenParameterAnnotation = Type
	.getObjectType("org/apache/maven/plugins/annotations/Parameter");

	private Constants() {
		super();
	}

}
