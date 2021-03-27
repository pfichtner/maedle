package com.pfichtner.github.maedle.transform.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IoUtils {
	
	private IoUtils() {
		super();
	}

	public static byte[] toBytes(InputStream inputStream) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(inputStream);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		for (int result = bis.read(); result != -1; result = bis.read()) {
			buf.write((byte) result);
		}
		return buf.toByteArray();
	}

}
