package com.flyscale.alarms;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flyscale.alarms.alarms.AlarmStateManager;
import com.flyscale.alarms.options.AlarmListOptions;
import com.flyscale.alarms.options.AlarmSetOptions;
import com.flyscale.alarms.options.SelectRepeatModeFragment;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.provider.DaysOfWeek;
import com.flyscale.alarms.utils.AlarmUtils;
import com.flyscale.alarms.utils.DLog;
import com.flyscale.alarms.widget.TextTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

/**
 * Created by Administrator on 2018/3/22 0022.
 */


public class AlarmListFragment extends BaseFragment implements
        LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = "AlarmListFragment";
    private boolean isAlarmListEmpty = true;
    private static Alarm mSelectedAlarm;
    private ListView mAlarmsList;
    private AlarmItemAdapter mAdapter;
    private View mEmptyView;

    private static final String KEY_DEFAULT_RINGTONE = "default_ringtone";

    public AlarmListFragment() {

    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getLoaderManager().initLoader(0, null, this);
        if (TextUtils.isEmpty(getDeafaultRingtone(getActivity()))) {
            setSystemAlarmRingtoneToPref();
        }

        getActivity().setVolumeControlStream(AudioManager.STREAM_ALARM);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle icicle) {
        DLog.i(TAG, "onCreateView()");
        View v = inflater.inflate(R.layout.alarm_fragment, container, false);
        TextView confirm = (TextView) v.findViewById(R.id.confirm);
        confirm.setText(getResources().getString(R.string.options));
        TextView title = (TextView) v.findViewById(R.id.title);
        title.setText(getResources().getString(R.string.app_name));

        //无闹钟
        mEmptyView = v.findViewById(R.id.alarms_empty_view);

        //闹钟列表
        mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);
        mAlarmsList.setOnCreateContextMenuListener(this);

        mAdapter = new AlarmItemAdapter(getActivity(),
                mAlarmsList);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                final int count = mAdapter.getCount();

                if (count == 0) {
                    mEmptyView.setVisibility(View.VISIBLE);
                    mAlarmsList.setVisibility(View.GONE);
                    isAlarmListEmpty = true;
                } else {
                    mEmptyView.setVisibility(View.GONE);
                    mAlarmsList.setVisibility(View.VISIBLE);
                    isAlarmListEmpty = false;
                }

                super.onChanged();
            }
        });

        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(false);
        mAlarmsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, "onItemSelected=" + i);
                AlarmItemAdapter.ItemHolder itemHolder = (AlarmItemAdapter.ItemHolder) view
                        .getTag();
                mSelectedAlarm = itemHolder.alarm;
                mActivity.mAlarm = itemHolder.alarm;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //test
