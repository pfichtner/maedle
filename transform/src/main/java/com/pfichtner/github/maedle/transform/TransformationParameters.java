package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.MojoClassAnalyser.mojoData;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

public class TransformationParameters {

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
	public Remapper exceptionRemapper;

}