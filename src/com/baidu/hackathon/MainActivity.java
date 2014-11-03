package com.baidu.hackathon;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.hackathon.ocr.ImgUtil;
import com.baidu.hackathon.tts.SpeakManager;
import com.baidu.hackathon.util.Processor;
import com.baidu.hackathon.util.Processor.OcrServerException;

public class MainActivity extends Activity {

	public static final String VOICE_CONTROL_TAKE_PHOTO_AGAIN = "重拍";
	public static final String VOICE_CONTROL_TAKE_PHOTO_AGAIN2 = "再来一次";
	public static final String VOICE_CONTROL_TAKE_VIEW_MORE = "查看更多";
	public static final String VOICE_CONTROL_TAKE_CHOOSE_PICTURE = "选取照片";

	public static final int ACTIVITY_STATE_NONE = 0;
	public static final int ACTIVITY_STATE_SHOWING_WAITING = 1;
	public static final int ACTIVITY_STATE_SHOWING_ERROR = 2;
	public static final int ACTIVITY_STATE_SHOWING_SUCCESS = 3;

	public static final String mPhotoFileName = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/ocr_test.jpg";
	private static final int REQUEST_CODE_TAKE_PHOTO = 1;
	private static final int REQUEST_CODE_CHOOSE_PICTURE = 2;

	private View mResultView;
	private View mErrorView;
	private View mWaittingView;

	private boolean mIsFirstCreate = true;
	private int mAcitivtyState = 0;

	private LinkedHashMap<String, String> mResultsMap = new LinkedHashMap<String, String>();
	private int mShowBaikeIndex = 0;

