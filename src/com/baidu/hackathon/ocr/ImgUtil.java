package com.baidu.hackathon.ocr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class ImgUtil {

	/**
	 * 获取文件base64编码
	 */
	public static String getBase64(Context context, Uri uri) {
		byte[] data = null;
		try {
			InputStream in = context.getContentResolver().openInputStream(uri);
			data = new byte[in.available()];
			in.read(data);
			in.close();
			String ourBase64 = Base64.encodeToString(data, Base64.NO_WRAP);
			return ourBase64;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 获取文件base64编码
	 */
	public static String getBase64(String filePath) {
		byte[] data = null;
		try {
			InputStream in = new FileInputStream(filePath);
			data = new byte[in.available()];
			in.read(data);
			in.close();
			return Base64.encodeToString(data, Base64.NO_WRAP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * bitmap2base64
	 */
	public static String bitmapToBase64(Bitmap bitmap) {

		String result = null;
		ByteArrayOutputStream baos = null;
		try {
			if (bitmap != null) {
				baos = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

				byte[] bitmapBytes = baos.toByteArray();
				result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
				Log.d("qcw", "bitmap size=" + result.length());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 获取压缩处理后的图片
	 */
	public static Bitmap getBitmap(Context context, Uri imgUri) {
		return compressImage(sizeBitmap(context, imgUri));
	}

	/**
	 * 压缩图片像素 �?��长�?宽像�?1000
	 */
	public static Bitmap sizeBitmap(Context context, Uri imgUri) {
		float maxSize = 1000f;
		try {
			BitmapFactory.Options newOpts = new BitmapFactory.Options();
			newOpts.inJustDecodeBounds = true;
			Bitmap bitmap = BitmapFactory.decodeStream(context
					.getContentResolver().openInputStream(imgUri), null,
					newOpts);

			newOpts.inJustDecodeBounds = false;
			int w = newOpts.outWidth;
			int h = newOpts.outHeight;
			float hh = maxSize;
			float ww = maxSize;
			int be = 1;
			if (w > h && w > ww) {
				be = (int) (newOpts.outWidth / ww);
			} else if (w < h && h > hh) {
				be = (int) (newOpts.outHeight / hh);
			}
			if (be <= 0)
				be = 1;
			newOpts.inSampleSize = be;
			bitmap = BitmapFactory.decodeStream(context.getContentResolver()
					.openInputStream(imgUri), null, newOpts);
			Log.i("qcw",
					String.format(
							"bitmap old(w,h)=(%s,%s),new(w,h)=(%s,%s)   bitmap size=%s",
							w, h, bitmap.getWidth(), bitmap.getHeight(),
							bitmap.getByteCount() / 1000));
			return bitmap;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 压缩图片质量 �?��size 1M
	 * 
	 */
	public static Bitmap compressImage(Bitmap image) {
		int maxSize = 1000;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		int options = 100;
		while (baos.toByteArray().length / 1024 > maxSize) {
			baos.reset();
			image.compress(Bitmap.CompressFormat.JPEG, options, baos);
			options -= 10;
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
		Log.i("qcw",
				"bitmap option=" + options + "  bitmap size="
						+ bitmap.getByteCount() / 1000);
		return bitmap;
	}

	// 图片允许最大空间 单位：KB
	public static Bitmap compressBitmap2TargetSize(Uri imageUri, float maxSize) {

		Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath());
		// 将字节换成KB
		int mid = bitmap.getByteCount() / 1024;
		// 判断bitmap占用空间是否大于允许最大空间 如果大于则压缩 小于则不压缩
		if (mid > maxSize) {
			// 获取bitmap大小 是允许最大大小的多少倍
			float i = mid / maxSize;
			// 开始压缩 此处用到平方根 将宽带和高度压缩掉对应的平方根倍
			// （1.保持刻度和高度和原bitmap比率一致，压缩后也达到了最大大小占用空间的大小）
			bitmap = zoomImage(bitmap, i);
		}
		return bitmap;
	}

	/***
	 * 图片的缩放方法
	 * 
	 * @param bgimage
	 *            ：源图片资源
	 * @param newWidth
	 *            ：缩放后宽度
	 * @param newHeight
	 *            ：缩放后高度
	 * @return
	 */
	public static Bitmap zoomImage(Bitmap bgimage, float ratio) {
		// 获取这个图片的宽和高
		float width = bgimage.getWidth();
		float height = bgimage.getHeight();
		double newWidth = width / Math.sqrt(ratio);
		double newHeight = height / Math.sqrt(ratio);
		// 创建操作图片用的matrix对象
		Matrix matrix = new Matrix();
		// 计算宽高缩放率
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// 缩放图片动作
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,
				(int) height, matrix, true);
		return bitmap;
	}
}
