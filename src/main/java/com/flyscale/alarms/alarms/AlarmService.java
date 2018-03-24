package com.flyscale.alarms.alarms;

import com.flyscale.alarms.AlarmAlertWakeLock;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.DLog;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;


/**
 * 管理闹钟的开启，关闭；
 * 控制启动  AlarmActivity ,AlarmKlaxon
 *
 * @author Administrator
 */
public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    //send by AlarmService  手机关机时，关闭闹钟
    public static final String PRE_SHUT_DOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    //send by AlarmService 当闹钟停止或者小睡时触发  （系统层action,必须发送，否则可能引起其他问题）
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
    //send by AlarmService  当闹钟响起时触发    （系统层action,必须发送，否则可能引起其他问题）
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    public static final String START_ALARM_ACTION = "com.hs.myClock.START_ALARM";
    public static final String STOP_ALARM_ACTION = "com.hs.myClock.STOP_ALARM";

    private Context mContext = null;
    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;

    private AlarmInstance mCurrentAlarm = null;//当前闹钟

    private final BroadcastReceiver mStopPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCurrentAlarm == null) {
                return;
            }

            AlarmStateManager.setDismissState(context, mCurrentAlarm);

        }
    };

    public static void startAlarm(Context context, AlarmInstance instance) {
        DLog.i(TAG, "startAlarm()");
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(START_ALARM_ACTION);

        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    public static void stopAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction(STOP_ALARM_ACTION);

        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long instanceId = AlarmInstance.getId(intent.getData());
        DLog.i(TAG, "onStartCommond()  instanceId=" + instanceId);
        String action = intent.getAction();
        AlarmInstance mInstance = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(PRE_SHUT_DOWN);
        registerReceiver(mStopPlayReceiver, filter);

        if (action.equals(START_ALARM_ACTION)) {
            DLog.d(TAG, "onStartCommond()  start alarm");
            ContentResolver resolver = this.getContentResolver();
            mInstance = AlarmInstance.getInstanceById(resolver, instanceId);

            if (mInstance == null) {
                DLog.d(TAG, "onStartCommond()  mInstance==null");
                AlarmAlertWakeLock.releaseCpuLock();
                return Service.START_NOT_STICKY;
            } else if (mCurrentAlarm != null) {
                DLog.d(TAG, "onStartCommond() mCurrentAlarm is not null");
                DLog.d(TAG, "onStartCommond()  " + mCurrentAlarm.mId + " : " + mInstance.mId);
                if (mCurrentAlarm.mId == mInstance.mId) {
                    DLog.d(TAG, "onStartCommond()  same instance");
                    return Service.START_NOT_STICKY;
                } else if (mCurrentAlarm.getAlarmTime() ==
                        mInstance.getAlarmTime()) {
                    DLog.d(TAG, "onStartCommond() same time");
                    AlarmStateManager.setMissedState(mContext, mInstance);
                    return Service.START_NOT_STICKY;
                }
            }
            startAlarmKlaxon(mInstance);
        } else if (action.equals(STOP_ALARM_ACTION)) {
            DLog.d(TAG, "onStartCommond()  stop alarm");
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    private void startAlarmKlaxon(AlarmInstance instance) {
        DLog.i(TAG, "startAlarmKlaxon()");
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(mContext, mCurrentAlarm);
            stopCurrentAlarm();
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        mCurrentAlarm = instance;
        initTelephonyService();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        boolean inCall = mInitialCallState != TelephonyManager.CALL_STATE_IDLE;

        if (inCall) {
            DLog.i(TAG, "startAlarmKlaxon()    in calling ");
            AlarmNotification.updateAlarmNotification(mContext, mCurrentAlarm);
        } else {
            DLog.i(TAG, "startAlarmKlaxon()    AlarmNotification.showAlarmNotification() ");
            AlarmNotification.showAlarmNotification(mContext, mCurrentAlarm);
        }
        AlarmKlaxon.start(mContext, mCurrentAlarm, inCall);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    /**
     * 关闭当前触发闹钟
     */
    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            sendBroadcast(new Intent(ALARM_DONE_ACTION));
            return;
        }

        AlarmKlaxon.stop(this);
        sendBroadcast(new Intent(ALARM_DONE_ACTION));
        mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        public void onCallStateChanged(int state, String ignored) {
            int newPhoneState = mInitialCallState;
            newPhoneState = mTelephonyManager.getCallState();

            if (mCurrentAlarm == null) {
                DLog.d(TAG, "onStateChange mCurrentAlarm is null, just return");
                return;
            }

            if (state != TelephonyManager.CALL_STATE_IDLE
                    && mInitialCallState == TelephonyManager.CALL_STATE_IDLE) {
                DLog.i(TAG, "AlarmService onCallStateChanged sendBroadcast to Missed alarm");
                sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this,
                        "AlarmService", mCurrentAlarm, AlarmInstance.MISSED_STATE));
            }

            if (newPhoneState == TelephonyManager.CALL_STATE_IDLE
                    && state == TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                DLog.i(TAG, "AlarmService onCallStateChanged user hung up the phone");
                /// M: If the alarm has been dismissed by user, shouldn't restart the alarm
                if (mCurrentAlarm.mAlarmState == AlarmInstance.FIRED_STATE) {
                    DLog.i(TAG, "AlarmService AlarmFiredState startAlarm");
                    startAlarm(mContext, mCurrentAlarm);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void onDestroy() {
        stopCurrentAlarm();
        unregisterReceiver(mStopPlayReceiver);
        super.onDestroy();
    }

    private void initTelephonyService() {
        mInitialCallState = mTelephonyManager.getCallState();
    }
}
