package com.pfichtner.github.maedle.transform;

import static com.pfichtner.github.maedle.transform.util.CollectionUtil.nonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ASM9;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import com.pfichtner.github.maedle.transform.util.AsmUtil;

public class MojoClassAnalyser extends ClassNode {

	public static class MojoData {

		private String classname;
		private Map<String, Object> mojoAnnotationValues = emptyMap();
		private List<FieldNode> mojoParameterFields = emptyList();
		
		public String getClassname() {
			return classname;
		}

		public boolean isMojo() {
			return !mojoAnnotationValues.isEmpty();
		}

		public Map<String, Object> getMojoAnnotationValues() {
			return mojoAnnotationValues;
		}

		public List<FieldNode> getMojoParameterFields() {
			return mojoParameterFields;
		}

	}

	private final MojoData mojoData = new MojoData();

	public MojoClassAnalyser() {
		super(ASM9);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		mojoData.classname = name;
		mojoData.mojoAnnotationValues = nonNull(this.invisibleAnnotations).stream()
				.filter(n -> Constants.MOJO_ANNOTATION.equals(Type.getType(n.desc))).findFirst().map(AsmUtil::toMap)
				.orElse(emptyMap());
		mojoData.mojoParameterFields = fields.stream().filter(f -> nonNull(f.invisibleAnnotations).stream()
				.map(a -> Type.getType(a.desc)).anyMatch(Constants.mavenParameterAnnotation::equals)).collect(toList());
	}

	public static MojoData mojoData(ClassReader classReader) {
		MojoClassAnalyser detector = new MojoClassAnalyser();
		classReader.accept(detector, 0);
		return detector.mojoData;
	}

}