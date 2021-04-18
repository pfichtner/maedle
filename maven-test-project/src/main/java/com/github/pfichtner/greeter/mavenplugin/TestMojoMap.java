package com.github.pfichtner.greeter.mavenplugin;

import static java.util.Comparator.comparing;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = TestMojoMap.GOAL)
public class TestMojoMap extends AbstractMojo {

	public static final String GOAL = "testMojoMap";

	// TODO howto to configure nested object in gradle build file?
//	public static class MyObject {
//		public int number;
//		public String text;
//	}

	@Parameter(name = "data")
	public Map<String, Integer> data;

//	@Parameter(name = "data2")
//	public Map<String, MyObject> data2;

	public void execute() throws MojoExecutionException, MojoFailureException {
		data.entrySet().stream().sorted(comparing(Entry<String, Integer>::getValue))
				.forEach(e -> System.out.println(e.getKey() + " * 42 = " + e.getValue() * 42));
	}

}
