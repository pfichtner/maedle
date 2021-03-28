package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

public class TransformationParameters {

	private final byte[] mojo;
	private final MojoData mojoData;
	private final Type extensionClass;
	private Remapper exceptionRemapper;

	public TransformationParameters(byte[] mojo) {
		this.mojo = mojo;
		this.mojoData = mojoData(new ClassReader(mojo));
		this.extensionClass = Type
				.getObjectType(getMojoData().getMojoType().getInternalName() + "GradlePluginExtension");
	}

	public byte[] getMojo() {
		return mojo;
	}

	public MojoData getMojoData() {
		return mojoData;
	}

	public Type getExtensionClass() {
		return extensionClass;
	}

	public Remapper getExceptionRemapper() {
		return exceptionRemapper;
	}

	public void setExceptionRemapper(Remapper exceptionRemapper) {
		this.exceptionRemapper = exceptionRemapper;
	}

}