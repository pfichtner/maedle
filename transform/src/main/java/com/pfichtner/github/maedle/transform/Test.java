package com.pfichtner.github.maedle.transform;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.Opcodes;

public class Test {
	
	public static void main(String[] args) {
		int a = ACC_PUBLIC; // 1
		System.out.println(a);
		
		a = a | ACC_STATIC; // +8 -> 9
		System.out.println(a);
		
		a = a ^ ACC_STATIC; // -1 -> 8
		System.out.println(a);

		a = a ^ ACC_PUBLIC; // -1 -> 8
		System.out.println(a);
		
	}

}
