package com.pfichtner.github.maedle.transform.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IoUtils {

	private IoUtils() {
		super();
	}

	public static byte[] toBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		copy(inputStream, buf);
		return buf.toByteArray();
	}

	public static void copy(InputStream inputStream, OutputStream buf) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(inputStream);
		for (int result = bis.read(); result != -1; result = bis.read()) {
			buf.write((byte) result);
		}
	}

}
