package com.flyscale.alarms.utils;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.flyscale.alarms.AlarmCallBack;
import com.flyscale.alarms.R;
import com.flyscale.alarms.ToastMaster;
import com.flyscale.alarms.alarms.AlarmStateManager;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;


public class AlarmUtils {
    public static final String FRAG_TAG_TIME_PICKER = "time_dialog";
    public static final String TAG = "AlarmUtils";

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

    /**
     * 更新闹钟
     *
     * @param alarm    更新闹钟内容
     * @param popToast 是否弹出闹钟提示框
     */
    public static void asyncUpdateAlarm(final Context context, final Alarm alarm, final boolean
            popToast, final AlarmCallBack callBack) {
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {

                    @Override
                    protected AlarmInstance doInBackground(Void... params) {
                        ContentResolver cr = context.getContentResolver();
                        AlarmStateManager.deleteAllInstances(context, alarm.id);
                        Alarm.updateAlarm(cr, alarm);

                        if (alarm.enabled) {
                            return setupAlarmInstance(context, alarm);
                        }
                        return null;
                    }

                    protected void onPostExecute(AlarmInstance instance) {
                        if (callBack != null) {
                            callBack.onExecuted(instance);
                        }
                    }

                };

        updateTask.execute();
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver resolver = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInsatnce(resolver, newInstance);
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    public static void asyncDeleteAlarm(final Context context, final Alarm alarm,  final AlarmCallBack callBack) {
        Log.i(TAG, "asyncDeleteAlarm,id=" + alarm.id);
        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {

            public synchronized void onPreExecute() {
                if (callBack != null) {
                    callBack.onExecuted(null);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (context != null && alarm != null) {
                    ContentResolver resolver = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, alarm.id);
                    Alarm.deleteAlarm(resolver, alarm.id);
                }
                return null;
            }
        };
        deleteTask.execute();
    }

//    public static void showTimeEditDialog(FragmentManager manager, final Alarm alarm,
//                                          TimePickerDialog.OnTimeSetListener listener, boolean
//                                                  is24HourMode) {
//
//        int hour, minutes;
//        if (alarm == null) {
//            hour = 0;
//            minutes = 0;
//        } else {
//            hour = alarm.hour;
//            minutes = alarm.minutes;
//        }
//
//        TimePickerDialog dialog = TimePickerDialog.newInstance(listener, hour, minutes,
//                is24HourMode);
//        dialog.setThemeDark(true);//时间选择框 主题风格选择
//
//        manager.executePendingTransactions();// 确保没有被添加过
//        final FragmentTransaction ft = manager.beginTransaction();
//
//        if (dialog != null && !dialog.isAdded()) {
//            ft.add(dialog, FRAG_TAG_TIME_PICKER);
//            /// M:Don't need use the method ft.commit(), because it may cause IllegalStateException
//            ft.commitAllowingStateLoss();
//        }
//    }

    public static String getFormattedTime(Context context, Calendar time) {
        String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    public static String getALarmText(Context context, AlarmInstance instance) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return !TextUtils.isEmpty(instance.mLabel) ? alarmTimeStr + " - " + instance.mLabel :
                alarmTimeStr;
    }

    public static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);//保证同一时间只有一个toast显示
        toast.show();
    }

    private static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                        context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                        context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                        context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                (dispHour ? 2 : 0) |
                (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static boolean isRingtoneExisted(Context context, String ringtone) {
        boolean result = false;
        if (!TextUtils.isEmpty(ringtone)) {
            if (ringtone.contains("internal")) {
                return true;
            }
            String path = getRingtonePath(context, ringtone);
            if (!TextUtils.isEmpty(path)) {
                result = new File(path).exists();
            }
        }
        return result;
    }

    private static String getRingtonePath(final Context mContext, final String alrmRingtone) {
        String filepath = null;
        final ContentResolver resolver = mContext.getContentResolver();

        if (!TextUtils.isEmpty(alrmRingtone)) {
            Cursor c = null;
            try {
                c = resolver.query(Uri.parse(alrmRingtone), null, null, null, null);
                if (c != null && c.moveToFirst()) {
                    filepath = c.getColumnName(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }

        }
        return filepath;
    }

}
