package com.pfichtner.github.maedle;

public class M {

	private E extension;

	public M(E extension) {
		this.extension = extension;
	}

	public void execute() {
		System.out.println("Hello, " + extension.greeter);
		System.out.println("I have a message for You: " + extension.message);
	}
}
