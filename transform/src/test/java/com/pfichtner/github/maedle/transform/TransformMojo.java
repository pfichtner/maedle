package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.BeanUtil.copyAttributes;
import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.apache.maven.plugin.Mojo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

public class TransformMojo {

	/**
	 * Based on the passed Mojo an new Mojo class is created. The noarg constructor
	 * gets migrated to a constructor having an <code>Extension</code> parameter.
	 * The Extension-Class parameter gets created as well.
	 * 
	 * @param originalMojo
	 * @return
	 * @throws Exception
	 */
	public static Object transformedMojoInstance(Mojo originalMojo) throws Exception {
		String extensionClassName = (originalMojo.getClass().getName() + "GradlePluginExtension").replace('.', '/');

		AsmClassLoader asmClassLoader = new AsmClassLoader(
				new URLClassLoader(urls(), Thread.currentThread().getContextClassLoader()));

		ClassWriter classWriter;
		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		StripMojoTransformer stripMojoTransformer = new StripMojoTransformer(classWriter, extensionClassName);
		new ClassReader(asStream(originalMojo.getClass())).accept(trace(stripMojoTransformer), EXPAND_FRAMES);
		Class<?> mojoClass = loadClass(asmClassLoader, classWriter, originalMojo.getClass().getName());

		classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		MojoToExtensionTransformer mojoToExtensionTransformer = new MojoToExtensionTransformer(trace(classWriter),
				extensionClassName, stripMojoTransformer.getFilteredFields());
		new ClassReader(asStream(originalMojo.getClass())).accept(mojoToExtensionTransformer, EXPAND_FRAMES);
		Class<?> extensionClass = loadClass(asmClassLoader, classWriter, extensionClassName);

		Object extension = extensionClass.newInstance();
		return mojoClass.getConstructor(extension.getClass()).newInstance(copyAttributes(originalMojo, extension));
	}

	private static ClassVisitor trace(ClassVisitor classVisitor) {
		return new TraceClassVisitor(classVisitor, new PrintWriter(System.out));
	}

	private static Class<?> loadClass(AsmClassLoader asmClassLoader, ClassWriter classWriter, String string) {
		return asmClassLoader.defineClass(classWriter.toByteArray(), string.replace('/', '.'));
	}

	private static URL[] urls() throws MalformedURLException {
		//TODO fix me
		String base = "/home/xck10h6/.m2/repository/";
		return Stream.of( //
//				"org/gradle/gradle-core/5.6.4/gradle-core-5.6.4.jar" //
				"org/gradle/gradle-core-api/5.6.4/gradle-core-api-5.6.4.jar" //
//				"org/gradle/gradle-logging/5.6.4/gradle-logging-5.6.4.jar", //
//				"org/codehaus/groovy/groovy/2.5.14/groovy-2.5.14.jar", //
//				"org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar" //
		).map(base::concat).map(File::new).map(File::toURI).map(TransformMojo::toURL).toArray(URL[]::new);
	}

	private static URL toURL(URI uri) {
		try {
			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
