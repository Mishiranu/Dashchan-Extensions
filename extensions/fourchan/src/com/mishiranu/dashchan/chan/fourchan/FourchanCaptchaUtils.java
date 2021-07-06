package com.mishiranu.dashchan.chan.fourchan;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Arrays;

public class FourchanCaptchaUtils {
	public static Integer findCenterOffset(Bitmap image) {
		// Find offset of central non-empty space to the center of the image
		// Array structure: [non-empty, empty, non-empty, empty, ..., non-empty]
		ArrayList<Integer> ranges = new ArrayList<>();
		boolean empty = false;
		int rangeLength = 0;
		int[] line = new int[image.getHeight()];
		for (int i = 0; i < image.getWidth(); i++) {
			image.getPixels(line, 0, 1, i, 0, 1, line.length);
			int emptyCount = 0;
			for (int c : line) {
				if (c >> 24 == 0) {
					emptyCount++;
				}
			}
			boolean itEmpty = emptyCount >= 10;
			if (itEmpty == empty) {
				rangeLength++;
			} else {
				empty = itEmpty;
				ranges.add(rangeLength);
				rangeLength = 1;
			}
		}
		ranges.add(rangeLength);
		if (empty) {
			ranges.add(0);
		}
		// Length should always be an odd number
		if (ranges.size() % 2 != 1) {
			throw new IllegalStateException();
		}
		if (ranges.size() == 1) {
			return null;
		} else {
			int centerIndex = ranges.size() / 2;
			int cx = (ranges.get(centerIndex - 1) + ranges.get(centerIndex) + ranges.get(centerIndex + 1)) / 2;
			for (int i = 0; i < centerIndex - 1; i++) {
				cx += ranges.get(i);
			}
			return image.getWidth() / 2 - cx;
		}
	}

	public interface BinarySearchCallback<T extends Throwable> {
		Integer getIndex(Bitmap[] images) throws T;
	}

	public static <T extends Throwable> Integer binarySearchOffset(Bitmap image, Bitmap background,
			int maxCount, int baseOffset, BinarySearchCallback<T> callback) throws T {
		int min = 0;
		int max = background.getWidth() - image.getWidth();
		int minStep = 3;
		Bitmap[] bitmaps = new Bitmap[maxCount];
		Canvas[] canvases = new Canvas[maxCount];
		try {
			while (max - min >= 2 * minStep) {
				int count = Math.min(maxCount, (max - min + minStep - 1) / minStep);
				for (int i = 0; i < count; i++) {
					if (bitmaps[i] == null) {
						bitmaps[i] = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
						canvases[i] = new Canvas(bitmaps[i]);
					}
					int dx = min + (max - min) * i / (count - 1);
					canvases[i].drawBitmap(background, baseOffset - dx, 0, null);
					canvases[i].drawBitmap(image, baseOffset, 0, null);
				}
				// TODO Display only "count" images after releasing a bug fix in ForegroundManager
				for (int i = count; i < maxCount; i++) {
					if (bitmaps[i] == null) {
						bitmaps[i] = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
						canvases[i] = new Canvas(bitmaps[i]);
					}
					bitmaps[i].eraseColor(0x00000000);
				}
				Integer result = callback.getIndex(Arrays.copyOf(bitmaps, maxCount));
				if (result == null) {
					return null;
				} else if (result < 0 || result >= count) {
					break;
				} else if (result == 0) {
					max = min + (max - min) / (count - 1) - 1;
				} else if (result == count - 1) {
					min = min + (max - min) * (count - 2) / (count - 1) + 1;
				} else {
					int newMin = min + (max - min) * (result - 1) / (count - 1) + 1;
					int newMax = min + (max - min) * (result + 1) / (count - 1) - 1;
					min = newMin;
					max = newMax;
				}
			}
			return -(min + max) / 2;
		} finally {
			for (Bitmap bitmap : bitmaps) {
				if (bitmap != null) {
					bitmap.recycle();
				}
			}
		}
	}

	// Transforms white into transparent
	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -1f, -1f, -1f, 1f, 0f});

	public static Bitmap create(Bitmap image, Bitmap background, int offset) {
		Bitmap tmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas tmpCanvas = new Canvas(tmp);
		if (background != null) {
			tmpCanvas.drawBitmap(background, offset, 0, null);
		}
		tmpCanvas.drawBitmap(image, 0, 0, null);
		Bitmap result = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
		Paint paint = new Paint();
		paint.setColorFilter(CAPTCHA_FILTER);
		new Canvas(result).drawBitmap(tmp, 0, 0, paint);
		tmp.recycle();
		return result;
	}
}
