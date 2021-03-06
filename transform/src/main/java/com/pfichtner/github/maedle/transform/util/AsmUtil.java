package com.pfichtner.github.maedle.transform.util;

import static java.util.Arrays.stream;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.tree.AbstractInsnNode.VAR_INSN;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class AsmUtil {

	public static final String JAVA_LANG_OBJECT = "java/lang/Object";

	private AsmUtil() {
		super();
	}

	public static boolean isNoArgConstructor(MethodNode methodNode) {
		return isConstructor(methodNode) && methodNode.desc.equals("()V");
	}

	public static boolean isConstructor(MethodNode methodNode) {
		return "<init>".equals(methodNode.name);
	}

	public static Map<String, Object> toMap(AnnotationNode annotationNode) {
		Map<String, Object> map = new HashMap<>();
		for (Iterator<Object> it = annotationNode.values.iterator(); it.hasNext();) {
			String key = String.valueOf(it.next());
			Object value = it.next();
			map.put(key, value != null && value.getClass().isArray() ? Arrays.asList((Object[]) value) : value);
		}
		return map;
	}

	public static Type objectTypeToInternal(Type type) {
		if (type.getSort() != Type.OBJECT) {
			throw new IllegalArgumentException(type + " not of type object");
		}
		return Type.getType(type.getDescriptor());
	}

	public static Type append(Type type, String suffix) {
		return Type.getObjectType(type.getInternalName() + suffix);
	}

	public static Stream<AbstractInsnNode> instructions(MethodNode methodNode) {
		return stream(methodNode.instructions.toArray());
	}

	public static Predicate<AbstractInsnNode> isAload(int var) {
		return n -> ((n.getOpcode() == ALOAD) && (n.getType() == VAR_INSN) && (((VarInsnNode) n).var == var));
	}

	public static AbstractInsnNode findNode(AbstractInsnNode node, Predicate<AbstractInsnNode> predicate,
			Function<AbstractInsnNode, AbstractInsnNode> getNode) {
		for (node = getNode.apply(node); node != null; node = getNode.apply(node)) {
			if (predicate.test(node)) {
				return node;
			}
		}
		return null;
	}

	public static int makePublic(int acc) {
		acc &= ~ACC_PRIVATE;
		acc &= ~ACC_PROTECTED;
		acc |= ACC_PUBLIC;
		return acc;
	}

	public static Predicate<? super AnnotationNode> isType(Type type) {
		return n -> type.equals(Type.getType(n.desc));
	}

	public static String[] trySplitOuterInnerClass(String internalName) {
		int idx = internalName.lastIndexOf('$');
		if (idx < 0) {
			return null;
		}
		String outer = internalName.substring(0, idx);
		String inner = internalName.substring(idx + 1);
		return new String[] { outer, inner };
	}

	public static String innerClass(String outer, String inner) {
		return outer + "$" + inner;
	}

	/**
	 * Maps oldType to newType even if <code>typeToMap</code> is an innerclass
	 * (a/b/X$Y)
	 * 
	 * @param oldType
	 * @param newType
	 * @param typeToMap
	 * @return mapped type
	 */
	public static String mapType(Type oldType, Type newType, String typeToMap) {
		String[] split = trySplitOuterInnerClass(typeToMap);
		if (split != null && Type.getObjectType(split[0]).equals(oldType)) {
			return innerClass(newType.getInternalName(), split[1]);
		} else if (Type.getObjectType(typeToMap).equals(oldType)) {
			return newType.getInternalName();
		}
		return typeToMap;
	}

}
