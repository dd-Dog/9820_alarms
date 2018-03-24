package com.flyscale.alarms.options;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.flyscale.alarms.AlarmCallBack;
import com.flyscale.alarms.BaseFragment;
import com.flyscale.alarms.MainActivity;
import com.flyscale.alarms.R;
import com.flyscale.alarms.SetAlarmTimeFragment;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.provider.DaysOfWeek;
import com.flyscale.alarms.utils.AlarmUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Created by MrBian on 2018/1/11.
 */

public class CustomFragment extends BaseFragment {

    public static final String TAG = "CustomFragment";
    private ListView mOptions;
    private String[] mOptionsData;
    private Alarm mAlarm;
    private boolean[] days;
    private OptionsAdapter optionsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return initView();
    }

    private void initData() {
        mOptionsData = getResources().getStringArray(R.array.days_in_week);
        days = new boolean[mOptionsData.length];
        mAlarm = MainActivity.mAlarm;
        HashSet<Integer> setDays = mAlarm.daysOfWeek.getSetDays();
        Iterator<Integer> iterator = setDays.iterator();
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            Log.i(TAG, "next=" + next);
            days[next - 1] = true;
        }
    }

    private View initView() {
        View view = mActivity.getLayoutInflater().inflate(R.layout.fragment_larmlist_options,
                null);
        mOptions = (ListView) view.findViewById(R.id.main);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView confirm = (TextView) view.findViewById(R.id.confirm);
        confirm.setText(getResources().getString(R.string.select));
        TextView back = (TextView) view.findViewById(R.id.back);
        back.setText(getResources().getString(R.string.confirm));
        title.setText(getResources().getString(R.string.custom));
        mOptions.setSelection(0);
        mOptions.setDivider(null);
        optionsAdapter = new OptionsAdapter();
        mOptions.setAdapter(optionsAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //进入界面时listview可能没有焦点，导致字体颜色和背景不对应,要主动获取焦点
        mOptions.requestFocus();
        mActivity.setCurrentFragment(this, TAG);
    }

    public boolean onKeyUp(int keyCode) {
        Log.i(TAG, "onKeyUp::keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_MENU:
                int position = mOptions.getSelectedItemPosition();
                days[position] = !days[position];
                optionsAdapter.notifyDataSetChanged();
                break;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        String s = "";
        for (int i = 0; i < days.length; i++) {
            if (days[i]) {
                s = s + "1";
            } else {
                s = s + "0";
            }
        }
        int bits = Integer.parseInt(s);
        mAlarm.daysOfWeek.mBitSet = bits;
        AlarmUtils.asyncUpdateAlarm(mContext, mAlarm, true, new AlarmCallBack() {
            @Override
            public void onExecuted(AlarmInstance instance) {
                mActivity.switchContent(CustomFragment.this, mActivity.getFragment(
                        AlarmSetOptions.class.getName(), AlarmSetOptions.TAG));
            }
        });
        return false;
    }


    class OptionsAdapter extends BaseAdapter {

        public OptionsAdapter() {
        }

        @Override
        public int getCount() {
            return mOptionsData.length;
        }

        @Override
        public String getItem(int position) {
            return mOptionsData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mActivity.getLayoutInflater().inflate(R.layout.days_item, null);
            TextView day = (TextView) view.findViewById(R.id.day);
            CheckBox cb = (CheckBox) view.findViewById(R.id.onoff);
            cb.setChecked(days[position]);
            day.setText(mOptionsData[position]);
            return view;
        }
    }
}
