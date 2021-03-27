package com.pfichtner.github.maedle.transform;

public class TaskExecutionException extends Exception {

	private static final long serialVersionUID = -445152653460551835L;

	public TaskExecutionException(String message) {
		super(message);
	}

	public TaskExecutionException(String message, Throwable t) {
		super(message, t);
	}

}
