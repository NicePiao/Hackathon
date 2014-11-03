package com.baidu.hackathon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.baidu.hackathon.CameraView.ICustomPicCallback;
import com.baidu.hackathon.tts.SpeakManager;

public class CameraActivity extends Activity {

	private CameraView cameraView;
	private Button cameraViewBtn;
	private String picSavePath;

	public static final String PIC_SAVE_PATH_EXTRA = "pic_save_path_extra";

	private LocalBroadcastManager mLocalBroadcastManager;
	private VoiceResultReceiver mVoiceResultReceiver;
	private SensorManager sensorManager;
	private PowerManager.WakeLock mWakeLock;
	private static final String TAG = CameraActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		picSavePath = getIntent().getStringExtra(PIC_SAVE_PATH_EXTRA);
		setContentView(R.layout.activity_camera_layout);
		screenHighLight();
		initCameraView();

		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		mVoiceResultReceiver = new VoiceResultReceiver();
		registerVoiceReceiver();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}

	private void screenHighLight() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mWakeLock.acquire();
	}

	private void initCameraView() {
		cameraView = (CameraView) findViewById(R.id.camera_view);
		cameraViewBtn = (Button) findViewById(R.id.camera_shutter_btn);
		cameraViewBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				shutterPic();
			}
		});
		cameraViewBtn.requestFocus();
	}

	private void shutterPic() {
		cameraView.setPicSavePath(picSavePath);
		cameraView.setCustomPicCallBackImpl(new ICustomPicCallback() {

			@Override
			public void onPictureTaken(byte[] data, String picSavePath) {
				savePic(data);
				setResult(RESULT_OK);
				finish();
				Log.d("qcw", data.length + " " + picSavePath);
			}
		});
		cameraView.shutter();
	}

	private void savePic(byte[] data) {
		FileOutputStream fos = null;
		try {
			File pictureFile = new File(picSavePath);
			fos = new FileOutputStream(pictureFile);
			fos.write(data);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cameraView.releaseCamera();
		unregisterVoiceReceiver();
	}

	private void handlerVoiceResult(String result) {
		if ("确定拍照".equalsIgnoreCase(result)) {
			SpeakManager.getInstance(this).speak("好的");
			shutterPic();
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

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (sensorManager != null) {// 注册监听器
			sensorManager.registerListener(sensorEventListener,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (sensorManager != null) {
			sensorManager.unregisterListener(sensorEventListener);
		}
	}

	private int last_x, last_y, last_z;
	private long last_current_time, accumulation_time;

	/**
	 * 重力感应监听
	 */
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// 四舍五入
			int x = Math.round(event.values[0]); // x轴方向的重力加速度，向右为正
			int y = Math.round(event.values[1]); // y轴方向的重力加速度，向前为正
			int z = Math.round(event.values[2]); // z轴方向的重力加速度，向上为正
			// Log.i(TAG, "x轴方向的重力加速度" + x + "；y轴方向的重力加速度" + y + "；z轴方向的重力加速度" +
			// z);

			long current_time = System.currentTimeMillis();
			if (last_current_time == 0) {
				last_current_time = current_time;
			}
			if (Math.abs(x - last_x) <= 0 && Math.abs(y - last_y) <= 0
					&& Math.abs(z - last_z) <= 0) {
				// Log.i(TAG, "accumulation_time:" + accumulation_time);
				if (accumulation_time >= 3 * 1000) {
					Log.i(TAG, "自动拍照");
					shutterPic();
					accumulation_time = 0;
				}
				accumulation_time += (current_time - last_current_time);
			} else {
				accumulation_time = 0;
			}

			last_current_time = current_time;
			last_x = x;
			last_y = y;
			last_z = z;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

}