//        Alarm alarm = new Alarm(15, 23);
//        alarm.enabled = true;
//        AlarmUtils.asyncAddAlarm(mContext, alarm);
//        alarm.hour = 15;
//        alarm.minutes = 31;
//        alarm.enabled = true;
//        AlarmUtils.asyncUpdateAlarm(mContext, mSelectedAlarm, true, null);
        return v;
    }

    public static void asyncAddAlarm(final Context context, final Alarm alarm) {
        final AsyncTask<Void, Void, AlarmInstance> addTask = new AsyncTask<Void, Void,
                AlarmInstance>() {

            @Override
            public synchronized void onPreExecute() {

            }

            @Override
            protected AlarmInstance doInBackground(Void... params) {
                if (context != null && alarm != null) {
                    ContentResolver reslover = context.getContentResolver();
                    Alarm newAlarm = Alarm.addAlarm(reslover, alarm);
                    if (newAlarm.enabled) {
                        return setupAlarmInstance(context, newAlarm);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }

        };

        addTask.execute();
    }

    public void onResume() {
        super.onResume();
        DLog.i(TAG, "onResume()");
        mActivity.setCurrentFragment(this, TAG);
        //进入界面时listview可能没有焦点，导致字体颜色和背景不对应,而且要第一次按键看起来没有反应，要主动获取焦点
        mAlarmsList.requestFocus();
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver resolver = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInsatnce(resolver, newInstance);
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }


    /**
     * 实例化并返回loader width givern id
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    /**
     * 此处做数据更新处理
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        DLog.i(TAG, "onLoadFinished : " + "count " + data.getCount());
        mAdapter.swapCursor(data);
    }

    /**
     * loader被重置，包含数据已不可用
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

//    /**
//     * 创建新闹钟闹钟
//     */
//    private void startCreatingAlarm() {
//        Log.d(TAG, "startCreatingAlarm  start");
//        mSelectedAlarm = null;
//        AlarmUtils.showTimeEditDialog(getChildFragmentManager(), null, this, DateFormat
//                .is24HourFormat(getActivity()));
//    }

//    /**
//     * TimePickerDialog.OnTimeSetListener 接口方法
//     */
//    @Override
//    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
//        DLog.i(TAG, "onTimeSet(): hourOfDay=" + hourOfDay + ", minute=" + minute);
//        if (mSelectedAlarm == null) {
//            Context context = getActivity();
//            ContentResolver resolver = context.getContentResolver();
//            for (Alarm a : Alarm.getAlarms(resolver, null)) {
//                if (a.hour == hourOfDay && a.minutes == minute) {
//                    Toast toast = Toast.makeText(context, context.getString(R.string
//                            .same_alarm_exit), Toast.LENGTH_LONG);
//                    ToastMaster.setToast(toast);
//                    toast.show();
//                    return;
//                }
//            }
//
//            Alarm a = new Alarm(hourOfDay, minute);
//            //使用程序默认闹钟铃声
//            String defaultRingtone = getDeafaultRingtone(getActivity());
//            if (AlarmUtils.isRingtoneExisted(getActivity(), defaultRingtone)) {
//                a.alert = Uri.parse(defaultRingtone);
//            } else {
//                //使用默认闹钟铃声
//                a.alert = getSystemAlarmRingtoneUri(getActivity());
//            }
//            a.enabled = true;
//            AlarmUtils.asyncAddAlarm(mContext, a);
//        } else {
//            mSelectedAlarm.hour = hourOfDay;
//            mSelectedAlarm.minutes = minute;
//            mSelectedAlarm.enabled = true;
//            AlarmUtils.asyncUpdateAlarm(mContext, mSelectedAlarm, true, null);
//            mSelectedAlarm = null;
//        }
//    }

    /**
     * 闹钟标签编辑窗口
     */
    private void showLabelDialog(final Alarm alarm) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(MainActivity
                .TAG_LABEL_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }

        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, getTag());
        ft.add(newFragment, MainActivity.TAG_LABEL_FRAGMENT);
        ft.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    /**
     * 设置闹钟标签
     */
    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        AlarmUtils.asyncUpdateAlarm(mContext, alarm, false, null);
    }

    /**
     * M : 获取程序默认闹钟铃声
     */
    private String getDeafaultRingtone(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultRingtone = prefs.getString(KEY_DEFAULT_RINGTONE, "");
        DLog.d(TAG, "getDeafaultRingtone : " + defaultRingtone);
        return defaultRingtone;
    }

    //系统默认闹钟铃声
    private static final String SYSTEM_SETTINGS_ALARM_ALERT =
            "content://settings/system/alarm_alert";

    /**
     * M : 获取系统默认闹钟铃声
     */
    private Uri getSystemAlarmRingtoneUri(Context context) {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = Uri.parse(SYSTEM_SETTINGS_ALARM_ALERT);
        }
        return uri;
    }

    /**
     * 设置程序默认闹钟铃声
     *
     * @param defaultRingtone
     */
    private void setDefaultRingtone(String defaultRingtone) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DEFAULT_RINGTONE, defaultRingtone);
        editor.apply();
    }

    /**
     * 没有设置默认铃声时，将系统默认铃声设为程序默认铃声并写入pre文件保存；
     */
    private void setSystemAlarmRingtoneToPref() {
        Uri uri = getSystemAlarmRingtoneUri(getActivity());
        setDefaultRingtone(uri.toString());
        DLog.d(TAG, "setSystemAlarmRingtoneToPref : " + uri.toString());
    }


    private static final int REQUEST_CODE_RINGTONE = 0x0001;

    private void launchRingTonePicker(Alarm alarm) {
        mSelectedAlarm = alarm;
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resutlCode, Intent data) {
        if (resutlCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_RINGTONE) {
                saveRingtoneUri(data);
            }
        }
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }

        if (null == mSelectedAlarm) {
            DLog.w(TAG, "saveRingtoneUri()  the alarm to change ringtone is null");
            return;
        }
        mSelectedAlarm.alert = uri;

        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
            setDefaultRingtone(uri.toString());//设置为程序默认闹钟铃声
            DLog.d(TAG, "saveRingtoneUri = " + uri.toString());
        }
        AlarmUtils.asyncUpdateAlarm(mContext, mSelectedAlarm, false, null);
    }


    @Override
    public boolean onKeyUp(int keyCode) {
        Log.i(TAG, "onKeyUp::keyCode=" + keyCode);
        int position = mAlarmsList.getSelectedItemPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (isAlarmListEmpty) {
                    startNewAlarm();
                } else {
                    mActivity.switchContent(this, mActivity.getFragment(
                            AlarmListOptions.class.getName(), AlarmListOptions.TAG));
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (isAlarmListEmpty) {
                    startNewAlarm();
                } else {
                    AlarmSetOptions.setAction(AlarmSetOptions.EDIT);
                    mActivity.switchContent(this, mActivity.getFragment(
                            AlarmSetOptions.class.getName(), AlarmSetOptions.TAG));
                }
                return true;
        }
        return false;
    }

    /**
     * 新建闹钟
     */
    private void startNewAlarm() {
        MainActivity.mAlarm = new Alarm();
        AlarmUtils.asyncAddAlarm(mContext, MainActivity.mAlarm);
        AlarmSetOptions.setAction(AlarmSetOptions.NEW_ALARM);
        mActivity.switchContent(this, mActivity.getFragment(
                AlarmSetOptions.class.getName(), AlarmSetOptions.TAG));
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    class AlarmItemAdapter extends CursorAdapter {
        private final Context mContext;
        private final ListView mList;
        private final LayoutInflater mFactory;
        private final String[] mShortWeekDayStrings;
        private final String[] mLongWeekDayStrings;
        private final int mColorLit;
        private final int mColorDim;

        private final boolean mHasVibrator;

        private final HashSet<Long> mExpanded = new HashSet<Long>();//expand items


        public AlarmItemAdapter(Context context, ListView list) {
            super(context, null, 0);
            mContext = context;
            mList = list;
            mFactory = LayoutInflater.from(context);

            DateFormatSymbols dfs = new DateFormatSymbols();
            mShortWeekDayStrings = dfs.getShortWeekdays();
            mLongWeekDayStrings = dfs.getWeekdays();

            Resources res = mContext.getResources();
            mColorLit = res.getColor(R.color.clock_white);
            mColorDim = res.getColor(R.color.clock_gray);

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = null;
            if (!getCursor().moveToPosition(position)) {
                DLog.d(TAG, "couldn't move cursor to position " + position);
                return null;
            }

            if (convertView == null) {
                v = newView(mContext, getCursor(), parent);
            } else {
                //防止convertview内容不全
                boolean badConvertView = convertView.findViewById(R.id.digital_clock) == null;
                // Do a translation check to test for animation. Change this to something more
                // reliable and robust in the future.
                if (convertView.getTranslationX() != 0 || convertView.getTranslationY() != 0 ||
                        badConvertView) {
                    convertView = null;
                    v = newView(mContext, getCursor(), parent);
                } else {
                    v = convertView;
                }
            }

            bindView(v, mContext, getCursor());
            AlarmItemAdapter.ItemHolder holder = (AlarmItemAdapter.ItemHolder) v.getTag();
            //防止最后一条闹钟被下方按钮区域遮挡
//            holder.footerFiller.setVisibility(position < getCount() - 1 ? View.GONE : View
// .VISIBLE);
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.alarm_time_item2, parent, false);
            setNewHolder(view);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                setNewHolder(view);
            }

            final AlarmItemAdapter.ItemHolder itemHolder = (AlarmItemAdapter.ItemHolder) tag;
            itemHolder.alarm = alarm;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong alarm.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);
            itemHolder.onoff.setEnabled(true);
            itemHolder.onoff.setOnCheckedChangeListener(new CompoundButton
                    .OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    Log.i(TAG, "onCheckedChanged=" + isChecked);
                    // 关闭开启闹钟
                    if (isChecked != alarm.enabled) {
//                        setItemAlpha(itemHolder, isChecked);//设置文字透明度
                        alarm.enabled = isChecked;
                        AlarmUtils.asyncUpdateAlarm(mContext, alarm, alarm.enabled, null);
                    }
                }
            });

