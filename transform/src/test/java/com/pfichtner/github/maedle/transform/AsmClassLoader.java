package com.pfichtner.github.maedle.transform;

public class AsmClassLoader extends ClassLoader {

	public AsmClassLoader(ClassLoader parent) {
		super(parent);
	}

	public Class<?> defineClass(byte[] bytes, String fqName) {
		return super.defineClass(fqName, bytes, 0, bytes.length);
	}
}