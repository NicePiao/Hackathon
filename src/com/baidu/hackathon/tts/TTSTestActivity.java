package com.baidu.hackathon.tts;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.hackathon.R;
import com.baidu.speechsynthesizer.SpeechSynthesizer;
import com.baidu.speechsynthesizer.SpeechSynthesizerListener;
import com.baidu.speechsynthesizer.publicutility.SpeechError;

public class TTSTestActivity extends Activity implements OnClickListener,
		SpeechSynthesizerListener {
	private EditText edit;
	private SpeechSynthesizer speechSynthesizer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_tts);
		edit = (EditText) findViewById(R.id.tts_test_edit);
		findViewById(R.id.tts_test_work).setOnClickListener(this);
		initTTS();
	}

	private void initTTS() {
		speechSynthesizer = new SpeechSynthesizer(getApplicationContext(),
				"hackathon_tts", this);
		speechSynthesizer.setApiKey("NtY6FIjAXEkGASXfkoxLlx5C",
				"EwVcAOsgIGGSLelz8csxYI63VVXxFFtk");
		speechSynthesizer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "10");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, "1");
		speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, "4");
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.tts_test_work:
			int ret = speechSynthesizer.speak(edit.getText().toString());
			if (ret != 0) {
				// ��ʼ�ϳ���ʧ��
			}
			break;

		default:
			break;
		}
	}

	@Override
	public void onStartWorking(SpeechSynthesizer synthesizer) {
		showToast("�����ϳɿ�ʼ��������ȴ����...");
	}

	@Override
	public void onSpeechStart(SpeechSynthesizer synthesizer) {
		showToast("�����ʶ���ʼ");
	}

	@Override
	public void onSpeechResume(SpeechSynthesizer synthesizer) {
		showToast("�����ʶ�����");
	}

	@Override
	public void onSpeechProgressChanged(SpeechSynthesizer synthesizer,
			int progress) {
		// TODO Auto-generated method stub
		showToast("�����ʶ���ȣ�" + progress + "%");
	}

	@Override
	public void onSpeechPause(SpeechSynthesizer synthesizer) {
		showToast("�����ʶ�����ͣ");
	}

	@Override
	public void onSpeechFinish(SpeechSynthesizer synthesizer) {
		showToast("�����ʶ���ֹͣ");
	}

	@Override
	public void onNewDataArrive(SpeechSynthesizer synthesizer,
			byte[] dataBuffer, int dataLength) {
		showToast("�µ������ϳ���Ƶ��ݣ�" + dataLength);
	}

	@Override
	public void onError(SpeechSynthesizer synthesizer, SpeechError error) {
		showToast("�����ϳɷ������" + error.errorDescription + "(" + error.errorCode
				+ ")");
	}

	@Override
	public void onCancel(SpeechSynthesizer synthesizer) {
		showToast("�����ϳ���ȡ��");
	}

	@Override
	public void onBufferProgressChanged(SpeechSynthesizer synthesizer,
			int progress) {
		// TODO Auto-generated method stub
		showToast("�����ϳ���ݻ����ȣ�" + progress + "%");
	}

	private void showToast(final String toast) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Toast.makeText(TTSTestActivity.this, toast, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}
}