	private AsyncTask<Void, Void, Void> handlerTask;
	private LocalBroadcastManager mLocalBroadcastManager;
	private VoiceResultReceiver mVoiceResultReceiver;
	private PowerManager.WakeLock mWakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_layout);
		initViews();
		screenHighLight();
		showResultError();
		go2Camera();

		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		mVoiceResultReceiver = new VoiceResultReceiver();

		startService(new Intent(this, VoiceWatchDogService.class));
	}

	private void screenHighLight() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mWakeLock.acquire();
	}

	private void initViews() {
		mResultView = findViewById(R.id.result_success);
		mErrorView = findViewById(R.id.result_error);
		mWaittingView = findViewById(R.id.result_waitting);
	}

	private void go2Camera() {
		if (handlerTask != null) {
			handlerTask.cancel(true);
		}
		Intent intent = new Intent(this, CameraActivity.class);
		intent.putExtra(CameraActivity.PIC_SAVE_PATH_EXTRA, mPhotoFileName);
		startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
		mAcitivtyState = ACTIVITY_STATE_NONE;
	}

	private void go2ChoosePic() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_CODE_CHOOSE_PICTURE);
	}

	private void handleResult(final Uri imageUri) {
		// final Bitmap bitmap = ImgUtil.compressBitmap2TargetSize(imageUri,
		// 1.5f * 1024);
		final Bitmap bitmap = ImgUtil.getBitmap(getApplicationContext(),
				imageUri);

		if (handlerTask != null) {
			handlerTask.cancel(true);
		}
		handlerTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				// TODO Auto-generated method stub
				showBackroundBitmap(bitmap);
				showWaitingPage();
			}

			@Override
			protected Void doInBackground(Void... params) {
				try {
					Processor processor = new Processor(getApplicationContext());
					List<String> ocrWords = processor.processOcr(bitmap);
					mResultsMap = processor.getChineseAndBaike(ocrWords);
					mShowBaikeIndex = 0;
				} catch (Exception e) {
					if (e instanceof OcrServerException) {
						SpeakManager.getInstance(MainActivity.this).speak(
								"ocr 服务器出错");
					}
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (mResultsMap.isEmpty()) {
					showResultError();
				} else {
					showResultSuccess();
				}
			}
		};
		handlerTask.execute();
	}

	private void showBackroundBitmap(Bitmap bitmap) {
		ImageView backgroudIm = (ImageView) findViewById(R.id.backgroud_im);
		backgroudIm.setImageBitmap(bitmap);
	}

	private void showWaitingPage() {
		mResultView.setVisibility(View.INVISIBLE);
		mErrorView.setVisibility(View.INVISIBLE);
		mWaittingView.setVisibility(View.VISIBLE);
		Button againBtn = (Button) mWaittingView
				.findViewById(R.id.waiting_page_again_tip);

		againBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				go2Camera();
			}
		});
		againBtn.requestFocus();
		mAcitivtyState = ACTIVITY_STATE_SHOWING_WAITING;
	}

	private void showResultError() {
		mResultView.setVisibility(View.INVISIBLE);
		mWaittingView.setVisibility(View.INVISIBLE);
		mErrorView.setVisibility(View.VISIBLE);
		Button againBtn = (Button) mErrorView
				.findViewById(R.id.error_page_again_tip);
		againBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				go2Camera();
			}
		});
		againBtn.requestFocus();
		mAcitivtyState = ACTIVITY_STATE_SHOWING_ERROR;
	}

	private void showResultSuccess() {
		mErrorView.setVisibility(View.INVISIBLE);
		mWaittingView.setVisibility(View.INVISIBLE);
		mResultView.setVisibility(View.VISIBLE);

		TextView reusltWordsTv = (TextView) mResultView
				.findViewById(R.id.resultword);
		final TextView konwMoreBtn = (TextView) mResultView
				.findViewById(R.id.know_more);
		TextView againBtn = (TextView) mResultView.findViewById(R.id.again);
		StringBuilder resultWordsSB = new StringBuilder();
		for (String resultWord : mResultsMap.keySet()) {
			resultWordsSB.append(resultWord).append("\n");
		}
		reusltWordsTv.setText(resultWordsSB.toString());
		SpeakManager.getInstance(this).speak(resultWordsSB.toString());
		if (TextUtils.isEmpty(getNextBaikeContent(false))) {
			konwMoreBtn.setEnabled(false);
			konwMoreBtn.setVisibility(View.INVISIBLE);
		} else {
			konwMoreBtn.setEnabled(true);
			konwMoreBtn.setVisibility(View.VISIBLE);
		}

		konwMoreBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String baikeContent = getNextBaikeContent(true);
				SpeakManager.getInstance(MainActivity.this).speak(baikeContent);

				if (TextUtils.isEmpty(getNextBaikeContent(false))) {
					konwMoreBtn.setEnabled(false);
				}
			}
		});
		againBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				go2Camera();
			}
		});
		if (konwMoreBtn.isEnabled()) {
			konwMoreBtn.requestFocus();
		} else {
			againBtn.requestFocus();
		}
		mAcitivtyState = ACTIVITY_STATE_SHOWING_SUCCESS;
	}

	private String getNextBaikeContent(boolean moveIndex) {
		String baikeContent = "";

		int index = mShowBaikeIndex;
		while (TextUtils.isEmpty(baikeContent) && index < mResultsMap.size()) {
			baikeContent = (String) mResultsMap.values().toArray()[index++];
		}

		if (moveIndex) {
			mShowBaikeIndex = index;
		}

		return baikeContent;
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerVoiceReceiver();
	}

	@Override
	protected void onPause() {
		super.onPause();
		SpeakManager.getInstance(MainActivity.this).stopSpeak();
		unregisterVoiceReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWakeLock.release();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("qcw", requestCode +" "+resultCode);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
				handleResult(Uri.fromFile(new File(mPhotoFileName)));
			} else if (requestCode == REQUEST_CODE_CHOOSE_PICTURE) {
				handleResult(data.getData());
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();

		if (handlerTask != null) {
			handlerTask.cancel(true);
		}
	}

	private void handlerVoiceResult(String result) {

		if (result.equalsIgnoreCase("再拍一次")) {
			SpeakManager.getInstance(MainActivity.this).speak("好的");
			go2Camera();
		}

		if (result.equalsIgnoreCase("选取照片")) {
			SpeakManager.getInstance(MainActivity.this).speak("好的");
			go2ChoosePic();
		}

		if (result.equalsIgnoreCase("了解更多")) {
			if (mAcitivtyState == ACTIVITY_STATE_SHOWING_SUCCESS) {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView konwMoreBtn = (TextView) mResultView
								.findViewById(R.id.know_more);
						if (konwMoreBtn.isEnabled()) {
							konwMoreBtn.performClick();
						} else {
							SpeakManager.getInstance(MainActivity.this).speak(
									"没有更多内容了");
						}
					}
				});
			}
		}

	}

	public static final String VOICE_RESULT_EXTRA = "voice_result_extra";
	public static final String VOICE_RESULT_ACTION = "voice_result_action";

	public class VoiceResultReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String result = intent.getStringExtra(VOICE_RESULT_EXTRA);
			if (!TextUtils.isEmpty(result)) {
				handlerVoiceResult(result);
			}
		}
	}

	private void registerVoiceReceiver() {
		IntentFilter intentFilter = new IntentFilter(VOICE_RESULT_ACTION);
		mLocalBroadcastManager.registerReceiver(mVoiceResultReceiver,
				intentFilter);
	}

	private void unregisterVoiceReceiver() {
		if (mLocalBroadcastManager != null) {
			mLocalBroadcastManager.unregisterReceiver(mVoiceResultReceiver);
		}
	}

}
