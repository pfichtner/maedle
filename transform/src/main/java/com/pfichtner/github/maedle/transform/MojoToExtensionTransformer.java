package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.isMojoAnnotation;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.JAVA_LANG_OBJECT;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.instructions;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.isConstructor;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

import java.util.BitSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.pfichtner.github.maedle.transform.MojoClassAnalyser.MojoData;
import com.pfichtner.github.maedle.transform.util.AsmUtil;

public class MojoToExtensionTransformer extends ClassNode {

	private final ClassVisitor classVisitor;
	private final MojoData mojoData;

	@Override
	public void visitEnd() {
		superName = JAVA_LANG_OBJECT;
		nonNull(invisibleAnnotations).removeIf(isMojoAnnotation);
		nonNull(visibleAnnotations).removeIf(isMojoAnnotation);
		nonNull(fields).removeIf(f -> !isMojoAttribute(f));
		nonNull(fields).stream().filter(this::isMojoAttribute).forEach(MojoToExtensionTransformer::makePublic);
		nonNull(methods).removeIf(m -> !isConstructor(m) && !refSingleFieldOnly(m));
		nonNull(methods).stream().filter(m -> isConstructor(m)).forEach(this::fixConstructor);
		accept(classVisitor);
	}

	private boolean isMojoAttribute(FieldNode fieldNode) {
		return isMojoAttribute(fieldNode.name);
	}

	private boolean isMojoAttribute(String fieldName) {
		return mojoData.getMojoParameterFields().stream().map(n -> n.name).anyMatch(fieldName::equals);
	}

	private static void makePublic(FieldNode fieldNode) {
		fieldNode.access = AsmUtil.makePublic(fieldNode.access);
	}

	private boolean refSingleFieldOnly(MethodNode methodNode) {
		return referencedFieldNames(methodNode).count() == 1 && !hasRefToAnyMethod(methodNode);
	}

	private Stream<String> referencedFieldNames(MethodNode methodNode) {
		return fieldInsnNode(methodNode).map(n -> n.name).distinct();
	}

	private Stream<FieldInsnNode> fieldInsnNode(MethodNode methodNode) {
		return instructions(methodNode).filter(typeIs(FIELD_INSN)).map(FieldInsnNode.class::cast);
	}

	private boolean hasRefToAnyMethod(MethodNode methodNode) {
		return instructions(methodNode).anyMatch(typeIs(METHOD_INSN));
	}

	private static Predicate<AbstractInsnNode> typeIs(int type) {
		return n -> n.getType() == type;
	}

	private void fixConstructor(MethodNode methodNode) {
		replaceSuperCallToNewSuperName(methodNode);
		removeFieldInitializersOfRemovedFields(methodNode);
	}

	private void replaceSuperCallToNewSuperName(MethodNode methodNode) {
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

	private void removeFieldInitializersOfRemovedFields(MethodNode methodNode) {
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
