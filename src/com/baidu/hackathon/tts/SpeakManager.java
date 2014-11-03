package com.baidu.hackathon.tts;

import com.baidu.hackathon.MainActivity;

import android.content.Context;
import android.text.TextUtils;

public class SpeakManager {

	private Context mContext;
	private static SpeakManager sInstance;
	private TTSObject mTTSObject;

	private SpeakManager(Context context) {
		mContext = context;
	}

	public synchronized static SpeakManager getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new SpeakManager(context);
		}
		return sInstance;
	}

	public void speak(String text) {
		stopSpeak();
		mTTSObject = new TTSObject(mContext);
		if (!TextUtils.isEmpty(text)) {
			mTTSObject.speak(text);
		}
	}

	public void stopSpeak() {
		if (mTTSObject != null) {
			mTTSObject.cancle();
		}
	}

}
