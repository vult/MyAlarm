package vult.studio.myalarm.activity;

import vult.studio.myalarm.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class SplashActivity extends Activity {
	private final int STOPSPLASH = 0;
	// time in milliseconds
	private final long SPLASHTIME = 2000;
	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		adView = (AdView) this.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		Message msg = new Message();
		msg.what = STOPSPLASH;
		try {
			splashHandler.sendMessageDelayed(msg, SPLASHTIME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Handler for splash screen
	 */
	private Handler splashHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == STOPSPLASH) {
				startActivity(new Intent(SplashActivity.this,
						AlarmActivity.class));
				finish();
			}
		}
	};

}
