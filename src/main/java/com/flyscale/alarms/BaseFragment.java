package com.flyscale.alarms;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import com.flyscale.alarms.provider.Alarm;

/**
 * Created by Administrator on 2018/3/22 0022.
 */

public abstract class BaseFragment extends Fragment {

    public MainActivity mActivity;
    public Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        mContext = getActivity().getApplicationContext();
    }

    public abstract boolean onKeyUp(int keyCode);
    public abstract boolean onBackPressed();
}
