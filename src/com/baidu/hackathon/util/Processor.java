package com.baidu.hackathon.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.baidu.hackathon.baike.BaikeUtil;
import com.baidu.hackathon.ocr.ImgUtil;
import com.baidu.hackathon.ocr.OcrData;
import com.baidu.hackathon.ocr.OcrData.OcrRet;
import com.baidu.hackathon.ocr.OcrHttpTask;
import com.baidu.hackathon.trans.TranslateUtil;

/**
 * 主界面服务处理器
 * 
 * @author qinchaowei
 * 
 */
public class Processor {

	private Context mContext;

	public Processor(Context context) {
		mContext = context;
	}

	private List<String> getOcrKeyword(OcrData ocrData) {
		List<String> keyValues = new ArrayList<String>();
		if (ocrData != null) {
			List<OcrRet> rets = ocrData.getRet();
			if (rets != null) {
				for (OcrRet ocrRet : rets) {
					if (!TextUtils.isEmpty(ocrRet.getWord())) {
						keyValues.add(ocrRet.getWord());
					}
				}
			}
		}
		return keyValues;
	}

	/**
	 * 获取Ocr解析文字列表
	 * 
	 * @param imageUri
	 *            图片Uri
	 * @throws OcrServerException 
	 */
	public List<String> processOcr(Uri imageUri) throws OcrServerException {
		Bitmap bitmap = ImgUtil.getBitmap(mContext, imageUri);
		return processOcr(bitmap);
	}

	/**
	 * 获取Ocr解析文字列表
	 * 
	 * @param imageUri
	 *            图片Uri
	 * @throws OcrServerException 
	 */
	public List<String> processOcr(Bitmap bitmap) throws OcrServerException {
		String base64Img = ImgUtil.bitmapToBase64(bitmap);
		OcrData data = new OcrHttpTask().ocrNet(base64Img);
		int cnt = 0;
		while (data != null && data.getErrno() >= -29999
				&& data.getErrno() <= -20000) {
			data = new OcrHttpTask().ocrNet(base64Img);
			cnt++;
			if (cnt == 5) {
				throw new OcrServerException();
			}
		}
		return getOcrKeyword(data);
	}

	/**
	 * 获取文字列表的 中文和百科内容
	 */
	public LinkedHashMap<String, String> getChineseAndBaike(
			List<String> wordList) {
		LinkedHashMap<String, String> results = new LinkedHashMap<String, String>();
		if (wordList == null || wordList.size() == 0) {
			return results;
		}

		ExecutorService executor = Executors
				.newFixedThreadPool(wordList.size());
		List<Future<List<String>>> futureList = new ArrayList<Future<List<String>>>();

		for (final String word : wordList) {
			Future<List<String>> future = executor
					.submit(new Callable<List<String>>() {

						@Override
						public List<String> call() throws Exception {
							List<String> result = new ArrayList<String>();
							String chineseText = TranslateUtil.toChinese(word);
							String baikeContent = BaikeUtil
									.getBaikeContent(chineseText);
							result.add(chineseText);
							result.add(baikeContent);
							return result;
						}
					});
			futureList.add(future);
		}

		for (Future<List<String>> future : futureList) {
			try {
				List<String> result = future.get();
				if (result != null && result.size() > 0) {
					String chineseText = result.get(0);
					String baikeContent = result.get(1);
					results.put(chineseText, baikeContent);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		return results;
	}

	public static class OcrServerException extends Exception {
	
	}

}
