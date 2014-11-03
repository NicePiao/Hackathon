package com.baidu.hackathon.ocr;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.google.gson.Gson;

public class OcrHttpTask {

	public static final String URL = "http://10.46.109.53:8181/vis-api.fcgi?";

	public OcrData ocrNet(String base64ImgStr) {
		HttpPost httpPost = new HttpPost(URL);
		// List<NameValuePair> param = new ArrayList<NameValuePair>();
		// param.add(new BasicNameValuePair("from", "android"));
		// param.add(new BasicNameValuePair("appid", "00000"));
		// param.add(new BasicNameValuePair("clientip", "10.10.10.10"));
		// param.add(new BasicNameValuePair("encoding", String.valueOf(1)));
		// param.add(new BasicNameValuePair("version", "1.0.0"));
		// param.add(new BasicNameValuePair("type", "st_ocrapi"));
		// param.add(new BasicNameValuePair("image", base64ImgStr));
		// param.add(new BasicNameValuePair("direction", String.valueOf(0)));
		// param.add(new BasicNameValuePair("detecttype", "LocateRecognize"));
		// // param.add(new BasicNameValuePair("mask", "xxx"));
		// param.add(new BasicNameValuePair("languagetype", "CHN_ENG"));
		// // param.add(new BasicNameValuePair("queryid", "xxx"));

		String paramEntity = "type=st_ocrapi&encoding=1&appid=10000&version=1.0.0&from=android&clientip=10.10.10.10&detecttype=LocateRecognize&image="
				+ base64ImgStr;
		try {
			httpPost.setEntity(new StringEntity(paramEntity));
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpPost);
			if (response != null
					&& response.getStatusLine().getStatusCode() == 200) {
				String content = EntityUtils.toString(response.getEntity(),
						"utf-8");
				Log.d("qcw", "content:" + content);
				return parseString(content);
			}
			Log.d("qcw", "result null");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Log.d("qcw", e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("qcw", e.toString());
		}

		return null;
	}

	private OcrData parseString(String responseStr) {
		Gson gson = new Gson();
		return gson.fromJson(responseStr, OcrData.class);
	}

}
