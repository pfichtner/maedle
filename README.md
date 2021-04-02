# maedle

A maven plugin that transforms a maven plugin into a gradle plugin (plugin will be available soon as gradle plugin). ;-)
The transformation is done in bytecode so it is independent of the programming langauge the mojo was written. So maedle should work for mojos written in Java, Groovy, Scala, Kotlin and any other language that compiles to JVM bytecode. 
The word "Maedle" is a contamination of maven and gradle and sounds very similar to the german word for a young girl. 

What is maedle capable of? 
Inside a JAR it searches for classes annotated with maven's ```@Mojo``` annotation. Those classes are transformed in the following way: 
- An extension class is created
- All attributes of the mojo class annotated with maven's ```@Parameter``` annotation are "moved" to the extension class created. All fields in the extension class will become public. 
- All read/write accesses to the mojo's attributes are redirected to the extension class
- The Mojo class' superclass is replaced by ```java.lang.Object```
- The Mojo class' ```@Mojo``` annotation is removed
- The Mojo's class gets a new constructor where an instance of the exentsion class can be passed. The exentsion instance passed is stored as class attribute
- A gradle plugin class is created. This class extends ```Plugin&lt;Project&gt;```. In the overriden ```apply``` method an instance of the extension class and an instance of the transformed mojo will be created and the extension is passed to the transformed mojo constructor. After that the mojo's ```execute``` method is called. 
- Exceptions ```org.apache.maven.plugin.MojoFailureException``` and ```org.apache.maven.plugin.MojoExecutionException``` are replaced by ```org.gradle.api.tasks.TaskExecutionException```
- Calls to maven's Logger will be replaced by call's to a Gradle Logger
- META-INF entry is created which holds the information for gradle to be able to use the plugin

An example. This maven Mojo...
```
@Mojo(name = "greet")
public class GreeterMojo extends AbstractMojo {

	@Parameter(name = "greeter")
	private String greeter = "maedle";

	@Parameter(name = "message")
	private String message = "Message from Mojo!";

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
```
...gets transformed to the extension class...
```
public class GreeterMojoExtension {
	public String greeter = "maedle";
	public String message = "Message from Mojo!";
}
```
...and thr rewritten mojo class...
```
public class GreeterMojoRewritten {

	private final GreeterMojoExtension extension;

	public GreeterMojoRewritten(GreeterMojoExtension extension) {
		this.extension = extension;
	}

	public void execute() {
		if (extension.greeter == null) {
			throw new TaskExecutionException("greeter must not be null");
		}
		if (extension.message == null) {
			throw new TaskExecutionException("message must not be null");
		}
		System.out.println("Hello, " + extension.greeter);
		System.out.println("I have a message for you: " + extension.message);
	}

}
```
...and a plugin class gets created
```
public class GreetingPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		GreetingPluginExtension extension = project.getExtensions().create("greeting", GreetingPluginExtension.class);
		Task task = project.task("greet");
		task.doLast(t -> execute(extension));
	}

}
```


Not yet implemented
- Only working if Mojo extends ```AbstractMojo``` (getLog)
- transform MavenProject, access jar file
```
Jar jarTask = (Jar) project.task("jar");
File jarFile = jarTask.getArchiveFile().getOrNull().getAsFile();
```
- support for ```dependsOn```
```
@Override
public void apply(Project project) {
	GreetingPluginExtension extension = project.getExtensions().create(EXTENSION, GreetingPluginExtension.class);
	Task task = project.task(TASK);
	task.dependsOn("jar").doLast(t -> execute(extension));
}

```
