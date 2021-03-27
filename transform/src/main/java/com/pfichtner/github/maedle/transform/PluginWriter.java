package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.ClassUtils.asStream;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.IOException;
import java.lang.reflect.Field;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import com.pfichtner.github.maedle.MaedlePluginTemplate;

public class PluginWriter {

	public static byte[] createPlugin(String pluginClass, String extensionClass, String mojoClass, String taskName,
			String extensionName) throws Exception {
		return createPluginMixin(pluginClass, extensionClass, mojoClass, taskName, extensionName);
	}

	private static byte[] createPluginMixin(String pluginClass, String extensionClass, String mojoClass,
			String taskName, String extensionName) {
		// TODO handle taskName
		// TODO handle extensionName

		ClassWriter cw = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);
		try {

			ClassVisitor cw2 = new ClassVisitor(ASM9, cw) {

				Class<?> plugin = MaedlePluginTemplate.class;
				Field extensionField = plugin.getDeclaredField("EXTENSION");
				Field taskField = plugin.getDeclaredField("TASK");

				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature,
						Object value) {
					if (name.equals(taskField.getName())) {
						value = taskName;
					} else if (name.equals(extensionField.getName())) {
						value = extensionName;
					}
					return super.visitField(access, name, descriptor, signature, value);
				}
			};

			ClassRemapper remapper = new ClassRemapper(cw2, new Remapper() {
				@Override
				public String map(String internalName) {
					if (internalName.equals("com/pfichtner/github/maedle/MaedlePluginTemplate")) {
						return pluginClass;
					} else if (internalName.equals("com/pfichtner/github/maedle/TransformedExtension")) {
						return extensionClass;
					} else if (internalName.equals("com/pfichtner/github/maedle/TransformedMojo")) {
						return mojoClass;
					} else {
						return internalName;
					}
				}

			});

			new ClassReader(asStream(MaedlePluginTemplate.class)).accept(remapper, EXPAND_FRAMES);
			return cw.toByteArray();
		} catch (IOException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] createPluginAsm(String pluginClass, String extensionClass, String mojoClass, String taskName,
			String extensionName) throws Exception {
		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, pluginClass,
				"Ljava/lang/Object;Lorg/gradle/api/Plugin<Lorg/gradle/api/Project;>;", "java/lang/Object",
				new String[] { "org/gradle/api/Plugin" });

		cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup",
				ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

		{
			fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "TASK", "Ljava/lang/String;", null, taskName);
			fv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Lorg/gradle/api/Project;)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/gradle/api/Project", "getExtensions",
					"()Lorg/gradle/api/plugins/ExtensionContainer;", true);
			mv.visitLdcInsn(extensionName);
			mv.visitLdcInsn(Type.getType("L" + extensionClass + ";"));
			mv.visitInsn(ICONST_0);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitMethodInsn(INVOKEINTERFACE, "org/gradle/api/plugins/ExtensionContainer", "create",
					"(Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, extensionClass);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("greet");
			mv.visitMethodInsn(INVOKEINTERFACE, "org/gradle/api/Project", "task",
					"(Ljava/lang/String;)Lorg/gradle/api/Task;", true);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInvokeDynamicInsn("execute", "(L" + extensionClass + ";)Lorg/gradle/api/Action;",
					new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(" //
							+ "Ljava/lang/invoke/MethodHandles$Lookup;" //
							+ "Ljava/lang/String;" //
							+ "Ljava/lang/invoke/MethodType;" //
							+ "Ljava/lang/invoke/MethodType;" //
							+ "Ljava/lang/invoke/MethodHandle;" //
							+ "Ljava/lang/invoke/MethodType;" //
							+ ")" //
							+ "Ljava/lang/invoke/CallSite;" //
							, false),
					new Object[] { //
							Type.getType("(Ljava/lang/Object;)V"),
							new Handle(H_INVOKESTATIC, pluginClass, "lambda$apply$0", "(" //
									+ "L" + extensionClass + ";" //
									+ "Lorg/gradle/api/Task;" //
									+ ")V", false), //
							Type.getType("(Lorg/gradle/api/Task;)V") //
					});
			mv.visitMethodInsn(INVOKEINTERFACE, "org/gradle/api/Task", "doLast",
					"(Lorg/gradle/api/Action;)Lorg/gradle/api/Task;", true);
			mv.visitInsn(POP);
			mv.visitInsn(RETURN);
			mv.visitMaxs(4, 3);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", "(Ljava/lang/Object;)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, "org/gradle/api/Project");
			mv.visitMethodInsn(INVOKEVIRTUAL, pluginClass, "apply", "(Lorg/gradle/api/Project;)V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, "lambda$apply$0",
					"(L" + extensionClass + ";Lorg/gradle/api/Task;)V", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, mojoClass);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, mojoClass, "<init>", "(L" + extensionClass + ";)V", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, mojoClass, "execute", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}

}
