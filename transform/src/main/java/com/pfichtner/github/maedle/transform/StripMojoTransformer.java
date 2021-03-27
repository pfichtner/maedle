package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_EXECUTION_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.MAVEN_MOJO_FAILURE_EXCEPTION;
import static com.pfichtner.github.maedle.transform.Constants.isMavenException;
import static com.pfichtner.github.maedle.transform.util.AsmUtil.withInstructions;
import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.tree.AbstractInsnNode.FIELD_INSN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

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

	private final ClassVisitor classVisitor;
	private final String extensionClassName;

	private final List<String> filteredFields = new ArrayList<>();
	private boolean classHasMojoAnno;
	private Map<String, Object> mojoAnnotationValues = emptyMap();

	public StripMojoTransformer(ClassVisitor classVisitor, String extensionClassName) {
		super(ASM9);
		this.classVisitor = classVisitor;
		this.extensionClassName = extensionClassName;
	}

	public Map<String, Object> getMojoAnnotationValues() {
		return mojoAnnotationValues;
	}

	public boolean isMojo() {
		return classHasMojoAnno;
	}

	@Override
	public void accept(ClassVisitor classVisitor) {
		if (classHasMojoAnno) {
			mojoAnnotationValues = this.invisibleAnnotations.stream()
					.filter(n -> Constants.MOJO_ANNOTATION.equals(Type.getType(n.desc))).findFirst()
					.map(n -> AsmUtil.toMap(n)).orElse(emptyMap());
			superName = "java/lang/Object";
			List<FieldNode> fieldsToRemove = fields.stream().filter(f -> nonNull(f.invisibleAnnotations).stream()
					.map(a -> Type.getType(a.desc)).noneMatch(Constants.mavenParameterAnnotation::equals))
					.collect(toList());
			fields.removeAll(fieldsToRemove);
			fields.stream().filter(f -> !fieldsToRemove.contains(f)).map(f -> f.name).forEach(filteredFields::add);
			fields.add(new FieldNode(ACC_PRIVATE, "extension", "L" + extensionClassName + ";", null, null));

			methods.removeIf(AsmUtil::isNoArgConstructor);
			methods.add(extensionClassConstructor());

			// TODO remap
			// mv.visitMethodInsn(INVOKEVIRTUAL,
			// "com/github/pfichtner/heapwatch/mavenplugin/HeapWatchMojo", "getLog",
			// "()Lorg/apache/maven/plugin/logging/Log;", false);
			// val slf4jLogger = LoggerFactory.getLogger("some-logger")
			// slf4jLogger.info("An info log message logged using SLF4j")

			methods.forEach(this::changeFieldOwner);
			methods.forEach(m -> m.exceptions = filterMavenExceptions(m.exceptions));
		}
		super.accept(mapMavenExceptions(classVisitor));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		classHasMojoAnno = Constants.MOJO_ANNOTATION.equals(Type.getType(descriptor));
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visitEnd() {
		accept(classVisitor);
	}

	private void changeFieldOwner(MethodNode m) {
		m.instructions.forEach(n -> {
			if (n.getType() == FIELD_INSN) {
				FieldInsnNode fin = (FieldInsnNode) n;
				if (StripMojoTransformer.this.name.equals(fin.owner) && filteredFields.contains(fin.name)) {
					m.instructions.insertBefore(n,
							new FieldInsnNode(n.getOpcode(), fin.owner, "extension", "L" + extensionClassName + ";"));
					fin.owner = extensionClassName;
				}
			}
		});
	}

	private ClassRemapper mapMavenExceptions(ClassVisitor classVisitor) {
		return new ClassRemapper(classVisitor, new Remapper() {
			@Override
			public String map(String internalName) {
				// TODO is there a gradle exception type we can map to?
				if (MAVEN_MOJO_FAILURE_EXCEPTION.equals(internalName)) {
					return "java/lang/RuntimeException";
				} else if (MAVEN_MOJO_EXECUTION_EXCEPTION.equals(internalName)) {
					return "java/lang/RuntimeException";
				} else {
					return internalName;
				}
			}
		});
	}

	private List<String> filterMavenExceptions(List<String> exceptions) {
		return exceptions.stream().filter(t -> !isMavenException(t)).collect(toList());
	}

	private MethodNode extensionClassConstructor() {
		return withInstructions(new MethodNode(ACC_PUBLIC, "<init>", "(L" + extensionClassName + ";)V", null, null),
				new VarInsnNode(ALOAD, 0), //
				new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false), //
				new VarInsnNode(ALOAD, 0), //
				new VarInsnNode(ALOAD, 1), //
				new FieldInsnNode(PUTFIELD, StripMojoTransformer.this.name, "extension",
						"L" + extensionClassName + ";"), //
				new InsnNode(RETURN) //
		);
	}

	public List<String> getFilteredFields() {
		return filteredFields;
	}

}