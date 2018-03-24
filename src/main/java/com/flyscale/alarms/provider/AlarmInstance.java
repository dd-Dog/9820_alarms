package com.flyscale.alarms.provider;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.flyscale.alarms.R;
import com.flyscale.alarms.SettingsActivity;
import com.flyscale.alarms.utils.DLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

public class AlarmInstance implements ClockContract.InstanceColumns {
    private static final String TAG = "AlarmInstance";
    //------------------------------数据定义--------------------------------------------------
    /**
     * Offset from alarm time to show low priority notification
     */
    public static final int LOW_NOTIFICATION_HOUR_OFFSET = -2;

    /**
     * Offset from alarm time to show high priority notification
     */
    public static final int HIGH_NOTIFICATION_MINUTE_OFFSET = -30;

    /**
     * Offset from alarm time to stop showing missed notification.
     */
    private static final int MISSED_TIME_TO_LIVE_HOUR_OFFSET = 12;

    /**
     * Default timeout for alarms in minutes.
     */
    private static final String DEFAULT_ALARM_TIMEOUT_SETTING = "10";

    //闹钟实例参数
    public long mId;
    public int mYear;
    public int mMonth;
    public int mDay;
    public int mHour;
    public int mMinute;
    public String mLabel;
    public boolean mVibrate;
    public Uri mRingtone;
    public Long mAlarmId;
    public int mAlarmState;

    // for cursor to use  : ColumnIndex
    private static final int ID_INDEX = 0;
    private static final int YEAR_INDEX = 1;
    private static final int MONTH_INDEX = 2;
    private static final int DAY_INDEX = 3;
    private static final int HOUR_INDEX = 4;
    private static final int MINUTES_INDEX = 5;
    private static final int LABEL_INDEX = 6;
    private static final int VIBRATE_INDEX = 7;
    private static final int RINGTONE_INDEX = 8;
    private static final int ALARM_ID_INDEX = 9;
    private static final int ALARM_STATE_INDEX = 10;
    private static final int COLUMN_COUNT = ALARM_STATE_INDEX + 1;

    public AlarmInstance(Calendar calendar) {
        mId = INVALID_ID;
        setAlarmTime(calendar);
        mLabel = "";
        mVibrate = false;
        mRingtone = null;
        mAlarmState = SILENT_STATE;
    }

    public AlarmInstance(Calendar calendar, Long alarmId) {
        this(calendar);
        mAlarmId = alarmId;
    }

    public AlarmInstance(Cursor c) {
        mId = c.getLong(ID_INDEX);
        mYear = c.getInt(YEAR_INDEX);
        mMonth = c.getInt(MONTH_INDEX);
        mDay = c.getInt(DAY_INDEX);
        mHour = c.getInt(HOUR_INDEX);
        mMinute = c.getInt(MINUTES_INDEX);
        mLabel = c.getString(LABEL_INDEX);
        mVibrate = c.getInt(VIBRATE_INDEX) == 1;

        if (c.isNull(RINGTONE_INDEX)) {
            mRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            mRingtone = Uri.parse(c.getString(RINGTONE_INDEX));
        }

        if (!c.isNull(ALARM_ID_INDEX)) {
            mAlarmId = c.getLong(ALARM_ID_INDEX);
        }
        mAlarmState = c.getInt(ALARM_STATE_INDEX);
    }

