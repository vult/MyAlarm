package vult.studio.myalarm.activity;

import vult.studio.myalarm.R;
import vult.studio.myalarm.adapter.AlarmListAdapter;
import vult.studio.myalarm.database.Database;
import vult.studio.myalarm.entity.Alarm;
import vult.studio.myalarm.service.AlarmServiceBroadcastReciever;
import vult.studio.myalarm.util.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * @author vult
 * 
 */
public class AlarmActivity extends Activity implements
		android.view.View.OnClickListener {

	private ImageButton mBtAddNew;
	private ListView mListView;
	private AlarmListAdapter mAlarmAdapter;
	private Object mData;
	private TextView mTvEmpty;
	private long lastPressedTime;
	private static final int PERIOD = 2000;
	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alarm_activity);

		adView = (AdView) this.findViewById(R.id.adView);
		if (Utils.isWifiConnected(this)) {
			adView.setVisibility(View.VISIBLE);
		} else {
			adView.setVisibility(View.GONE);
		}
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		mBtAddNew = (ImageButton) findViewById(R.id.button_news);
		mTvEmpty = (TextView) findViewById(R.id.alarm_activity_tv_empty);

		mBtAddNew.setOnClickListener(this);

		mListView = (ListView) findViewById(R.id.list);
		// mSwipeListView = (SwipeListView) findViewById(android.R.id.list);

		mData = getLastNonConfigurationInstance();
		if (mData == null) {
			mAlarmAdapter = new AlarmListAdapter(this);
		} else {
			mAlarmAdapter = (AlarmListAdapter) mData;
		}
		mListView.setAdapter(mAlarmAdapter);
		mAlarmAdapter.notifyDataSetChanged();

		if (mListView.getCount() == 0) {
			mTvEmpty.setVisibility(View.VISIBLE);
		} else {
			mTvEmpty.setVisibility(View.GONE);
		}

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				Alarm alarm = (Alarm) mAlarmAdapter.getItem(position);
				Intent intent = new Intent(AlarmActivity.this,
						AlarmPreferencesActivity.class);
				intent.putExtra("alarm", alarm);
				startActivity(intent);

			}
		});

		mListView.setLongClickable(true);
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView,
					View view, int position, long id) {
				final Alarm alarm = (Alarm) mAlarmAdapter.getItem(position);
				Builder dialog = new AlertDialog.Builder(AlarmActivity.this);
				dialog.setTitle("Delete");
				dialog.setMessage("Are you sure you want to delete this alarm?");
				dialog.setPositiveButton("Ok", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						mAlarmAdapter.getMathAlarms().remove(alarm);
						mAlarmAdapter.notifyDataSetChanged();

						if (mListView.getCount() == 0) {
							mTvEmpty.setVisibility(View.VISIBLE);
						} else {
							mTvEmpty.setVisibility(View.GONE);
						}

						Database.init(AlarmActivity.this);
						Database.deleteEntry(alarm);

						AlarmActivity.this.callMathAlarmScheduleService();
					}
				});
				dialog.setNegativeButton("Cancel", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				dialog.show();

				return true;
			}
		});
		callMathAlarmScheduleService();
	}

	private void callMathAlarmScheduleService() {
		Intent mathAlarmServiceIntent = new Intent(AlarmActivity.this,
				AlarmServiceBroadcastReciever.class);
		sendBroadcast(mathAlarmServiceIntent, null);
	}

	@Override
	protected void onPause() {
		// setListAdapter(null);
		Database.deactivate();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		@SuppressWarnings("deprecation")
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			mAlarmAdapter = new AlarmListAdapter(this);
		} else {
			mAlarmAdapter = (AlarmListAdapter) data;
		}

		mListView.setAdapter(mAlarmAdapter);
		if (mListView.getCount() == 0) {
			mTvEmpty.setVisibility(View.VISIBLE);
		} else {
			mTvEmpty.setVisibility(View.GONE);
		}
		mAlarmAdapter.notifyDataSetChanged();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mAlarmAdapter;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.checkBox_alarm_active) {
			CheckBox checkBox = (CheckBox) v;
			Alarm alarm = (Alarm) mAlarmAdapter.getItem((Integer) checkBox
					.getTag());
			alarm.setAlarmActive(checkBox.isChecked());
			Database.update(alarm);
			AlarmActivity.this.callMathAlarmScheduleService();
			if (checkBox.isChecked()) {
				Toast.makeText(AlarmActivity.this,
						alarm.getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG)
						.show();
			}
		} else if (v.getId() == R.id.button_news) {
			Intent newAlarmIntent = new Intent(AlarmActivity.this,
					AlarmPreferencesActivity.class);
			newAlarmIntent.putExtra("ALARM_ACTIVITY", "ALARM_ACTIVITY");
			startActivity(newAlarmIntent);
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				switch (event.getAction()) {
				case KeyEvent.ACTION_DOWN:
					if (event.getDownTime() - lastPressedTime < PERIOD) {
						finish();
					} else {
						Toast.makeText(AlarmActivity.this,
								"Press again to exist.", Toast.LENGTH_SHORT)
								.show();
						lastPressedTime = event.getEventTime();
					}
					return true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
