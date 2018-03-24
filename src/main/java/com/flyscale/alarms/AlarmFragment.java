package com.flyscale.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import com.flyscale.alarms.alarms.AlarmService;
import com.flyscale.alarms.alarms.AlarmStateManager;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.DLog;
import com.flyscale.alarms.utils.Utils;

/**
 * Created by Administrator on 2018/3/24 0024.
 */

public class AlarmFragment extends BaseFragment {
    private AlarmInstance mInstance;
    public static final String TAG = "AlarmFragment";
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
                DLog.i(TAG, "Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = mActivity.getLayoutInflater().inflate(R.layout.alarm_alert, null);
        Utils.setTimeFormat((TextClock) (view.findViewById(R.id.digitalClock)),
                (int) getResources().getDimension(R.dimen.bottom_text_size));
        //update title
        final String titleText = mInstance.getLabelOrDefault(mContext);
        TextView tv = (TextView) view.findViewById(R.id.alertTitle);
        tv.setText(titleText);

        // Register to get the alarm done/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        mActivity.registerReceiver(mReceiver, filter);
        return view;
    }


    private void snooze() {
        AlarmStateManager.setSnoozeState(mContext, mInstance);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(mContext, mInstance);
    }

    @Override
    public boolean onKeyUp(int keyCode) {
        return false;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity.unregisterReceiver(mReceiver);
    }
}
