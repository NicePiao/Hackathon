package com.baidu.hackathon.tts;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

import com.baidu.speechsynthesizer.SpeechSynthesizer;

public class TTSObject {
	private Context mActivity;
	private SpeechSynthesizer speechSynthesizer;

	public TTSObject(Context activity) {
		mActivity = activity;
		initTTS();
	}

	private void initTTS() {
		speechSynthesizer = new SpeechSynthesizer(mActivity, "hackathon_tts",
				null);
		speechSynthesizer.setApiKey("NtY6FIjAXEkGASXfkoxLlx5C",
				"EwVcAOsgIGGSLelz8csxYI63VVXxFFtk");
		speechSynthesizer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//		AudioManager am=(AudioManager)mActivity.getSystemService(Context.AUDIO_SERVICE);  
//		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "10");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, "1");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, "4");
	}

	public void speak(String speech) {
		speechSynthesizer.speak(speech);
	}

	public void cancle() {
		speechSynthesizer.cancel();
	}
}
