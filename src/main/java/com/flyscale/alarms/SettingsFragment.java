package com.flyscale.alarms;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flyscale.alarms.options.AlarmSetOptions;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.utils.AlarmUtils;


/**
 * Created by MrBian on 2018/1/11.
 */

public class SettingsFragment extends BaseFragment {

    public static final String TAG = "SettingsFragment";
    private ListView mOptions;
    private String[] mOptionsData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initView();
    }

    private void initData() {
        mOptionsData = getResources().getStringArray(R.array.alarmoptions);
    }

    private View initView() {
        View view = mActivity.getLayoutInflater().inflate(R.layout.fragment_larmlist_options,
                null);
        mOptions = (ListView) view.findViewById(R.id.main);
        TextView title = (TextView)view.findViewById(R.id.main);
        title.setText(getResources().getString(R.string.settings));
        mOptions.setSelection(0);
        mOptions.setDivider(null);
        OptionsAdapter optionsAdapter = new OptionsAdapter();
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
    }

    private void handleOption(int position) {
        switch (position) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    }

    public boolean onKeyUp(int keyCode) {
        Log.i(TAG, "onKeyUp::keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_MENU:
                int position = mOptions.getSelectedItemPosition();
                handleOption(position);
                break;
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
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
            TextView item = (TextView) mActivity.getLayoutInflater().inflate(R.layout.item, null);
            item.setText(mOptionsData[position]);
            return item;
        }
    }
}
