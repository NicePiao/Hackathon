package com.baidu.hackathon.trans;

import android.text.TextUtils;

/**
 * 翻译静态工具类
 * 
 * @author qinchaowei
 * 
 */
public class TranslateUtil {
	/**
	 * 是否是英文
	 */
	public static boolean isEnglish(String word) {
		return String.valueOf(word.charAt(0)).getBytes().length == 1;
	}

	/**
	 * 翻译成汉语
	 */
	public static String toChinese(String word) {
		if (isEnglish(word)) {
			ParseJsonBean result = new BaiduTranslate(new RequestObject(word))
					.getTranslateResult();
			if (result != null && result.getTrans_result() != null
					&& !result.getTrans_result().isEmpty()) {
				String value = result.getTrans_result().get(0).getDst();
				if (!TextUtils.isEmpty(value)) {
					return value;
				}
			}
		}

		return word;
	}

}
