package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.isMojoAnnotation;
import static com.pfichtner.github.maedle.transform.Constants.isParameterAnnotation;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.JAVA_LANG_OBJECT;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.isConstructor;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.makePublic;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.mapType;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
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
		superName = JAVA_LANG_OBJECT;
		nonNull(invisibleAnnotations).removeIf(isMojoAnnotation);
		nonNull(visibleAnnotations).removeIf(isMojoAnnotation);
		nonNull(fields).removeIf(f -> !isMojoAttribute(f));
		nonNull(fields).stream().filter(this::isMojoAttribute).forEach(f -> {
			f.access = makePublic(f.access);
			nonNull(f.visibleAnnotations).removeIf(isParameterAnnotation);
			nonNull(f.invisibleAnnotations).removeIf(isParameterAnnotation);
		});
		nonNull(methods).removeIf(m -> !isConstructor(m));
		nonNull(methods).stream().filter(m -> isConstructor(m)).forEach(this::fixConstructor);
		accept(classVisitor);
	}

	private boolean isMojoAttribute(FieldNode fieldNode) {
		return isMojoAttribute(fieldNode.name);
	}

	private boolean isMojoAttribute(String fieldName) {
		return mojoData.getMojoParameterFields().stream().map(n -> n.name).anyMatch(fieldName::equals);
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

	public MojoToExtensionTransformer(ClassVisitor classVisitor, Type newType, MojoData mojoData) {
		super(ASM9);
		this.classVisitor = new ClassRemapper(classVisitor, new Remapper() {
			@Override
			public String map(String internalName) {
				return mapType(Type.getObjectType(MojoToExtensionTransformer.this.name), newType, internalName);
			}

		});
		this.mojoData = mojoData;
	}

}
