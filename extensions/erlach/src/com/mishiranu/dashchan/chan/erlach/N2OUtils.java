package com.mishiranu.dashchan.chan.erlach;

import android.util.Pair;
import chan.http.WebSocket;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class N2OUtils {
	private static int readInt(byte[] bytes, int start, int count) {
		int result = 0;
		for (int i = 0; i < count; i++) {
			result = result << 8 | bytes[start + i] & 0xff;
		}
		return result;
	}

	public static Pair<ArrayList<String>, ArrayList<Integer>> parse(byte[] bytes) {
		// Simple BERT format decoding for strings
		ArrayList<String> strings = new ArrayList<>();
		ArrayList<Integer> ints = new ArrayList<>();
		for (int i = 0; i < bytes.length; i++) {
			int indexValue = bytes[i] & 0xff;
			switch (indexValue) {
				case 97:
				case 104: {
					ints.add(readInt(bytes, i + 1, 1));
					i++;
					break;
				}
				case 98:
				case 108: {
					ints.add(readInt(bytes, i + 1, 4));
					i += 4;
					break;
				}
				case 110:
				case 111: {
					int value;
					if (indexValue == 110) {
						value = readInt(bytes, i + 1, 1);
						i++;
					} else {
						value = readInt(bytes, i + 1, 4);
						i += 4;
					}
					ints.add(value);
					i += value;
					break;
				}
				case 100:
				case 107:
				case 109: {
					int size;
					if (indexValue == 109) {
						size = (bytes[i + 1] & 0xff) << 24 | (bytes[i + 2] & 0xff) << 16
								| (bytes[i + 3] & 0xff) << 8 | (bytes[i + 4] & 0xff);
						i += 4;
					} else {
						size = (bytes[i + 1] & 0xff) << 8 | (bytes[i + 2] & 0xff);
						i += 2;
					}
					strings.add(new String(bytes, i + 1, size));
					i += size;
					break;
				}
			}
		}
		return new Pair<>(strings, ints);
	}

	private static WebSocket.ComplexBinaryBuilder fromIntLengthOut(WebSocket.ComplexBinaryBuilder builder,
			int type, int value, int[] lengthOut) {
		try {
			if (type == 0x62 || type == 0x6c || type == 0x6d || type == 0x6f) {
				return builder.bytes(type, (value >> 24) & 0xff, (value >> 16) & 0xff,
						(value >> 8) & 0xff, value & 0xff);
			} else if (type == 0x64 || type == 0x6b) {
				if (value > 0xffff || value < 0) {
					value = 0xffff;
				}
				return builder.bytes(type, (value >> 8) & 0xff, value & 0xff);
			} else if (type == 0x61 || type == 0x68 || type == 0x6e) {
				if (value > 0xff || value < 0) {
					value = 0xff;
				}
				return builder.bytes(type, value & 0xff);
			} else {
				return builder;
			}
		} finally {
			if (lengthOut != null) {
				lengthOut[0] = value;
			}
		}
	}

	public static WebSocket.ComplexBinaryBuilder.Wrapper fromInt(int type, int value) {
		return builder -> fromIntLengthOut(builder, type, value, null);
	}

	public static WebSocket.ComplexBinaryBuilder.Wrapper fromString(int type, String binData) {
		return builder -> {
			int[] actualLength = {0};
			byte[] bytes = binData.getBytes();
			builder = fromIntLengthOut(builder, type, bytes.length, actualLength);
			if (actualLength[0] < bytes.length) {
				return builder.stream(new ByteArrayInputStream(bytes), actualLength[0]);
			} else {
				return builder.bytes(bytes);
			}
		};
	}

	public static void writeBytes(java.io.ByteArrayOutputStream stream, int... bytes) {
		for (int b : bytes) {
			stream.write(b);
		}
	}

	public static int writeInt(java.io.ByteArrayOutputStream stream, int type, int value) {
		writeBytes(stream, type);
		if (type == 0x62 || type == 0x6c || type == 0x6d || type == 0x6f) {
			writeBytes(stream, (value >> 24) & 0xff, (value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff);
		} else if (type == 0x64 || type == 0x6b) {
			if (value > 0xffff || value < 0) {
				value = 0xffff;
			}
			writeBytes(stream, (value >> 8) & 0xff, value & 0xff);
		} else if (type == 0x61 || type == 0x68 || type == 0x6e) {
			if (value > 0xff || value < 0) {
				value = 0xff;
			}
			writeBytes(stream, value & 0xff);
		} else {
			value = 0;
		}
		return value;
	}

	public static void writeString(java.io.ByteArrayOutputStream stream, int type, String binData) {
		byte[] bytes = binData.getBytes();
		int actualLength = writeInt(stream, type, bytes.length);
		stream.write(bytes, 0, Math.min(bytes.length, actualLength));
	}
}
