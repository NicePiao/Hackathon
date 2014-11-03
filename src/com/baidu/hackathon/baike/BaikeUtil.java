package com.baidu.hackathon.baike;

import java.io.IOException;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.text.TextUtils;

public class BaikeUtil {

	private static Document queryBaike(String url) {
		int timeOut = 10 * 1000;
		String ua = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.137 Safari/537.36 LBBROWSER";
		try {
			Document doc = Jsoup.connect(url).timeout(timeOut).userAgent(ua)
					.get();
			return doc;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String parseCardContent(Document doc) {
		try {
			Element summaryEl = doc.select("div[class=card-summary-content]")
					.get(0);
			summaryEl.select("sup").remove();

			return summaryEl.text();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String parseMultiCard(Document doc) {
		try {
			Element ulEl = doc.select(
					"ul[class=custom_dot  para-list list-paddingleft-1]")
					.get(0);
			// ȡ��һ�������
			Element liEl = ulEl.select(
					"li[class=list-dot list-dot-paddingleft]").get(0);

			// ������ת����
			return "http://baike.baidu.com"
					+ liEl.select("a[href]").get(0).attr("href");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getBaikeContent(String keyword) {
		try {
			String url = String
					.format("http://baike.baidu.com/search/word?word=%s&pic=1&sug=1&enc=utf8",
							URLEncoder.encode(keyword, "utf-8"));
			Document doc = queryBaike(url);
			if (doc != null) {
				String summary = parseCardContent(doc);
				if (TextUtils.isEmpty(summary)) {
					String redirect = parseMultiCard(doc);
					doc = queryBaike(redirect);
					summary = parseCardContent(doc);
				}

				return summary;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

}
