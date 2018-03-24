package com.flyscale.alarms;

import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.utils.DLog;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity implements LabelDialogFragment.AlarmLabelDialogHandler {
    private static final String TAG = "MainActivity";
    public static final String TAG_ALARM_FRAGMENT = "alarm_fragment";
    public static final String TAG_LABEL_FRAGMENT = "label_fragment";
    private BaseFragment mCurrentFragment;
    public static Alarm mAlarm;
    public HashMap<String, BaseFragment> mFragments;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFragments = new HashMap<String, BaseFragment>();
        mFragments.put(AlarmListFragment.TAG, new AlarmListFragment());
        if (savedInstanceState == null) {
            BaseFragment fragment = getFragment(AlarmListFragment.class.getSimpleName(),AlarmListFragment.TAG);
            getFragmentManager().beginTransaction().add(R.id.container,fragment,
                    AlarmListFragment.TAG).commit();
            mCurrentFragment = fragment;
        }
    }

    /**
     * 从Map获取fragment对象，如果没有则创建
     * @param clazz
     * @param tag
     * @return
     */
    public BaseFragment getFragment(String clazz, String tag)  {
        BaseFragment baseFragment = mFragments.get(tag);
        if (baseFragment == null) {
            try {
                baseFragment = (BaseFragment) Class.forName(clazz).newInstance();
                mFragments.put(tag, baseFragment);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mFragments.put(tag, baseFragment);
        }
        return baseFragment;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp::keyCode=" + keyCode);
        if (mCurrentFragment != null) {
            if (mCurrentFragment.onKeyUp(keyCode)) {
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void switchFragment(BaseFragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mCurrentFragment = fragment;
    }

    public void switchFragmentNoToBackStatck(BaseFragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment, tag);
        fragmentTransaction.commit();
        mCurrentFragment = fragment;
    }

    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        DLog.i(TAG, "onDialogLabelSet() " + "label=" + label + ", tag=" + tag);
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmListFragment) {
            ((AlarmListFragment) frag).setLabel(alarm, label);
        }
    }

    public void setCurrentFragment(BaseFragment fragment, String tag) {
        mCurrentFragment = fragment;
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        if (mCurrentFragment != null) {
            if (mCurrentFragment.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }


    public void remove(BaseFragment fragment) {
        Log.i(TAG, "remove::fragment=" + fragment.getClass().getSimpleName());
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commit();
    }

    public void switchContent(BaseFragment from, BaseFragment to) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (!to.isAdded()) {    // 先判断是否被add过
            transaction.hide(from).add(R.id.container, to).commit();
        } else {
            transaction.hide(from).show(to).commit();
        }
        setCurrentFragment(to, null);
    }

}
