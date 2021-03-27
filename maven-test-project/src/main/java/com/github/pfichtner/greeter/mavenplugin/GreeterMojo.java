package com.github.pfichtner.greeter.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = GreeterMojo.GOAL)
public class GreeterMojo extends AbstractMojo {

	public static final String GOAL = "greet";

	@Parameter(name = "greeter")
	public String greeter = "pfichtner";

	@Parameter(name = "message")
	public String message = "Message from Mojo!";

//	public void setMessage(String message) {
//		this.message = message;
//	}
//
//	public String getMessage() {
//		return message;
//	}

	public void execute() throws MojoExecutionException {
		System.out.println("Hello, " + greeter);
		System.out.println("I have a message for You: " + message);
	}

}
