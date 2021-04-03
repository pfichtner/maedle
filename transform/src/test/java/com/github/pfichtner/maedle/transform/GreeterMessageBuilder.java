package com.github.pfichtner.maedle.transform;

import java.util.HashMap;
import java.util.Map;

public class GreeterMessageBuilder {

	private final Map<Object, Object> data = new HashMap<>();

	public GreeterMessageBuilder withGreeter(String greeter) {
		data.put("greeter", greeter);
		return this;
	}

	public GreeterMessageBuilder withMessage(String message) {
		data.put("message", message);
		return this;
	}

	public Map<Object, Object> build() {
		return new HashMap<Object, Object>(data);
	}

}
