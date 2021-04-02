package com.pfichtner.github.maedle;

/**
 * This class has no need expect to compile the mixin class
 * {@link MaedlePluginTemplate}.
 */
public class M {

	private final E e;

	public M(E e) {
		this.e = e;
	}

	public void execute() {
		System.out.println(e);
	}

}
