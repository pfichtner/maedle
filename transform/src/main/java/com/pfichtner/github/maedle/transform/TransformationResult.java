package com.pfichtner.github.maedle.transform;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class TransformationResult {

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
		read(new StripMojoTransformer(cw, parameters.getExtensionClass(), parameters.getMojoData())
				.withRemapper(parameters.getRemapper()));
		return cw.toByteArray();
	}

	private byte[] extension() throws IOException {
		ClassWriter cw = newClassWriter();
		read(new MojoToExtensionTransformer(cw, parameters.getExtensionClass().getInternalName(),
				parameters.getMojoData()));
		return cw.toByteArray();
	}

	private void read(ClassVisitor cv) throws IOException {
		new ClassReader(parameters.getMojo()).accept(cv, EXPAND_FRAMES);
	}

	private ClassWriter newClassWriter() {
		return new ClassWriter(COMPUTE_MAXS);
	}

	public byte[] getTransformedMojo() {
		return transformedMojo;
	}

	public byte[] getExtension() {
		return extension;
	}

}