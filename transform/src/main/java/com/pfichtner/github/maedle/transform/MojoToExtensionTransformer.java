package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.isMojoAnnotation;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;

public class MojoToExtensionTransformer extends ClassNode {

	private final ClassVisitor classVisitor;
	private final MojoData mojoData;

	@Override
	public void visitEnd() {
		superName = "java/lang/Object";
		nonNull(invisibleAnnotations).removeIf(isMojoAnnotation);
		nonNull(visibleAnnotations).removeIf(isMojoAnnotation);
		fields.removeIf(f -> !isMojoAttribute(f));
		methods.removeIf(m -> !isConstructor(m) && (refToFields(m).size() != 1 || hasRefToMethod(m)));
		methods.stream().filter(MojoToExtensionTransformer::isConstructor).forEach(this::fixConstructor);
		accept(classVisitor);
	}

	private boolean isMojoAttribute(FieldNode fieldNode) {
		return isMojoAttribute(fieldNode.name);
	}

	private boolean isMojoAttribute(String fieldName) {
		return mojoData.getMojoParameterFields().stream().map(n -> n.name).anyMatch(fieldName::equals);
	}

	private static boolean isConstructor(MethodNode methodNode) {
		return "<init>".equals(methodNode.name);
	}

	private Set<String> refToFields(MethodNode methodNode) {
		return stream(methodNode.instructions.toArray()).filter(n -> n.getType() == FIELD_INSN)
				.map(FieldInsnNode.class::cast).map(n -> n.name).collect(toSet());
	}

	private boolean hasRefToMethod(MethodNode methodNode) {
		return stream(methodNode.instructions.toArray()).anyMatch(n -> n.getType() == METHOD_INSN);
	}

	private void fixConstructor(MethodNode methodNode) {
		replaceSuperCall(methodNode);
		removeFieldInitializers(methodNode);
	}

	private void replaceSuperCall(MethodNode methodNode) {
		AbstractInsnNode node = methodNode.instructions.getFirst();
		while (node != null) {
			if (node.getType() == METHOD_INSN) {
				MethodInsnNode methodInsnNode = (MethodInsnNode) node;
				methodInsnNode.owner = superName;
				methodInsnNode.desc = "()V";
				return;
			}
			node = node.getNext();
		}
	}

	private void removeFieldInitializers(MethodNode methodNode) {
		// TODO this will filter constants assignments (boolean b = true) but what if
		// the fields is initialized using some method call (boolean b = someLogic())?
		AbstractInsnNode insnNode = methodNode.instructions.getFirst();
		if (insnNode.getType() == FIELD_INSN && insnNode.getOpcode() == PUTFIELD) {
			FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
			if (!isMojoAttribute(fieldInsnNode.name)) {
				methodNode.instructions.remove(insnNode);
				methodNode.instructions.remove(insnNode.getPrevious());
			}
		}
	}

	public MojoToExtensionTransformer(ClassVisitor classVisitor, String newClassName, MojoData mojoData) {
		super(ASM9);
		this.classVisitor = new ClassRemapper(classVisitor, new Remapper() {
			@Override
			public String map(String internalName) {
				return internalName.equals(MojoToExtensionTransformer.this.name) ? newClassName
						: super.map(internalName);
			}
		});
		this.mojoData = mojoData;
	}

}
