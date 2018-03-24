package com.flyscale.alarms.alarms;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.TextView;

import com.flyscale.alarms.R;
import com.flyscale.alarms.SettingsActivity;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.DLog;
import com.flyscale.alarms.utils.Utils;
import com.flyscale.alarms.widget.multiwaveview.GlowPadView;

public class AlarmActivity2 extends Activity {
    private static final String TAG = "AlarmActivity";
    private int mVolumeBehavior;
    private AlarmInstance mInstance;

    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DLog.d(TAG, " Broadcast Receiver - " + action);
            if (action.equals(ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                dismiss();
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                finish();
            } else {
                DLog.i(TAG, "Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };


    /**
     * С˯
     */
    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mInstance);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(this, mInstance);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long instanceId = AlarmInstance.getId(getIntent().getData());
		mInstance=AlarmInstance.getInstanceById(this.getContentResolver(), instanceId);
		if(mInstance==null){
			DLog.e(TAG, "onCreate()   mInstance is  null");
			finish();
			return;
		}

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        ///M: Don't show the wallpaper when the alert arrive. @{
        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);

        final String vol = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BUTTONS,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);
        DLog.d(TAG, "onCreate  mVolumeBehavior = " + mVolumeBehavior + ", "
                + this.getResources().getStringArray(R.array.volume_button_setting_entries)
                [mVolumeBehavior]);

        updateLayout();
        // Register to get the alarm done/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }



    private void updateLayout() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.alarm_alert, null);
        setContentView(view);
		updateTitle();
        Utils.setTimeFormat((TextClock) (view.findViewById(R.id.digitalClock)),
                (int) getResources().getDimension(R.dimen.bottom_text_size));

    }

    private void updateTitle() {
        final String titleText = mInstance.getLabelOrDefault(this);
        TextView tv = (TextView) findViewById(R.id.alertTitle);
        TextView left = (TextView) findViewById(R.id.confirm);
        TextView right = (TextView) findViewById(R.id.back);
        left.setText(getResources().getString(R.string.snooze));
        right.setText(getResources().getString(R.string.close));
        tv.setText(titleText);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;
                        case 2:
                            dismiss();
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                snooze();
                break;
            default:
                dismiss();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (mInstance == null) {
            super.onDestroy();
            return;
        }

        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        snooze();
        super.onBackPressed();
    }
}
