package com.flyscale.alarms.options;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flyscale.alarms.AlarmCallBack;
import com.flyscale.alarms.AlarmListFragment;
import com.flyscale.alarms.BaseFragment;
import com.flyscale.alarms.MainActivity;
import com.flyscale.alarms.R;
import com.flyscale.alarms.SetAlarmTimeFragment;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.provider.DaysOfWeek;
import com.flyscale.alarms.utils.AlarmUtils;
import com.flyscale.alarms.widget.TextTime;


/**
 * Created by MrBian on 2018/1/11.
 */

public class AlarmSetOptions extends BaseFragment {

    public static final String TAG = "AlarmSetOptions";
    public static final int EDIT = 1001;
    public static final int NEW_ALARM = 1002;
    private static int mAction;
    private ListView mOptions;
    private String[] mOptionsData;
    private String[] mValues;
    private Alarm mAlarm;
    private OptionsAdapter optionsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        initData();
        return initView();
    }

    private void initData() {
        mOptionsData = getResources().getStringArray(R.array.alarm_set_options);
        mValues = new String[mOptionsData.length];
        setAlarm(MainActivity.mAlarm);
    }

    private View initView() {
        View view = mActivity.getLayoutInflater().inflate(R.layout.fragment_larmlist_options,
                null);
        mOptions = (ListView) view.findViewById(R.id.main);
        TextView title = (TextView)view.findViewById(R.id.title);
        title.setText(getResources().getString(R.string.alarm_settings));
        mOptions.setSelection(0);
        mOptions.setDivider(null);
        optionsAdapter = new OptionsAdapter();
        mOptions.setAdapter(optionsAdapter);
        mOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleOption(position);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //进入界面时listview可能没有焦点，导致字体颜色和背景不对应,要主动获取焦点
        mOptions.requestFocus();
        mActivity.setCurrentFragment(this, TAG);
        refresh();
    }

    private void handleOption(int position) {
        switch (position) {
            case 0:
                mActivity.switchContent(this, mActivity.getFragment(
                        SetAlarmTimeFragment.class.getName(), SetAlarmTimeFragment.TAG));
                break;
            case 1:
                mAlarm.enabled = !mAlarm.enabled;
                MainActivity.mAlarm = mAlarm;
                AlarmUtils.asyncUpdateAlarm(mContext, mAlarm, true, new AlarmCallBack() {
                    @Override
                    public void onExecuted(AlarmInstance instance) {
                        refresh();
                    }
                });
                break;
            case 2:
                mActivity.switchContent(this, mActivity.getFragment(
                        SelectRepeatModeFragment.class.getName(), SelectRepeatModeFragment.TAG));
                break;
            case 3:
                break;

        }
    }

    private void refresh() {
        mAlarm = MainActivity.mAlarm;
        setAlarm(mAlarm);
        optionsAdapter.notifyDataSetChanged();
    }

    public boolean onKeyUp(int keyCode) {
        Log.i(TAG, "onKeyUp::keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                int position = mOptions.getSelectedItemPosition();
                handleOption(position);
                break;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        mActivity.switchContent(this, mActivity.getFragment(
                AlarmListFragment.class.getName(), AlarmListFragment.TAG));
        return true;
    }

    public void setAlarm(Alarm alarm) {
        Log.i(TAG, "setAlarm,alarm=" + alarm);
        mAlarm = alarm;
        if (mAlarm != null) {
//            mValues[0] = mAlarm.hour + "";
            mValues[1] = mAlarm.enabled ? getResources().getString(R.string.open) :
                    getResources().getString(R.string.close);
            int mode = mAlarm.daysOfWeek.getRepeatMode();
            switch (mode) {
                case DaysOfWeek.ONCE:
                    mValues[2] = mActivity.getResources().getString(R.string.once);
                    break;
                case DaysOfWeek.EVERY_DAY:
                    mValues[2] = mActivity.getResources().getString(R.string.eveday);
                    break;
                case DaysOfWeek.WORK_DAY:
                    mValues[2] = mActivity.getResources().getString(R.string.workday);
                    break;
                case DaysOfWeek.CUSTOM:
                    mValues[2] = mActivity.getResources().getString(R.string.custom);
                    break;
            }
            mValues[3] = TextUtils.isEmpty(mAlarm.label) ?
                    getResources().getString(R.string.default_label) : mAlarm.label;
        }
    }

    public static void setAction(int action) {
        mAction = action;
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
            View view = mActivity.getLayoutInflater().inflate(position != 0 ? R.layout.set_item :
                    R.layout.set_item_time, null);
            TextView key = (TextView) view.findViewById(R.id.key);
            key.setText(mOptionsData[position]);
            if (position != 0) {
                TextView value = (TextView) view.findViewById(R.id.value);
                value.setText(mValues[position]);
            } else {
                TextTime time = (TextTime) view.findViewById(R.id.value);
                time.setTime(mAlarm.hour, mAlarm.minutes);
                time.setFormat((int) mActivity.getResources().getDimension(R.dimen.textsize));
            }
            return view;
        }
    }
}
