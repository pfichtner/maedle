# maedle

A maven plugin that transforms a maven plugin into a gradle plugin (plugin will be available soon as gradle plugin). ;-)
The word "Maedle" is a contamination of maven and gradle and sounds very similar to the german word for a young girl. 

What is maedle capable of? 
Inside a JAR it searches for classes annotated with maven's ```Mojo``` annotation. Those classes are transformed in the followin way: 
- An extension class is created
- All attributes of the mojo class annotated with maven's ```Parameter``` annotation are moved to the extension class created
- All read/write accesses to the mojo's attributes are redirected to the extension class
- The Mojo class' superclass is replaced by java.lang.Object
- The Mojo's class gets a new constructor where an instance of the exentsion class can be passed. The exentsion instance passed is stored as a class attribute
- A gradle plugin class is created. This class creates an instance of the extension class and an instance of the transformed mojo and pass the extension instance to it. After that the execute mojo's method is called. 
- Exceptions org.apache.maven.plugin.MojoFailureException org.apache.maven.plugin.MojoExecutionException are replaced by org.gradle.api.tasks.TaskExecutionException
- Calls to maven's Logger will be replaced by call's to a Gradle Logger
- META-INF entry is created which holds the information for gradle to be able to use the plugin
