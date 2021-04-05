package com.pfichtner.github.maedle.transform;

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
import java.io.InputStream;
import java.util.Objects;

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

	public static byte[] createPlugin(Type pluginClass, Type extensionClass, Type mojoClass, String taskName,
			String extensionName) {
		return createPlugin(pluginClass.getInternalName(), extensionClass.getInternalName(),
				mojoClass.getInternalName(), taskName, extensionName);
	}

	public static byte[] createPlugin(String pluginClass, String extensionClass, String mojoClass, String taskName,
			String extensionName) {
		try {
			return createPluginMixin(pluginClass, extensionClass, mojoClass, taskName, extensionName);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] createPluginMixin(String pluginClass, String extensionClass, String mojoClass,
			String taskName, String extensionName)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		Type pluginType = Type.getObjectType("com/pfichtner/github/maedle/MaedlePluginTemplate");
		Type extensionType = Type.getType(com.pfichtner.github.maedle.E.class);
		Type mojoType = Type.getType(com.pfichtner.github.maedle.M.class);

		ClassWriter cw1 = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);
		try {

			ClassRemapper cw2 = new ClassRemapper(cw1, new Remapper() {
				@Override
				public String map(String internalName) {
					Type type = Type.getObjectType(internalName);
					if (type.equals(pluginType)) {
						return pluginClass;
					} else if (type.equals(extensionType)) {
						return extensionClass;
					} else if (type.equals(mojoType)) {
						return mojoClass;
					} else {
						return internalName;
					}
				}
			});

			ClassVisitor cw3 = new ClassVisitor(ASM9, cw2) {

				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature,
						Object value) {
					if (name.equals("T_NAME")) {
						value = taskName;
					} else if (name.equals("E_NAME")) {
						value = extensionName;
					}
					return super.visitField(access, name, descriptor, signature, value);
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature,
						String[] exceptions) {
					return new MethodVisitor(ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {

						@Override
						public void visitLdcInsn(Object cst) {
							// replace compile time constants
							if (Objects.equals(MaedlePluginTemplate.T_NAME, cst)) {
								cst = taskName;
							} else if (Objects.equals(MaedlePluginTemplate.E_NAME, cst)) {
								cst = extensionName;
							}
							super.visitLdcInsn(cst);
						}
					};
				}
			};
			new ClassReader(stream(pluginType)).accept(cw3, EXPAND_FRAMES);
			return cw1.toByteArray();
		} catch (IOException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private static InputStream stream(Type pluginType) {
		return PluginWriter.class.getClassLoader().getResourceAsStream(pluginType.getInternalName() + ".class");
	}

	private static byte[] createPluginAsm(String pluginClass, String extensionClass, String mojoClass, String taskName,
			String extensionName) {
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
