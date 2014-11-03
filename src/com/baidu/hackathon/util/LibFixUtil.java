package com.baidu.hackathon.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class LibFixUtil {

	//private static final String LIB_PATH = "data/data/com.baidu.hackathon/lib/";
	private static final String LIB_PATH2 = "/data/app-lib/com.baidu.hackathon-1/";
	private static final String LIB_PATH3 = "/data/app-lib/com.baidu.hackathon-2/";

	public static void fixLib(Context context) {

		List<String> libPathList = new ArrayList<String>();
		//libPathList.add(LIB_PATH);
		libPathList.add(LIB_PATH2);
		libPathList.add(LIB_PATH3);
		for (String libPath : libPathList) {
			File libPathDir = new File(libPath);
			if (libPathDir.exists()) {
				try {
					String[] bakLibs = context.getAssets().list("lib");
					for (String libName : bakLibs) {
						File targetFile = new File(libPath + libName);
						if (!targetFile.exists()) {
							copyFile(context, "lib/" + libName, targetFile);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}

	// 复制文件
	public static void copyFile(Context context, String assertFilePath,
			File targetFile) throws IOException {
		BufferedInputStream inBuff = null;
		BufferedOutputStream outBuff = null;
		try {
			// 新建文件输入流并对它进行缓冲
			inBuff = new BufferedInputStream(context.getAssets().open(
					assertFilePath));

			// 新建文件输出流并对它进行缓冲
			outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

			// 缓冲数组
			byte[] b = new byte[1024 * 5];
			int len;
			while ((len = inBuff.read(b)) != -1) {
				outBuff.write(b, 0, len);
			}
			// 刷新此缓冲的输出流
			outBuff.flush();
		} finally {
			// 关闭流
			if (inBuff != null)
				inBuff.close();
			if (outBuff != null)
				outBuff.close();
		}
	}

}
