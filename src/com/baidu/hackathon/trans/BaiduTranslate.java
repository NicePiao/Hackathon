package com.baidu.hackathon.trans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

public class BaiduTranslate {
	private RequestObject ro;

	public BaiduTranslate(RequestObject ro) {
		this.ro = ro;
	}

	public ParseJsonBean getTranslateResult() {
		String uriAPI = ro.getUriAPI().toString();
		if (uriAPI == null) {
			return null;
		}
		// 建立HTTP Get对象
		HttpGet httpRequest = new HttpGet(uriAPI);
		HttpResponse httpResponse;
		try {
			httpResponse = new DefaultHttpClient().execute(httpRequest);

			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				 StringBuilder builder = new StringBuilder(); 
                 BufferedReader bufferedReader = new BufferedReader( 
                         new InputStreamReader(httpResponse.getEntity().getContent())); 
                 for (String s = bufferedReader.readLine(); s != null; s = bufferedReader
                         .readLine()) { 
                     builder.append(s); 
                 } 				
                 
                 Gson gson =new Gson();
                 ParseJsonBean pjb = gson.fromJson(builder.toString(), ParseJsonBean.class);
                 return pjb;
			} 
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}