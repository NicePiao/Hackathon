package com.baidu.hackathon.ocr.google;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

public class GoogleOcrUtil {

	public static final String LANGUAGE_TYPE_ENG = "eng";
	public static final String LANGUAGE_TYPE_CHI_SIM = "chi";

	public static String ocrBitmap(Context context, String language,
			Bitmap bitmap) {
		try {
			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.init(context.getExternalFilesDir(language).getAbsolutePath()
					+ "/", "eng");
			baseApi.setImage(bitmap);
			String text = baseApi.getUTF8Text();
			baseApi.clear();
			baseApi.end();
			return text;
		} catch (Exception e) {
			Log.i("ocr", e.toString());
			return e.toString();
		}
	}

}
