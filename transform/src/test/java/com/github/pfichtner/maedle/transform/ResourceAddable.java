package com.github.pfichtner.maedle.transform;

import java.io.IOException;

public interface ResourceAddable {
	void add(byte[] content, String path) throws IOException;
}