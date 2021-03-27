package com.pfichtner.github.maedle;

public class TransformedMojo {

	private TransformedExtension extension;

	public TransformedMojo(TransformedExtension extension) {
		this.extension = extension;
	}

	public void execute() {
		System.out.println("Hello, " + extension.greeter);
		System.out.println("I have a message for You: " + extension.message);
	}
}