    public void setAlarmTime(Calendar calendar) {
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    public Calendar getAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.MONTH, mMonth);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }


    public Calendar getLowNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR_OF_DAY, LOW_NOTIFICATION_HOUR_OFFSET);
        return calendar;
    }

    public Calendar getHighNotificationTime() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, HIGH_NOTIFICATION_MINUTE_OFFSET);
        return calendar;
    }

    public Calendar getMissedTimeToLive() {
        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.HOUR, MISSED_TIME_TO_LIVE_HOUR_OFFSET);
        return calendar;
    }


    /**
     * 无操作，响铃超时
     */
    public Calendar getTimeout(Context context) {
        String timeoutSetting = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_AUTO_SILENCE, DEFAULT_ALARM_TIMEOUT_SETTING);
        int timeoutMinutes = Integer.parseInt(timeoutSetting);

        //时间设置为永不
        if (timeoutMinutes < 0) {
            return null;
        }

        Calendar calendar = getAlarmTime();
        calendar.add(Calendar.MINUTE, timeoutMinutes);
        return calendar;
    }

    public String getLabelOrDefault(Context context) {
        return mLabel.isEmpty() ? context.getString(R.string.default_label) : mLabel;
    }

    //----------------------------------操作定义-----------------------------------------------
    private static final String[] QUERY_COLUMNS = {
            _ID,
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTES,
            LABEL,
            VIBRATE,
            RINGTONE,
            ALARM_ID,
            ALARM_STATE
    };

    public static ContentValues createContentValues(AlarmInstance instance) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (instance.mId != INVALID_ID) {
            values.put(_ID, instance.mId);
        }

        values.put(YEAR, instance.mYear);
        values.put(MONTH, instance.mMonth);
        values.put(DAY, instance.mDay);
        values.put(HOUR, instance.mHour);
        values.put(MINUTES, instance.mMinute);
        values.put(LABEL, instance.mLabel);
        values.put(VIBRATE, instance.mVibrate ? 1 : 0);
        if (instance.mRingtone == null) {
            // We want to put null in the database, so we'll be able
            // to pick up on changes to the default alarm
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, instance.mRingtone.toString());
        }
        values.put(ALARM_ID, instance.mAlarmId);
        values.put(ALARM_STATE, instance.mAlarmState);
        return values;
    }

    public static Uri getUri(long intanceId) {
        return ContentUris.withAppendedId(CONTENT_URI, intanceId);
    }

    public static long getId(Uri contentUri) {
        if (contentUri != null)
            return ContentUris.parseId(contentUri);
        return -1;
    }

    /**
     * 通过instanceId获取闹钟实例
     */
    public static AlarmInstance getInstanceById(ContentResolver contentResolver, long intanceId) {
        Cursor cursor = contentResolver.query(getUri(intanceId), QUERY_COLUMNS, null, null, null);
        AlarmInstance result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new AlarmInstance(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }


    /**
     * 通过alarmId获取闹钟实例
     */
    public static List<AlarmInstance> getInstanceByAlarmId(ContentResolver contentResolver, long
            alarmId) {
        return getInstances(contentResolver, ALARM_ID + "=" + alarmId);
    }

    /**
     * 指定条件获取闹钟实例
     */
    public static List<AlarmInstance> getInstances(ContentResolver contentResolver, String
			selection, String... selectionArgs) {
        Cursor cursor = contentResolver.query(CONTENT_URI, QUERY_COLUMNS, selection,
				selectionArgs, null);
        List<AlarmInstance> result = new LinkedList<AlarmInstance>();

        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new AlarmInstance(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return result;
    }


    public static AlarmInstance addInsatnce(ContentResolver contentResolver, AlarmInstance
			instance) {
        String dupSelector = AlarmInstance.ALARM_ID + "=" + instance.mAlarmId;

        //避免重复添加相同的闹钟实例；
        for (AlarmInstance otherInstance : getInstances(contentResolver, dupSelector)) {
            if (otherInstance.getAlarmTime().equals(instance.getAlarmTime())) {
                DLog.w(TAG, "检测到重复的闹钟实例 ，updating :" + otherInstance + " to " + instance);
                instance.mId = otherInstance.mId;
                updateInstance(contentResolver, instance);
                return instance;
            }
        }

        ContentValues values = createContentValues(instance);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        instance.mId = getId(uri);
        return instance;
    }

    public static boolean updateInstance(ContentResolver contentResolver, AlarmInstance instance) {
        if (instance.mId == INVALID_ID) return false;
        ContentValues values = createContentValues(instance);
        long rowUpdated = contentResolver.update(getUri(instance.mId), values, null, null);
        return rowUpdated == 1;
    }

    public static boolean deleteInstance(ContentResolver contentResolver, AlarmInstance instance) {
        if (instance.mId == INVALID_ID) return false;
        long deleteRows = contentResolver.delete(getUri(instance.mId), null, null);
        return deleteRows == 1;
    }

    public static Intent createIntent(String action, long instanceId) {
        return new Intent(action).setData(getUri(instanceId));
    }

    public static Intent createIntent(Context context, Class<?> cls, long intanceId) {
        return new Intent(context, cls).setData(getUri(intanceId));
    }

    //---------------------------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlarmInstance)) return false;
        final AlarmInstance other = (AlarmInstance) o;
        return mId == other.mId;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(mId).hashCode();
    }


    public String toString() {
        return "AlarmInstance{" +
                "mId=" + mId +
                ", mYear=" + mYear +
                ", mMonth=" + mMonth +
                ", mDay=" + mDay +
                ", mHour=" + mHour +
                ", mMinute=" + mMinute +
                ", mLabel=" + mLabel +
                ", mVibrate=" + mVibrate +
                ", mRingtone=" + mRingtone +
                ", mAlarmId=" + mAlarmId +
                ", mAlarmState=" + mAlarmState +
                "}";
    }


}
