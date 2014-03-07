package vult.studio.myalarm.activity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import vult.studio.myalarm.R;
import vult.studio.myalarm.alert.MathProblem;
import vult.studio.myalarm.alert.StaticWakeLock;
import vult.studio.myalarm.entity.Alarm;
import vult.studio.myalarm.util.Utils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author vult
 * 
 */
public class AlarmAlertActivity extends Activity implements OnClickListener {

	private Alarm mAlarmObject;
	private MediaPlayer mMediaPlayer;
	private StringBuilder mAnswerBuilder = new StringBuilder();
	private MathProblem mMathProblem;
	private Vibrator mVibrator;
	private boolean alarmActive;
	private TextView mProblemView;
	private TextView mAnswerView;
	private String mAnswerString;
	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.alarm_alert);
		adView = (AdView) this.findViewById(R.id.adView);

		if (Utils.isWifiConnected(this)) {
			adView.setVisibility(View.VISIBLE);
		} else {
			adView.setVisibility(View.GONE);
		}

		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		Bundle bundle = this.getIntent().getExtras();
		mAlarmObject = (Alarm) bundle.getSerializable("alarm");

		this.setTitle(mAlarmObject.getAlarmName());

		switch (mAlarmObject.getDifficulty()) {
		case EASY:
			mMathProblem = new MathProblem(3);
			break;
		case MEDIUM:
			mMathProblem = new MathProblem(4);
			break;
		case HARD:
			mMathProblem = new MathProblem(5);
			break;
		}

		mAnswerString = String.valueOf(mMathProblem.getAnswer());
		if (mAnswerString.endsWith(".0")) {
			mAnswerString = mAnswerString.substring(0,
					mAnswerString.length() - 2);
		}

		mProblemView = (TextView) findViewById(R.id.textView1);
		mProblemView.setText(mMathProblem.toString());

		mAnswerView = (TextView) findViewById(R.id.textView2);
		mAnswerView.setText("= ?");

		((Button) findViewById(R.id.Button0)).setOnClickListener(this);
		((Button) findViewById(R.id.Button1)).setOnClickListener(this);
		((Button) findViewById(R.id.Button2)).setOnClickListener(this);
		((Button) findViewById(R.id.Button3)).setOnClickListener(this);
		((Button) findViewById(R.id.Button4)).setOnClickListener(this);
		((Button) findViewById(R.id.Button5)).setOnClickListener(this);
		((Button) findViewById(R.id.Button6)).setOnClickListener(this);
		((Button) findViewById(R.id.Button7)).setOnClickListener(this);
		((Button) findViewById(R.id.Button8)).setOnClickListener(this);
		((Button) findViewById(R.id.Button9)).setOnClickListener(this);
		((Button) findViewById(R.id.Button_clear)).setOnClickListener(this);
		((Button) findViewById(R.id.Button_decimal)).setOnClickListener(this);
		((Button) findViewById(R.id.Button_minus)).setOnClickListener(this);

		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);

		PhoneStateListener phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					Log.d(getClass().getSimpleName(), "Incoming call: "
							+ incomingNumber);
					try {
						mMediaPlayer.pause();
					} catch (IllegalStateException e) {

					}
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					Log.d(getClass().getSimpleName(), "Call State Idle");
					try {
						mMediaPlayer.start();
					} catch (IllegalStateException e) {

					}
					break;
				}
				super.onCallStateChanged(state, incomingNumber);
			}
		};

		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		// Toast.makeText(this, answerString, Toast.LENGTH_LONG).show();

		startAlarm();

	}

	@Override
	protected void onResume() {
		super.onResume();
		alarmActive = true;
	}

	private void startAlarm() {

		if (mAlarmObject.getAlarmTonePath() != "") {
			mMediaPlayer = new MediaPlayer();
			if (mAlarmObject.getVibrate()) {
				mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				long[] pattern = { 1000, 200, 200, 200 };
				mVibrator.vibrate(pattern, 0);
			}
			try {
				mMediaPlayer.setVolume(1.0f, 1.0f);
				mMediaPlayer.setDataSource(this,
						Uri.parse(mAlarmObject.getAlarmTonePath()));
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
				mMediaPlayer.start();

			} catch (Exception e) {
				mMediaPlayer.release();
				alarmActive = false;
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (!alarmActive)
			super.onBackPressed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		StaticWakeLock.lockOff(this);
	}

	@Override
	protected void onDestroy() {
		try {
			if (mVibrator != null)
				mVibrator.cancel();
		} catch (Exception e) {

		}
		try {
			mMediaPlayer.stop();
		} catch (Exception e) {

		}
		try {
			mMediaPlayer.release();
		} catch (Exception e) {

		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (!alarmActive)
			return;
		String button = (String) v.getTag();
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		if (button.equalsIgnoreCase("clear")) {
			if (mAnswerBuilder.length() > 0) {
				mAnswerBuilder.setLength(mAnswerBuilder.length() - 1);
				mAnswerView.setText(mAnswerBuilder.toString());
			}
		} else if (button.equalsIgnoreCase(".")) {
			if (!mAnswerBuilder.toString().contains(button)) {
				if (mAnswerBuilder.length() == 0)
					mAnswerBuilder.append(0);
				mAnswerBuilder.append(button);
				mAnswerView.setText(mAnswerBuilder.toString());
			}
		} else if (button.equalsIgnoreCase("-")) {
			if (mAnswerBuilder.length() == 0) {
				mAnswerBuilder.append(button);
				mAnswerView.setText(mAnswerBuilder.toString());
			}
		} else {
			mAnswerBuilder.append(button);
			mAnswerView.setText(mAnswerBuilder.toString());
			if (isAnswerCorrect()) {
				alarmActive = false;
				if (mVibrator != null)
					mVibrator.cancel();
				try {
					mMediaPlayer.stop();
				} catch (IllegalStateException ise) {

				}
				try {
					mMediaPlayer.release();
				} catch (Exception e) {

				}
				this.finish();
			}
		}
		if (mAnswerView.getText().length() >= mAnswerString.length()
				&& !isAnswerCorrect()) {
			mAnswerView.setTextColor(Color.RED);
		} else {
			mAnswerView.setTextColor(Color.WHITE);
		}
	}

	public boolean isAnswerCorrect() {
		boolean correct = false;
		try {
			correct = mMathProblem.getAnswer() == Float
					.parseFloat(mAnswerBuilder.toString());
		} catch (NumberFormatException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return correct;
	}

}
