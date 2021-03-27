package com.github.pfichtner.greeter.mavenplugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = HeapWatchMojo.GOAL, defaultPhase = POST_INTEGRATION_TEST)
public class HeapWatchMojo extends AbstractMojo {

	public static final String GOAL = "verify";

	@Parameter(name = "gclog", required = true)
	public File gclog;

	@Parameter(name = "heapOccupancy")
	public Map<String, String> heapOccupancy;
	@Parameter(name = "heapAfterGC")
	public Map<String, String> heapAfterGC;
	@Parameter(name = "heapSpace")
	public Map<String, String> heapSpace;
	@Parameter(name = "metaspaceOccupancy")
	public Map<String, String> metaspaceOccupancy;
	@Parameter(name = "metaspaceAfterGC")
	public Map<String, String> metaspaceAfterGC;
	@Parameter(name = "metaspaceSpace")
	public Map<String, String> metaspaceSpace;

	@Parameter(name = "breakBuildOnValidationError")
	public boolean breakBuildOnValidationError = true;

	@Parameter(name = "greeter")
	public String greeter = "pfichtner";
	@Parameter(name = "message")
	public String message = "Message from Mojo!";
//	@Parameter(name = "messageNoDefault")
//    public String messageNoDefault;

	public void execute() throws MojoExecutionException {
//		if (this.gclog == null) {
//			throw new NullPointerException("gclog");
//		}
//		if (!this.gclog.exists()) {
//			throw new IllegalStateException(gclog + " does not exist");
//		}

		System.out.println("Hello, " + greeter);
		System.out.println("I have a message for You: " + message);
	}

}
