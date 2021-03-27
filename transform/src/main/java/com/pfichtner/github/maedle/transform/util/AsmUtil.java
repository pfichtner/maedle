package com.pfichtner.github.maedle.transform.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public final class AsmUtil {

	private AsmUtil() {
		super();
	}

	public static boolean isNoArgConstructor(MethodNode methodNode) {
		return isConstructor(methodNode) && methodNode.desc.equals("()V");
	}

	private static boolean isConstructor(MethodNode methodNode) {
		return methodNode.name.equals("<init>");
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

	public static MethodNode withInstructions(MethodNode methodNode, AbstractInsnNode... instructions) {
		Arrays.stream(instructions).forEach(methodNode.instructions::add);
		return methodNode;
	}

}
