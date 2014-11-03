package com.baidu.hackathon.baike;

import java.io.IOException;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.hackathon.R;
public class BaikeTestActivity extends Activity implements OnClickListener {
	private EditText edit;
	private TextView resultTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_baike);
		findViewById(R.id.baike_test_work).setOnClickListener(this);
		resultTv = (TextView) findViewById(R.id.baike_test_result);
	}

	private Document queryBaike(String url) {
		int timeOut = 10 * 1000;
		String ua = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.137 Safari/537.36 LBBROWSER";
		try {
			Document doc = Jsoup.connect(url).timeout(timeOut).userAgent(ua)
					.get();
			return doc;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String parseCardContent(Document doc) {
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

	private String parseMultiCard(Document doc) {
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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.baike_test_work:
			if (!TextUtils.isEmpty(edit.getText().toString())) {
				new QueryAndParseBaikeTask().execute();
			}
			break;

		default:
			break;
		}
	}

	private class QueryAndParseBaikeTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			try {
				String url = String
						.format("http://baike.baidu.com/search/word?word=%s&pic=1&sug=1&enc=utf8",
								URLEncoder.encode(edit.getText().toString(),
										"utf-8"));
				Document doc = queryBaike(url);
				if (doc != null) {
					String summary = parseCardContent(doc);
					if (TextUtils.isEmpty(summary)) {
						// �����Ǹ������ҳ�� ���һ������ҳ�沢����
						String redirect = parseMultiCard(doc);
						doc = queryBaike(redirect);
						summary = parseCardContent(doc);
					}

					return summary;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			if (TextUtils.isEmpty(result)) {
				Toast.makeText(BaikeTestActivity.this, "��δ�������������",
						Toast.LENGTH_SHORT).show();
			} else {
				resultTv.setText(result);
			}
		}
	}
}
