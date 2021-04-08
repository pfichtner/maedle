package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_FAILURE_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MOJO_ANNOTATION;
import static com.pfichtner.github.maedle.transform.Constants.isMavenException;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.JAVA_LANG_OBJECT;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.findNode;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.isType;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.isAload;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.objectTypeToInternal;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

import java.util.List;

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
import com.pfichtner.github.maedle.transform.util.AsmUtil;

/**
 * ClassVisitor that changes the class as follows
 * <li>
 * <li>Change super class to Object and let class implement
 * Plugin&lt;Project&gt;</li>
 * <li>filter out all class members annotated with @Parameter</li>
 * <li>rewrite all accesses to class members to extension class</li> // TODO
 * <li>removes thrown exception declarations from Mojo#execute method</li> //
 */
public class StripMojoTransformer extends ClassNode {

	private static final String FIELD_NAME_FOR_EXTENSION_INSTANCE = "extension";

	private static final Remapper defaultRemapper = new Remapper() {
		@Override
		public String map(String internalName) {
			if (MAVEN_MOJO_FAILURE_EXCEPTION.equals(Type.getObjectType(internalName))) {
				return "org/gradle/api/tasks/TaskExecutionException";
			} else if (MAVEN_MOJO_EXECUTION_EXCEPTION.equals(Type.getObjectType(internalName))) {
				return "org/gradle/api/tasks/TaskExecutionException";
			} else {
				return internalName;
			}
		}
	};

	private final ClassVisitor classVisitor;
	private final Type mojoClass;
	private final Type extensionClass;

	private final MojoData mojoData;

	private Remapper remapper = defaultRemapper;

	public StripMojoTransformer(ClassVisitor classVisitor, Type mojoClass, Type extensionClass, MojoData mojoData) {
		super(ASM9);
		this.classVisitor = classVisitor;
		this.mojoClass = mojoClass;
		this.extensionClass = extensionClass;
		this.mojoData = mojoData;
	}

	public StripMojoTransformer withRemapper(Remapper remapper) {
		this.remapper = remapper == null ? defaultRemapper : remapper;
		return this;
	}

	@Override
	public void accept(ClassVisitor classVisitor) {
		nonNull(invisibleAnnotations).removeIf(isType(MOJO_ANNOTATION));
		nonNull(visibleAnnotations).removeIf(isType(MOJO_ANNOTATION));
		superName = Type.getType(Object.class).getInternalName();
		fields.removeAll(mojoData.getMojoParameterFields());
		fields.add(new FieldNode(ACC_PRIVATE, FIELD_NAME_FOR_EXTENSION_INSTANCE,
				objectTypeToInternal(extensionClass).getDescriptor(), null, null));

		methods.removeIf(AsmUtil::isNoArgConstructor);
		methods.add(extensionClassConstructor());

		methods.forEach(this::mapMavenToGradleLogging);
		methods.forEach(this::replaceFieldAccess);
		methods.forEach(m -> m.exceptions = removeMavenExceptions(m.exceptions));
		super.accept(remap(classVisitor));
	}

	@Override
	public void visitEnd() {
		accept(classVisitor);
	}

	private void replaceFieldAccess(MethodNode methodNode) {
		methodNode.instructions.forEach(n -> {
			if (n.getType() == FIELD_INSN) {
				FieldInsnNode fin = (FieldInsnNode) n;
				if (hasMovedToExtensionClass(fin)) {
					AbstractInsnNode aload0 = findNode(n, isAload(0), AbstractInsnNode::getPrevious);
					methodNode.instructions.insert(aload0, new FieldInsnNode(GETFIELD, fin.owner,
							FIELD_NAME_FOR_EXTENSION_INSTANCE, objectTypeToInternal(extensionClass).getDescriptor()));
					fin.owner = extensionClass.getInternalName();
				}
			}
		});
	}

	private void mapMavenToGradleLogging(MethodNode methodNode) {
		methodNode.instructions.forEach(n -> {
			if (n.getType() == METHOD_INSN) {
				MethodInsnNode methodInsn = (MethodInsnNode) n;
				if (isGetLogAccess(methodInsn)) {
					methodInsn.owner = "java/lang/Object";
					methodInsn.name = "getClass";
					methodInsn.desc = "()Ljava/lang/Class;";
					methodNode.instructions.insert(methodInsn,
							new MethodInsnNode(INVOKESTATIC, "org/gradle/api/logging/Logging", "getLogger",
									"(Ljava/lang/Class;)Lorg/gradle/api/logging/Logger;"));
				} else if (isLogCall(methodInsn)) {
					// maven accepts CharSequences, gradle Strings, this SHOULD normaly not be a
					// problem: Better solution would be to transform CharSequences to Strings (if
					// not already instance of). Even maven is calling #toString on the passed
					// CharSequences, which does not really make sense since so the method could
					// also accept any type (java.lang.Object).
					methodInsn.owner = "org/gradle/api/logging/Logger";
					methodInsn.desc = "(Ljava/lang/String;)V";
				}
			}
		});
	}

	private boolean isGetLogAccess(MethodInsnNode node) {
		return node.getOpcode() == INVOKEVIRTUAL && node.owner.equals(this.name) && node.name.equals("getLog");
	}

	private boolean isLogCall(MethodInsnNode node) {
		return node.getOpcode() == INVOKEINTERFACE && node.owner.equals("org/apache/maven/plugin/logging/Log");
	}

	private boolean hasMovedToExtensionClass(FieldInsnNode node) {
		return this.name.equals(node.owner)
				&& mojoData.getMojoParameterFields().stream().map(f -> f.name).anyMatch(node.name::equals);
	}

	private ClassRemapper remap(ClassVisitor classVisitor) {
		return new ClassRemapper(new ClassRemapper(classVisitor, new Remapper() {
			@Override
			public String map(String internalName) {
				return Type.getObjectType(internalName).equals(mojoData.getMojoType()) //
						? mojoClass.getInternalName() //
						: internalName;
			}
		}), remapper);
	}

	private static List<String> removeMavenExceptions(List<String> exceptions) {
		return exceptions.stream().filter(t -> !isMavenException(Type.getObjectType(t))).collect(toList());
	}

	private MethodNode extensionClassConstructor() {
		// TODO (xxx) is an array, use Type#??? to create
		MethodNode mv = new MethodNode(ACC_PUBLIC, "<init>",
				"(" + objectTypeToInternal(extensionClass).getDescriptor() + ")V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, "<init>", "()V", false);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(PUTFIELD, this.name, FIELD_NAME_FOR_EXTENSION_INSTANCE,
				objectTypeToInternal(extensionClass).getDescriptor());
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
		return mv;
	}

}