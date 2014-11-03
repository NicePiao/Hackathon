package com.baidu.hackathon.trans;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class RequestObject {

	static final String url = "http://openapi.baidu.com/public/2.0/bmt/translate?client_id=";
	static final String apiKey = "NtY6FIjAXEkGASXfkoxLlx5C";
	static final String from = "auto";
	static final String to = "auto";

	private StringBuilder uriAPI = new StringBuilder();

	public StringBuilder getUriAPI() {
		return uriAPI;
	}

	public void setUriAPI(StringBuilder uriAPI) {
		this.uriAPI = uriAPI;
	}

	public RequestObject(String word) {
		try {
			uriAPI = uriAPI.append(url).append(apiKey).append("&q=")
					.append(URLEncoder.encode(word, "utf-8")).append("&from=")
					.append(from).append("&to=").append(to);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}
}
