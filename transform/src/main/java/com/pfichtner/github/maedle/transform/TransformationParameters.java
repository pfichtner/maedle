package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

public class TransformationParameters {

	public static TransformationParameters fromMojo(byte[] mojo) {
		MojoData mojoData = mojoData(new ClassReader(mojo));
		Type mojoClass = mojoData.getMojoType();
		Type extensionClass = Type.getObjectType(mojoClass.getInternalName() + "GradlePluginExtension");
		return new TransformationParameters(mojo, mojoData, mojoClass, extensionClass);
	}

	public TransformationParameters(byte[] mojo, MojoData mojoData, Type mojoClass, Type extensionClass) {
		this.mojo = mojo;
		this.mojoData = mojoData;
		this.mojoClass = mojoClass;
		this.extensionClass = extensionClass;
	}

	private final byte[] mojo;
	private final MojoData mojoData;
	private Type mojoClass;
	private Type extensionClass;
	private Remapper classRemapper;

	public byte[] getMojo() {
		return mojo;
	}

	public MojoData getMojoData() {
		return mojoData;
	}

	public Type getMojoClass() {
		return mojoClass;
	}

	public Type getExtensionClass() {
		return extensionClass;
	}

	public Remapper getRemapper() {
		return classRemapper;
	}

	public TransformationParameters withMojoClass(Type mojoClass) {
		this.mojoClass = mojoClass;
		return this;
	}

	public TransformationParameters withExtensionClass(Type extensionClass) {
		this.extensionClass = extensionClass;
		return this;
	}

	public TransformationParameters withRemapper(Remapper remapper) {
		this.classRemapper = remapper;
		return this;
	}

}