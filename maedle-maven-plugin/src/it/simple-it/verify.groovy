def jarFile = new File(basedir, 'target/simple-it-1.0-SNAPSHOT.jar')
assert jarFile.exists()

Set expected = [ 'META-INF/MANIFEST.MF', 'transform/me/GreeterMojoRewritten.class', 'transform/me/GreeterMojo.class', 'transform/me/GreeterMojoGradlePluginExtension.class', 'transform/me/GreeterMojoGradlePlugin.class', 'META-INF/gradle-plugins/transform.me.GreeterMojo.properties', 'META-INF/maven/sample.plugin.it/simple-it/pom.xml', 'META-INF/maven/sample.plugin.it/simple-it/pom.properties' ]
Set names = new java.util.zip.ZipFile(jarFile).entries().findAll { !it.directory }.collect { it.name }

assert expected.sort().equals(names.sort())