//            setItemAlpha(itemHolder, itemHolder.onoff.isChecked());//设置文字透明度

            itemHolder.clock.setFormat((int) mContext.getResources().getDimension(R.dimen
                    .textsize));
            itemHolder.clock.setTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                //修改闹钟时间
                public void onClick(View v) {
                    Log.i(TAG, "onClick");
                    mSelectedAlarm = itemHolder.alarm;
                    //编辑闹钟
//                    AlarmUtils.showTimeEditDialog(getChildFragmentManager(), mSelectedAlarm,
//                            AlarmListFragment.this,
//                            DateFormat.is24HourFormat(getActivity()));
                    //使这个itemview在屏幕上显示完全
                    itemHolder.alarmItem.post(mScrollRunnable);
                }
            });


            boolean expanded = isAlarmExpanded(itemHolder.alarm);
//            itemHolder.expandArea.setVisibility(expanded ? View.VISIBLE : View.GONE);
//            itemHolder.summary.setVisibility(expanded ? View.GONE : View.VISIBLE);

            final String daysOfWeekStr = alarm.daysOfWeek.toString(getActivity(), false);
           /* if (!TextUtils.isEmpty(daysOfWeekStr)) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        expandAlarm(itemHolder, false);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });
            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }*/

        }


        private void bindExpandArea(final AlarmItemAdapter
                .ItemHolder itemHolder, final Alarm alarm) {
           /* if (!TextUtils.isEmpty(alarm.label)) {
                itemHolder.clickableLabel.setText(alarm.label);
                itemHolder.clickableLabel.setTextColor(mColorLit);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
                itemHolder.clickableLabel.setTextColor(mColorDim);
            }

            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    showLabelDialog(alarm);
                }
            });

            if (alarm.daysOfWeek.isRepeating()) {
                itemHolder.repeat.setChecked(true);
                itemHolder.repeatDays.setVisibility(View.VISIBLE);
            } else {
                itemHolder.repeat.setChecked(false);
                itemHolder.repeatDays.setVisibility(View.GONE);
            }*/
           /* itemHolder.repeat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean checked = ((CheckBox) view).isChecked();
                    if (checked) {
                        itemHolder.repeatDays.setVisibility(View.VISIBLE);
                        if (!alarm.daysOfWeek.isRepeating()) {
                            alarm.daysOfWeek.setDaysOfWeek(true, DAY_ORDER);
                        }
                        updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
                    } else {
                        itemHolder.repeatDays.setVisibility(View.GONE);
                        alarm.daysOfWeek.clearAllDays();
                    }
                    asyncUpdateAlarm(alarm, false);
                }
            });*/
            updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;
//                itemHolder.dayButtonParents.clone()[i].setOnClickListener(new View
//                        .OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        itemHolder.dayButtons[buttonIndex].toggle();
//                        final boolean checked = itemHolder.dayButtons[buttonIndex].isChecked();
//                        int day = DAY_ORDER[buttonIndex];
//                        alarm.daysOfWeek.setDaysOfWeek(checked, day);
//                        if (checked) {
//                            turnOffDayOfWeek(itemHolder, buttonIndex);
//                        } else {
//                            turnOffDayOfWeek(itemHolder, buttonIndex);
//                            if (!alarm.daysOfWeek.isRepeating()) {
//                                itemHolder.repeat.setChecked(false);
//                                itemHolder.repeatDays.setVisibility(View.GONE);
//                            }
//                        }
//                        asyncUpdateAlarm(alarm, false);
//                    }
//                });
            }

            if (!mHasVibrator) {
//                itemHolder.vibrate.setVisibility(View.GONE);
                alarm.vibrate = false;
            } else {
//                itemHolder.vibrate.setVisibility(View.VISIBLE);
               /* if (!alarm.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                    itemHolder.vibrate.setTextColor(mColorDim);
                } else {
                    itemHolder.vibrate.setChecked(true);
                    itemHolder.vibrate.setTextColor(mColorLit);
                }*/
            }

           /* itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean checked = ((CheckBox) v).isChecked();
                    if (checked) {
                        itemHolder.vibrate.setTextColor(mColorLit);
                    } else {
                        itemHolder.vibrate.setTextColor(mColorDim);
                    }
                    alarm.vibrate = checked;
                    asyncUpdateAlarm(alarm, false);
                }
            });*/

            final String ringtoneTitle;
            if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
                ringtoneTitle = mContext.getResources().getString(R.string.silent_alarm_summary);
            } else {
                if (!AlarmUtils.isRingtoneExisted(mContext, alarm.alert.toString())) {
                    alarm.alert = getSystemAlarmRingtoneUri(mContext);
                }
                ringtoneTitle = getRingToneTitle(alarm.alert);
            }
           /* itemHolder.ringtone.setText(ringtoneTitle);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchRingTonePicker(alarm);
                }
            });*/
        }


        /**
         * 设置文字透明度
         *
         * @param holder
         * @param enabled
         */
        private void setItemAlpha(AlarmItemAdapter.ItemHolder
                                          holder, boolean enabled) {
            final float alpha = enabled ? 1f : 0.5f;
            holder.clock.setAlpha(alpha);
        }

        private void updateDaysOfWeekButtons(AlarmItemAdapter
                                                     .ItemHolder holder, DaysOfWeek daysOfWeek) {
            HashSet<Integer> setDays = daysOfWeek.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(DAY_ORDER[i])) {
                    turnOnDayOfWeek(holder, i);
                } else {
                    turnOffDayOfWeek(holder, i);
                }
            }
        }

        private void turnOffDayOfWeek(AlarmItemAdapter
                                              .ItemHolder holder, int dayIndex) {
//            holder.dayButtons[dayIndex].setChecked(false);
//            holder.dayButtons[dayIndex].setTextColor(mColorDim);
        }

        private void turnOnDayOfWeek(AlarmItemAdapter
                                             .ItemHolder holder, int dayIndex) {
//            holder.dayButtons[dayIndex].setChecked(true);
//            holder.dayButtons[dayIndex].setTextColor(mColorLit);
        }

        private String getRingToneTitle(Uri uri) {
            Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
            String title = ringTone.getTitle(mContext);
            return title;
        }

        private void setNewHolder(View view) {
            final AlarmItemAdapter.ItemHolder holder = new AlarmItemAdapter.ItemHolder();
            holder.alarmItem = (LinearLayout) view.findViewById(R.id.alarm_item);
            holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
            holder.onoff = (CheckBox) view.findViewById(R.id.onoff);
//            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
//            holder.delete = (ImageView) view.findViewById(R.id.alarm_delete);
//            holder.summary = view.findViewById(R.id.summary);
//            holder.expandArea = view.findViewById(R.id.expand_area);
//            holder.hairLine = view.findViewById(R.id.hairline);
//            holder.arrow = (ImageView) view.findViewById(R.id.arrow);
//            holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
//            holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
//            holder.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);
//            holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);
//            holder.footerFiller = view.findViewById(R.id.alarm_footer_filler);
//            holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
//            holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);

            // Build button for each day.
          /*  for (int i = 0; i < 7; i++) {
                final ViewGroup viewgroup = (ViewGroup) mFactory.inflate(R.layout.day_button,
                        holder.repeatDays, false);
                final ToggleButton button = (ToggleButton) viewgroup.getChildAt(0);
                final int dayToShowIndex = DAY_ORDER[i];
                button.setText(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOn(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOff(mShortWeekDayStrings[dayToShowIndex]);
                button.setContentDescription(mLongWeekDayStrings[dayToShowIndex]);
                holder.repeatDays.addView(viewgroup);
                holder.dayButtons[i] = button;
                holder.dayButtonParents[i] = viewgroup;
            }*/

            view.setTag(holder);
        }

        private final int[] DAY_ORDER = new int[]{
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
        };

        public class ItemHolder {
            LinearLayout alarmItem;
            TextTime clock;
            CheckBox onoff;
//            TextView daysOfWeek;
//            ImageView delete;
//            View expandArea;
//            View summary;
//            TextView clickableLabel;
//            CheckBox repeat;
//            LinearLayout repeatDays;
//            ViewGroup[] dayButtonParents = new ViewGroup[7];
//            ToggleButton[] dayButtons = new ToggleButton[7];
//            CheckBox vibrate;
//            TextView ringtone;
//            View hairLine;
//            ImageView arrow;
//            View collapseExpandArea;
//            View footerFiller;

            Alarm alarm;
        }

        /**
         * 除了基类方法中更新数据意外，负责listview的绘制，以及新增删除闹钟的动画控制；
         */
        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            Cursor c = super.swapCursor(cursor);
            return c;
        }

        private boolean isAlarmExpanded(Alarm alarm) {
            return mExpanded.contains(alarm.id);
        }

        //滑动条目，使其完全显示在屏幕上
        private long mScrollAlarmId = -1;
        private final Runnable mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScrollAlarmId != -1) {
                    View v = getViewById(mScrollAlarmId);
                    if (v != null) {
                        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getRight());
                        mList.requestChildRectangleOnScreen(v, rect, false);
                    }
                    mScrollAlarmId = -1;
                }
            }

        };

        private View getViewById(long id) {
            for (int i = 0; i < mList.getCount(); i++) {
                View v = mList.getChildAt(i);
                if (v != null) {
                    AlarmItemAdapter.ItemHolder h = (AlarmItemAdapter.ItemHolder) (v.getTag());
                    if (h != null && h.alarm.id == id) {
                        return v;
                    }
                }
            }
            return null;
        }
    }

}

