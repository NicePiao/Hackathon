package com.baidu.hackathon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		initGoogleOcr();
	}

	private void initGoogleOcr() {
		copyTessdataToSdcard1();
		copyTessdataToSdcard2();
	}

	private void copyTessdataToSdcard1() {
		File outFile = new File(getExternalFilesDir("eng/tessdata")
				+ "/eng.traineddata");
		if (outFile.exists()) {
			return;
		}

		BufferedInputStream is = null;
		BufferedOutputStream os = null;
		try {
			is = new BufferedInputStream(getAssets().open("google-ocr-tessdata/eng/tessdata/eng.traineddata"));
			os = new BufferedOutputStream(new FileOutputStream(outFile));

			byte[] buffer = new byte[1024];
			while (is.read(buffer) != -1) {
				os.write(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				os.close();
			} catch (Exception e) {

			}
		}

	}
	
	
	private void copyTessdataToSdcard2() {
		File outFile = new File(getExternalFilesDir("chi/tessdata")
				+ "/eng.traineddata");
		if (outFile.exists()) {
			return;
		}

		BufferedInputStream is = null;
		BufferedOutputStream os = null;
		try {
			is = new BufferedInputStream(getAssets().open("google-ocr-tessdata/chi/tessdata/eng.traineddata"));
			os = new BufferedOutputStream(new FileOutputStream(outFile));

			byte[] buffer = new byte[1024];
			while (is.read(buffer) != -1) {
				os.write(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				os.close();
			} catch (Exception e) {

			}
		}

	}
}
