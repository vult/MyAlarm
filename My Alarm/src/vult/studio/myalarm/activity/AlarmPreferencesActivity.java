package vult.studio.myalarm.activity;

import java.util.Calendar;

import vult.studio.myalarm.R;
import vult.studio.myalarm.adapter.AlarmPreferenceListAdapter;
import vult.studio.myalarm.database.Database;
import vult.studio.myalarm.entity.Alarm;
import vult.studio.myalarm.preferences.AlarmPreference;
import vult.studio.myalarm.preferences.AlarmPreference.Key;
import vult.studio.myalarm.service.AlarmServiceBroadcastReciever;
import vult.studio.myalarm.util.Utils;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * @author vult
 * 
 */
public class AlarmPreferencesActivity extends ListActivity implements
		android.view.View.OnClickListener {

	private ImageButton mIbDelete;
	private TextView mTvOk;
	private TextView mTvCancel;
	private Alarm mAlarmObject;
	private MediaPlayer mMediaPlayer;
	private CountDownTimer mAlarmToneTimer;
	private TextView mTvTitleBar;
	private AdView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alarm_preferences);
		adView = (AdView) this.findViewById(R.id.adView);

		if (Utils.isWifiConnected(this)) {
			adView.setVisibility(View.VISIBLE);
		} else {
			adView.setVisibility(View.GONE);
		}

		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		mIbDelete = (ImageButton) findViewById(R.id.toolbar).findViewById(
				R.id.alarm_preferences_bt_delete);
		mTvOk = (TextView) findViewById(R.id.alarm_preferences_tv_ok);
		mTvCancel = (TextView) findViewById(R.id.alarm_preferences_tv_cancel);
		mTvTitleBar = (TextView) findViewById(R.id.alarm_preferences_bt_delete_tv_title_bar);

		String string = getIntent().getStringExtra("ALARM_ACTIVITY");
		if (string != null) {
			mIbDelete.setVisibility(View.GONE);
			mTvTitleBar.setText("Add New Alarm");
		} else {
			mIbDelete.setVisibility(View.VISIBLE);
		}

		mIbDelete.setOnClickListener(this);
		mTvOk.setOnClickListener(this);
		mTvCancel.setOnClickListener(this);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.containsKey("alarm")) {
			setMathAlarm((Alarm) bundle.getSerializable("alarm"));
		}

	}

	private void callMathAlarmScheduleService() {
		Intent mathAlarmServiceIntent = new Intent(this,
				AlarmServiceBroadcastReciever.class);
		sendBroadcast(mathAlarmServiceIntent, null);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final AlarmPreferenceListAdapter alarmPreferenceListAdapter = (AlarmPreferenceListAdapter) getListAdapter();
		final AlarmPreference alarmPreference = (AlarmPreference) alarmPreferenceListAdapter
				.getItem(position);

		AlertDialog.Builder alert;
		switch (alarmPreference.getType()) {
		case BOOLEAN:
			CheckedTextView checkedTextView = (CheckedTextView) v;
			boolean checked = !checkedTextView.isChecked();
			((CheckedTextView) v).setChecked(checked);
			switch (alarmPreference.getKey()) {
			case ALARM_ACTIVE:
				mAlarmObject.setAlarmActive(checked);
				if (checked) {
					checkedTextView.setText("Turn on");
				} else {
					checkedTextView.setText("Turn off");
				}
				break;
			case ALARM_VIBRATE:
				mAlarmObject.setVibrate(checked);
				if (checked) {
					Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					vibrator.vibrate(1000);
					checkedTextView.setText("Vibrate on");
				} else {
					checkedTextView.setText("Vibrate off");
				}
				break;
			default:
				break;
			}
			alarmPreference.setValue(checked);
			break;
		case STRING:

			alert = new AlertDialog.Builder(this);

			alert.setTitle(alarmPreference.getTitle());
			// alert.setMessage(message);

			// Set an EditText view to get user input
			final EditText input = new EditText(this);

			input.setText(alarmPreference.getValue().toString());

			alert.setView(input);
			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							alarmPreference
									.setValue(input.getText().toString());

							if (alarmPreference.getKey() == Key.ALARM_NAME) {
								mAlarmObject.setAlarmName(alarmPreference
										.getValue().toString());
							}

							alarmPreferenceListAdapter
									.setMathAlarm(getMathAlarm());
							alarmPreferenceListAdapter.notifyDataSetChanged();
						}
					});
			alert.show();
			break;
		case LIST:
			alert = new AlertDialog.Builder(this);

			alert.setTitle(alarmPreference.getTitle());
			// alert.setMessage(message);

			CharSequence[] items = new CharSequence[alarmPreference
					.getOptions().length];
			for (int i = 0; i < items.length; i++)
				items[i] = alarmPreference.getOptions()[i];

			alert.setItems(items, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (alarmPreference.getKey()) {
					case ALARM_DIFFICULTY:
						Alarm.Difficulty d = Alarm.Difficulty.values()[which];
						mAlarmObject.setDifficulty(d);
						break;
					case ALARM_TONE:
						mAlarmObject
								.setAlarmTonePath(alarmPreferenceListAdapter
										.getAlarmTonePaths()[which]);
						if (mAlarmObject.getAlarmTonePath() != null) {
							if (mMediaPlayer == null) {
								mMediaPlayer = new MediaPlayer();
							} else {
								if (mMediaPlayer.isPlaying())
									mMediaPlayer.stop();
								mMediaPlayer.reset();
							}
							try {
								// mediaPlayer.setVolume(1.0f, 1.0f);
								mMediaPlayer.setVolume(0.2f, 0.2f);
								mMediaPlayer.setDataSource(
										AlarmPreferencesActivity.this, Uri
												.parse(mAlarmObject
														.getAlarmTonePath()));
								mMediaPlayer
										.setAudioStreamType(AudioManager.STREAM_ALARM);
								mMediaPlayer.setLooping(false);
								mMediaPlayer.prepare();
								mMediaPlayer.start();

								// Force the mediaPlayer to stop after 3
								// seconds...
								if (mAlarmToneTimer != null)
									mAlarmToneTimer.cancel();
								mAlarmToneTimer = new CountDownTimer(3000, 3000) {
									@Override
									public void onTick(long millisUntilFinished) {

									}

									@Override
									public void onFinish() {
										try {
											if (mMediaPlayer.isPlaying())
												mMediaPlayer.stop();
										} catch (Exception e) {

										}
									}
								};
								mAlarmToneTimer.start();
							} catch (Exception e) {
								try {
									if (mMediaPlayer.isPlaying())
										mMediaPlayer.stop();
								} catch (Exception e2) {

								}
							}
						}
						break;
					default:
						break;
					}
					alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
					alarmPreferenceListAdapter.notifyDataSetChanged();
				}

			});

			alert.show();
			break;
		case MULTIPLE_LIST:
			alert = new AlertDialog.Builder(this);

			alert.setTitle(alarmPreference.getTitle());
			// alert.setMessage(message);

			CharSequence[] multiListItems = new CharSequence[alarmPreference
					.getOptions().length];
			for (int i = 0; i < multiListItems.length; i++)
				multiListItems[i] = alarmPreference.getOptions()[i];

			boolean[] checkedItems = new boolean[multiListItems.length];
			for (Alarm.Day day : getMathAlarm().getDays()) {
				checkedItems[day.ordinal()] = true;
			}
			alert.setMultiChoiceItems(multiListItems, checkedItems,
					new OnMultiChoiceClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								int which, boolean isChecked) {

							Alarm.Day thisDay = Alarm.Day.values()[which];

							if (isChecked) {
								mAlarmObject.addDay(thisDay);
							} else {
								// Only remove the day if there are more than 1
								// selected
								if (mAlarmObject.getDays().length > 1) {
									mAlarmObject.removeDay(thisDay);
								} else {
									// If the last day was unchecked, re-check
									// it
									((AlertDialog) dialog).getListView()
											.setItemChecked(which, true);
								}
							}

						}
					});
			alert.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					alarmPreferenceListAdapter.setMathAlarm(getMathAlarm());
					alarmPreferenceListAdapter.notifyDataSetChanged();
				}
			});
			alert.show();
			break;
		case TIME:
			TimePickerDialog timePickerDialog = new TimePickerDialog(this,
					new OnTimeSetListener() {

						@Override
						public void onTimeSet(TimePicker timePicker, int hours,
								int minutes) {
							Calendar newAlarmTime = Calendar.getInstance();
							newAlarmTime.set(Calendar.HOUR_OF_DAY, hours);
							newAlarmTime.set(Calendar.MINUTE, minutes);
							newAlarmTime.set(Calendar.SECOND, 0);
							mAlarmObject.setAlarmTime(newAlarmTime);
							alarmPreferenceListAdapter
									.setMathAlarm(getMathAlarm());
							alarmPreferenceListAdapter.notifyDataSetChanged();
						}
					}, mAlarmObject.getAlarmTime().get(Calendar.HOUR_OF_DAY),
					mAlarmObject.getAlarmTime().get(Calendar.MINUTE), true);
			timePickerDialog.setTitle(alarmPreference.getTitle());
			timePickerDialog.show();
		default:
			break;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] bundle = { getMathAlarm(), getListAdapter() };
		return bundle;
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (mMediaPlayer != null)
				mMediaPlayer.release();
		} catch (Exception e) {
		}

		// setListAdapter(null);

	}

	@Override
	protected void onResume() {

		// Restore data in event of case of orientation change
		@SuppressWarnings("deprecation")
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			if (getMathAlarm() == null)
				setMathAlarm(new Alarm());

			setListAdapter(new AlarmPreferenceListAdapter(this, getMathAlarm()));
		} else {
			Object[] bundle = (Object[]) data;
			setMathAlarm((Alarm) bundle[0]);
			setListAdapter((AlarmPreferenceListAdapter) bundle[1]);
		}
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);

		super.onResume();
	}

	public Alarm getMathAlarm() {
		return mAlarmObject;
	}

	public void setMathAlarm(Alarm alarm) {
		this.mAlarmObject = alarm;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.alarm_preferences_bt_delete) {
			Builder dialog = new AlertDialog.Builder(
					AlarmPreferencesActivity.this);
			dialog.setTitle("Delete");
			dialog.setMessage("Are you sure you want to delete this alarm?");
			dialog.setPositiveButton("Ok", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					Database.init(getApplicationContext());
					if (getMathAlarm().getId() < 1) {
						// Alarm not saved
					} else {
						Database.deleteEntry(mAlarmObject);
						callMathAlarmScheduleService();
					}
					Toast.makeText(AlarmPreferencesActivity.this,
							"This alarm has been deleted successful",
							Toast.LENGTH_SHORT).show();
					finish();
				}
			});
			dialog.setNegativeButton("Cancel", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog.show();
		} else if (v.getId() == R.id.alarm_preferences_tv_cancel) {
			finish();
		} else if (v.getId() == R.id.alarm_preferences_tv_ok) {
			Database.init(getApplicationContext());
			if (getMathAlarm().getId() < 1) {
				Database.create(getMathAlarm());
			} else {
				Database.update(getMathAlarm());
			}
			callMathAlarmScheduleService();
			Toast.makeText(AlarmPreferencesActivity.this,
					getMathAlarm().getTimeUntilNextAlarmMessage(),
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	// @Override
	// public boolean dispatchTouchEvent(MotionEvent event) {
	// View v = getCurrentFocus();
	// boolean ret = super.dispatchTouchEvent(event);
	//
	// if (v instanceof EditText) {
	// View w = getCurrentFocus();
	// int scrcoords[] = new int[2];
	// w.getLocationOnScreen(scrcoords);
	// float x = event.getRawX() + w.getLeft() - scrcoords[0];
	// float y = event.getRawY() + w.getTop() - scrcoords[1];
	//
	// if (event.getAction() == MotionEvent.ACTION_UP
	// && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w
	// .getBottom())) {
	// InputMethodManager imm = (InputMethodManager)
	// getSystemService(Context.INPUT_METHOD_SERVICE);
	// imm.hideSoftInputFromWindow(getWindow().getCurrentFocus()
	// .getWindowToken(), 0);
	// }
	// }
	// return ret;
	// }

}
