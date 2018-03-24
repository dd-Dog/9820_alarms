package com.flyscale.alarms;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flyscale.alarms.options.AlarmSetOptions;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.AlarmUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/3/23 0023.
 */

public class SetAlarmTimeFragment extends BaseFragment {


    public static final String TAG = "SetAlarmTimeFragment";
    private TextView oneHours;
    private TextView twoHours;
    private TextView oneMinutes;
    private TextView twoMinutes;
    private String time = "";
    private List<TextView> mList = new ArrayList<TextView>();
    private char[] storageTime;
    private List<String> mTime = new ArrayList<String>();
    private int mIndex;
    private Alarm mAlarm;
    private boolean backEnabled = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        initData();
        return initView();
    }


    private View initView() {
        View view = mActivity.getLayoutInflater().inflate(R.layout.activity_alarm_time, null);
        oneHours = (TextView) view.findViewById(R.id.hours_one);
        twoHours = (TextView) view.findViewById(R.id.hours_two);
        oneMinutes = (TextView) view.findViewById(R.id.minutes_one);
        twoMinutes = (TextView) view.findViewById(R.id.minutes_two);
        oneHours.setTextColor(Color.WHITE);
        oneHours.setBackgroundColor(Color.BLACK);


        oneHours.setText(String.valueOf(storageTime[0]));
        twoHours.setText(String.valueOf(storageTime[1]));
        oneMinutes.setText(String.valueOf(storageTime[2]));
        twoMinutes.setText(String.valueOf(storageTime[3]));

        mList.add(oneHours);
        mList.add(twoHours);
        mList.add(oneMinutes);
        mList.add(twoMinutes);
        return view;
    }

    private void initData() {
        mAlarm = MainActivity.mAlarm;
        String hour = String.valueOf(mAlarm.hour);
        String minutes = String.valueOf(mAlarm.minutes);
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        String time = hour + minutes;
        Log.i(TAG, "time=" + time);
        storageTime = time.toCharArray();

        mTime.add(String.valueOf(storageTime[0]));
        mTime.add(String.valueOf(storageTime[1]));
        mTime.add(String.valueOf(storageTime[2]));
        mTime.add(String.valueOf(storageTime[3]));
    }

    @Override
    public boolean onKeyUp(int keyCode) {
        Log.i(TAG, "onKeyUp,keyCode=" + keyCode);
        switch (keyCode) {
            //确定
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                backEnabled = false;
                String h = mTime.get(0) + mTime.get(1);
                String m = mTime.get(2) + mTime.get(3);
                int hour = Integer.parseInt(h);
                int minute = Integer.parseInt(m);
                mAlarm.hour = hour;
                mAlarm.minutes = minute;
                MainActivity.mAlarm = mAlarm;
                AlarmUtils.asyncUpdateAlarm(mContext, mAlarm, true, new AlarmCallBack() {
                    @Override
                    public void onExecuted(AlarmInstance instance) {
                        Log.i(TAG, "onExecuted");
                        mActivity.switchContent(SetAlarmTimeFragment.this, mActivity.getFragment(
                                AlarmSetOptions.class.getName(), AlarmSetOptions.TAG));
                    }
                });
                return true;
            //左
            case KeyEvent.KEYCODE_DPAD_LEFT:
                setLeftDown();
                return true;
            //右
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                setRightDown();
                return true;
            //0
            case KeyEvent.KEYCODE_0:
                mList.get(mIndex).setText("0");
                mTime.set(mIndex, "0");
                setRightDown();
                return true;
            //1
            case KeyEvent.KEYCODE_1:
                mList.get(mIndex).setText("1");
                mTime.set(mIndex, "1");
                setRightDown();
                return true;
            //2
            case KeyEvent.KEYCODE_2:
                CharSequence text = mList.get(1).getText();
                if ((text.equals("4") || text.equals("5") || text.equals("6") ||
                        text.equals("7") || text.equals("8") || text.equals("9")) && mIndex == 0) {

                } else {
                    mList.get(mIndex).setText("2");
                    mTime.set(mIndex, "2");
                    setRightDown();
                }
                return true;
            //3
            case KeyEvent.KEYCODE_3:
                if (mIndex > 0) {
                    mList.get(mIndex).setText("3");
                    mTime.set(mIndex, "3");
                    setRightDown();
                }
                return true;
            //4
            case KeyEvent.KEYCODE_4:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0) {
                    mList.get(mIndex).setText("4");
                    mTime.set(mIndex, "4");
                    setRightDown();
                }
                return true;
            //5
            case KeyEvent.KEYCODE_5:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0) {
                    mList.get(mIndex).setText("5");
                    mTime.set(mIndex, "5");
                    setRightDown();
                }
                return true;
            //6
            case KeyEvent.KEYCODE_6:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0 && (mIndex == 1 || mIndex == 3)) {
                    mList.get(mIndex).setText("6");
                    mTime.set(mIndex, "6");
                    setRightDown();
                }
                return true;
            //7
            case KeyEvent.KEYCODE_7:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0 && (mIndex == 1 || mIndex == 3)) {
                    mList.get(mIndex).setText("7");
                    mTime.set(mIndex, "7");
                    setRightDown();
                }
                break;
            //8
            case KeyEvent.KEYCODE_8:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0 && (mIndex == 1 || mIndex == 3)) {
                    mList.get(mIndex).setText("8");
                    mTime.set(mIndex, "8");
                    setRightDown();
                }
                return true;
            //9
            case KeyEvent.KEYCODE_9:
                if (mList.get(0).getText().equals("2") && mIndex == 1) {

                } else if (mIndex > 0 && (mIndex == 1 || mIndex == 3)) {
                    mList.get(mIndex).setText("9");
                    mTime.set(mIndex, "9");
                    setRightDown();
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        mActivity.switchContent(SetAlarmTimeFragment.this, mActivity.getFragment(
                AlarmSetOptions.class.getName(), AlarmSetOptions.TAG));
        return true;
    }

    //按右键增加的方法
    private void setRightDown() {
        mList.get(mIndex).setBackgroundColor(Color.WHITE);
        mList.get(mIndex).setTextColor(Color.BLACK);
        mIndex = (mIndex + 1) % 4;//取余
        mList.get(mIndex).setBackgroundColor(Color.BLACK);
        mList.get(mIndex).setTextColor(Color.WHITE);
    }

    //按左键执行的方法
    private void setLeftDown() {
        mList.get(mIndex).setBackgroundColor(Color.WHITE);
        mList.get(mIndex).setTextColor(Color.BLACK);
        mIndex = (mIndex - 1) % 4;//取余
        if (mIndex < 0) {
            mIndex = 3;
        }
        mList.get(mIndex).setBackgroundColor(Color.BLACK);
        mList.get(mIndex).setTextColor(Color.WHITE);
    }

    @Override
    public void onResume() {
        super.onResume();
        backEnabled = true;
        mActivity.setCurrentFragment(this, TAG);
    }


}
