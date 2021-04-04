package transform.me;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = GreeterMojo.GOAL)
public class GreeterMojo extends AbstractMojo {

	public static final String GOAL = "greet";

	@Parameter(name = "greeter")
	public String greeter = "pfichtner";

	@Parameter(name = "message")
	public String message = "Message from Mojo!";

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (greeter == null) {
			throw new MojoFailureException("greeter must not be null");
		}
		if (message == null) {
			throw new MojoFailureException("message must not be null");
		}
		System.out.println("Hello, " + greeter);
		System.out.println("I have a message for you: " + message);
	}

}
