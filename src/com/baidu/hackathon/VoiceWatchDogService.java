package com.baidu.hackathon;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.baidu.android.speech.asr.IWakeUpListener;
import com.baidu.android.speech.asr.WakeUpHelper;
import com.baidu.android.speech.asr.audiosource.RecorderAudioSource;

public class VoiceWatchDogService extends Service {
	private WakeUpHelper mWakeUpHelper;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mWakeUpHelper = new WakeUpHelper(VoiceWatchDogService.this);
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startWakeUp();
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopWakeUp();
		mWakeUpHelper.destroy();
	}

	private void startWakeUp() {
		mWakeUpHelper.startListening(new RecorderAudioSource(),
				new IWakeUpListener() {

					@Override
					public void onWakeUp(String result) {
						Log.d("qcw", "wake up service success " + result);
						Intent i = new Intent(MainActivity.VOICE_RESULT_ACTION);
						i.putExtra(MainActivity.VOICE_RESULT_EXTRA, result);

						LocalBroadcastManager.getInstance(
								VoiceWatchDogService.this).sendBroadcast(i);
					}

					@Override
					public void onError(int arg0) {
						Log.d("qcw", "wake up service onError " + arg0);
						mWakeUpHelper.cancel();
						mWakeUpHelper.startListening(new RecorderAudioSource(),
								this, null);
					}
				}, null);

	}

	private void stopWakeUp() {
		mWakeUpHelper.cancel();
	}

}
