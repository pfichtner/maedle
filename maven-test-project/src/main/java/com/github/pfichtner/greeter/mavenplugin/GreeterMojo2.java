package com.github.pfichtner.greeter.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = GreeterMojo2.GOAL)
public class GreeterMojo2 extends AbstractMojo {

	public static final String GOAL = "greet2";

	@Parameter(name = "greeter")
	public String greeter = "pfichtner";

	@Parameter(name = "message")
	private String message = "Message from Mojo2!";

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (greeter == null) {
			throw new MojoFailureException("greeter must not be null");
		}
		if (message == null) {
			throw new MojoFailureException("message must not be null");
		}
		System.out.println("Hello, " + greeter);
		System.out.println("I have a message for You: " + message);
	}

}
