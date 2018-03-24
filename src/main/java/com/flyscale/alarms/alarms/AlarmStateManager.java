package com.flyscale.alarms.alarms;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.flyscale.alarms.AlarmAlertWakeLock;
import com.flyscale.alarms.AsyncHandler;
import com.flyscale.alarms.SettingsActivity;
import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.AlarmUtils;
import com.flyscale.alarms.utils.DLog;
import com.flyscale.alarms.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class AlarmStateManager extends BroadcastReceiver {
	private static final String TAG = "AlarmStateManager";
	private static final String CHANGE_STATE_ACTION = "com.hs.myclock.CHANGE_STATE";

	private static final String ALARM_MANAGER_TAG = "ALARM_MANAGER";
																	
	public static final String ALARM_STATE_EXTRA = "intent.extra.alarm.state";
																				
	// 避免设置闹钟时间正好是闹钟触发时间，添加15s缓冲时间
	private static final int ALARM_FIRE_BUFFER = 15;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final PendingResult result = goAsync();
		AlarmAlertWakeLock.acquireCpuWakeLock(context);
		AsyncHandler.post(new Runnable() {

			@Override
			public void run() {
				handleIntent(context, intent);
				result.finish();
				AlarmAlertWakeLock.releaseCpuLock();
			}

		});

	}

	private void handleIntent(Context context, Intent intent) {
		final String action = intent.getAction();
		DLog.i(TAG, "received intent " + intent);
		Uri uri = intent.getData();
		AlarmInstance instance = AlarmInstance.getInstanceById(
				context.getContentResolver(), AlarmInstance.getId(uri));

		if (instance == null) {
			DLog.e(TAG, "Can't change state for unknown instance: " + uri);
			return;
		}

		if (CHANGE_STATE_ACTION.equals(action)) {
			int globaldId = getGlobalIntentId(context);
			int intentId = intent.getIntExtra(ALARM_GLOBAL_ID_EXTRA, -1);
			int alarmState = intent.getIntExtra(ALARM_STATE_EXTRA, -1);
			DLog.i(TAG, "globalId: " + globaldId + "\n" + "intentId: "
					+ intentId + "\n" + "alarmState: " + alarmState);

			if (alarmState >= 0) {
				setAlarmState(context, instance, alarmState);
			} else {
				registerInstance(context, instance, true);
			}
		}
	}

	public static void setSilentState(Context context, AlarmInstance instance) {
		DLog.d(TAG, "setSilentState to instance " + instance.mId);
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.SILENT_STATE;
		AlarmInstance.updateInstance(resolver, instance);

		AlarmNotification.clearNotification(context, instance);
		scheduleInstanceStateChange(context, instance.getLowNotificationTime(),
				instance, AlarmInstance.LOW_NOTIFICATION_STATE);
	}

	public static void setLowNotificationState(Context context,
			AlarmInstance instance) {
		DLog.d(TAG, "setLowNotificationState to instance " + instance.mId);
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.LOW_NOTIFICATION_STATE;
		AlarmInstance.updateInstance(resolver, instance);

		AlarmNotification.showLowPriorityNotification(context, instance);
		scheduleInstanceStateChange(context,
				instance.getHighNotificationTime(), instance,
				AlarmInstance.HIGH_NOTIFICATION_STATE);
	}

	public static void setHideNotificationState(Context context,
			AlarmInstance instance) {
		DLog.d(TAG, "setHideNotificationState to instance " + instance.mId);
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.HIDE_NOTIFICATION_STATE;
		AlarmInstance.updateInstance(resolver, instance);

		AlarmNotification.clearNotification(context, instance);
		scheduleInstanceStateChange(context,
				instance.getHighNotificationTime(), instance,
				AlarmInstance.HIGH_NOTIFICATION_STATE);
	}

	public static void setHighNotificationState(Context context,
			AlarmInstance instance) {
		DLog.d(TAG, "setHighNotificationState to instance " + instance.mId);
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.HIGH_NOTIFICATION_STATE;
		AlarmInstance.updateInstance(resolver, instance);

		AlarmNotification.showHighPriorityNofification(context, instance);
		scheduleInstanceStateChange(context, instance.getAlarmTime(), instance,
				AlarmInstance.FIRED_STATE);
	}

	public static void setFiredState(Context context, AlarmInstance instance) {
		DLog.d(TAG, "setFiredState to instance " + instance.mId);
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.FIRED_STATE;
		AlarmInstance.updateInstance(resolver, instance);

		AlarmService.startAlarm(context, instance);

		Calendar timeout = instance.getTimeout(context);
		if (timeout != null) {
			scheduleInstanceStateChange(context, timeout, instance,
					AlarmInstance.MISSED_STATE);
		}

		updateNextAlarm(context);
	}

	public static void setSnoozeState(Context context, AlarmInstance instance) {
		DLog.d(TAG, "setSnoozeState to instance " + instance.mId);
		AlarmService.stopAlarm(context, instance);
		
		ContentResolver resolver = context.getContentResolver();
		instance.mAlarmState = AlarmInstance.SNOOZE_STATE;
		
		String snoozeMinutesStr=PreferenceManager.getDefaultSharedPreferences(context)
				.getString(SettingsActivity.KEY_ALARM_SNOOZE, SettingsActivity.DEFAULT_SNOOZE_MINUTES);
		int snoozeMinutes=Integer.parseInt(snoozeMinutesStr);
		Calendar newAlarmTime=Calendar.getInstance();
		newAlarmTime.add(Calendar.MINUTE,snoozeMinutes);
		DLog.d(TAG,"setSnoozeState to instance "+instance.mId+" at "+
				AlarmUtils.getFormattedTime(context, newAlarmTime));
		instance.setAlarmTime(newAlarmTime);
		AlarmInstance.updateInstance(resolver, instance);
		
		AlarmNotification.showSnoozeNotification(context, instance);
		scheduleInstanceStateChange(context, newAlarmTime, instance, AlarmInstance.FIRED_STATE);
		
		updateNextAlarm(context);
	}

	public static void setMissedState(Context context, AlarmInstance instance) {
		DLog.d(TAG, "setMissedState to instance " + instance.mId);
		
		AlarmService.stopAlarm(context, instance);
		if(instance.mAlarmId!=null){
			updateParentAlarm(context, instance);
		}
		
		ContentResolver resolver=context.getContentResolver();
		instance.mAlarmState=AlarmInstance.MISSED_STATE;
		AlarmInstance.updateInstance(resolver, instance);
		
		AlarmNotification.showMissedNotification(context, instance);
		scheduleInstanceStateChange(context, instance.getMissedTimeToLive(), instance, AlarmInstance.DISMISSED_STATE);
		updateNextAlarm(context);
	}

	public static void setDismissState(Context context, AlarmInstance instance) {
		DLog.d(TAG, "setDismissState to instance " + instance.mId);
		unregisterInstance(context, instance);

		if (instance.mAlarmId != null) {
			updateParentAlarm(context, instance);
		}

		AlarmInstance.deleteInstance(context.getContentResolver(), instance);
		updateNextAlarm(context);
	}

	/**
	 * 注册instance到statemanager,设置正确的state;
	 * 闹钟生成后自动调整state，只有添加新闹钟,系统时间变更,重新开机时调用这个方法；
	 */
	public static void registerInstance(Context context,
			AlarmInstance instance, boolean updateNextAlarm) {
		Calendar currentTime = Calendar.getInstance();
		Calendar alarmTime = instance.getAlarmTime();
		Calendar timeoutTime = instance.getTimeout(context);// 响铃超时
		Calendar lowNotificationTime = instance.getLowNotificationTime();
		Calendar highNotificationTime = instance.getHighNotificationTime();
		Calendar missedTTL = instance.getMissedTimeToLive();

		if (instance.mAlarmState == AlarmInstance.DISMISSED_STATE) {
			// This should never happen, but add a quick check here
			setDismissState(context, instance);
			return;
		} else if (instance.mAlarmState == AlarmInstance.FIRED_STATE) {
			boolean hasTimeOut = timeoutTime != null
					&& currentTime.after(timeoutTime);
			if (!hasTimeOut) {
				AlarmNotification.updateAlarmNotification(context, instance);
				setFiredState(context, instance);
				return;
			}
		} else if (instance.mAlarmState == AlarmInstance.MISSED_STATE) {
			//闹钟miss，但时间又发生了改变；
			if (currentTime.before(alarmTime)) {
				if (instance.mAlarmId == null) {
					setDismissState(context, instance);
					return;
				}
				
				ContentResolver cr = context.getContentResolver();
				Alarm alarm = Alarm.getAlarm(cr, instance.mAlarmId);
				alarm.enabled = true;
				Alarm.updateAlarm(cr, alarm);
			}
		}

		if (currentTime.after(missedTTL)) {
			setDismissState(context, instance);
		} else if (currentTime.after(alarmTime)) {
			Calendar alarmBuffer = Calendar.getInstance();
			alarmBuffer.setTime(alarmTime.getTime());
			alarmBuffer.add(Calendar.SECOND, ALARM_FIRE_BUFFER);
			if (currentTime.before(alarmBuffer)) {
				setFiredState(context, instance);
			} else {
				setMissedState(context, instance);
			}
		} else if (instance.mAlarmState == AlarmInstance.SNOOZE_STATE) {
			AlarmNotification.showSnoozeNotification(context, instance);
			scheduleInstanceStateChange(context, instance.getAlarmTime(),
					instance, AlarmInstance.FIRED_STATE);
		} else if (currentTime.after(highNotificationTime)) {
			setHighNotificationState(context, instance);
		} else if (currentTime.after(lowNotificationTime)) {
			if (instance.mAlarmState == AlarmInstance.HIDE_NOTIFICATION_STATE) {
				setHideNotificationState(context, instance);
			} else {
				setLowNotificationState(context, instance);
			}
		} else {
			setSilentState(context, instance);
		}

		if (updateNextAlarm) {
			updateNextAlarm(context);
		}
	}

	public static void unregisterInstance(Context context,
			AlarmInstance instance) {
		AlarmService.stopAlarm(context, instance);
		AlarmNotification.clearNotification(context, instance);
		cancelScheduledInstance(context, instance);
	}

	private static void scheduleInstanceStateChange(Context context,
			Calendar time, AlarmInstance instance, int newState) {
		long timeInMillis = time.getTimeInMillis();
		DLog.d(TAG,
				"Scheduling state : " + newState + "   to  instance :"
						+ instance.mId + " at "
						+ AlarmUtils.getFormattedTime(context, time) + " ("
						+ timeInMillis + ")");
		Intent stateChangeIntent = createStateChangeIntent(context,
				ALARM_MANAGER_TAG, instance, newState);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				instance.hashCode(), stateChangeIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		if (Utils.isKitKatOrLater()) {
			// AlarmManager.RTC_WAKEUP : 手机处于休眠状态，闹钟时间到，可唤醒手机
			am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
		} else {
			am.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
		}
	}

	private static void cancelScheduledInstance(Context context,
			AlarmInstance instance) {
		Intent intent = createStateChangeIntent(context, ALARM_MANAGER_TAG,
				instance, null);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				instance.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pendingIntent);
	}

	public static Intent createStateChangeIntent(Context context, String tag,
			AlarmInstance instance, Integer state) {
		Intent intent = AlarmInstance.createIntent(context,
				AlarmStateManager.class, instance.mId);
		intent.setAction(CHANGE_STATE_ACTION);
		intent.addCategory(tag);
		intent.putExtra(ALARM_GLOBAL_ID_EXTRA, getGlobalIntentId(context));
		DLog.i(TAG, "createStateChangeIntent GlobalIntentId = "
				+ getGlobalIntentId(context));
		if (state != null) {
			intent.putExtra(ALARM_STATE_EXTRA, state.intValue());
		}
		return intent;
	}

	/**
	 * 删除并解注册 alarmId相关的所有alarmInstance；
	 */
	public static void deleteAllInstances(Context context, long alarmId) {
		ContentResolver resolver = context.getContentResolver();
		List<AlarmInstance> instances = AlarmInstance.getInstanceByAlarmId(
				resolver, alarmId);
		
		for (AlarmInstance instance : instances) {
			unregisterInstance(context, instance);
			AlarmInstance.deleteInstance(resolver, instance);
		}
		updateNextAlarm(context);
	}

	private void setAlarmState(Context context, AlarmInstance instance,
			int state) {
		switch (state) {
		case AlarmInstance.SILENT_STATE:
			setSilentState(context, instance);
			break;
		case AlarmInstance.LOW_NOTIFICATION_STATE:
			setLowNotificationState(context, instance);
			break;
		case AlarmInstance.HIDE_NOTIFICATION_STATE:
			setHideNotificationState(context, instance);
			break;
		case AlarmInstance.HIGH_NOTIFICATION_STATE:
			setHighNotificationState(context, instance);
			break;
		case AlarmInstance.FIRED_STATE:
			setFiredState(context, instance);
			break;
		case AlarmInstance.SNOOZE_STATE:
			setSnoozeState(context, instance);
			break;
		case AlarmInstance.MISSED_STATE:
			setMissedState(context, instance);
			break;
		case AlarmInstance.DISMISSED_STATE:
			setDismissState(context, instance);
			break;
		default:
			DLog.e(TAG, "Trying to change to unknown alarm state: " + state);
		}
	}

	public static void updateNextAlarm(Context context) {
		AlarmInstance nextAlarm = getNearestAlarm(context);
		AlarmNotification.broadcastNextAlarm(context, nextAlarm);
	}

	public static AlarmInstance getNearestAlarm(Context context) {
		AlarmInstance nextAlarm = null;
		ContentResolver cr = context.getContentResolver();
		String activeAlarmQuery = AlarmInstance.ALARM_STATE + "<"
				+ AlarmInstance.FIRED_STATE;
		for (AlarmInstance instance : AlarmInstance.getInstances(cr,
				activeAlarmQuery)) {
			if (nextAlarm == null
					|| instance.getAlarmTime().before(nextAlarm.getAlarmTime())) {
				nextAlarm = instance;
			}
		}
		return nextAlarm;
	}

	/**
	 * Used by dismissed and missed states, to update parent alarm. This will
	 * either disable, delete or reschedule parent alarm.
	 * 
	 * @param context
	 *            application context
	 * @param instance
	 *            to update parent for
	 */
	private static void updateParentAlarm(Context context,
			AlarmInstance instance) {
		ContentResolver resover = context.getContentResolver();
		Alarm alarm = Alarm.getAlarm(resover, instance.mAlarmId);

		if (alarm == null) {
			DLog.w(TAG,
					"updateParentAlarm() :   alarm is null or has been deleted");
		}

		if (!alarm.daysOfWeek.isRepeating()) {
			if (alarm.deleteAfterUse) {
				DLog.d(TAG, "Deleting parent alarm: " + alarm.id);
				Alarm.deleteAlarm(resover, alarm.id);
			} else {
				DLog.d(TAG, "Disabling parent alarm: " + alarm.id);
				alarm.enabled = false;
				Alarm.updateAlarm(resover, alarm);
			}
		} else {
			Calendar currentTime = Calendar.getInstance();
			AlarmInstance nextRepeatedInstance = alarm
					.createInstanceAfter(currentTime);
			DLog.d(TAG,
					"Creating new instance for repeating alarm "
							+ alarm.id
							+ " at "
							+ AlarmUtils.getFormattedTime(context,
									nextRepeatedInstance.getAlarmTime()));
			AlarmInstance.addInsatnce(resover, nextRepeatedInstance);
			registerInstance(context, nextRepeatedInstance, true);
		}
	}

	public static void fixAlarmInstances(Context context){
		HashMap<Long,AlarmInstance> duplicatedInstance=new HashMap<Long,AlarmInstance>();
		final ContentResolver resolver=context.getContentResolver();
		
		for(AlarmInstance instance : AlarmInstance.getInstances(resolver, null)){
			if(duplicatedInstance.get(instance.mAlarmId)==null){
				duplicatedInstance.put(instance.mAlarmId, instance);
			}else{
				AlarmInstance.deleteInstance(resolver, instance);
			}
			getFixedAlarmInstance(context, instance);
			AlarmStateManager.registerInstance(context, instance, false);
		}
		AlarmStateManager.updateNextAlarm(context);
	}
	
	private static AlarmInstance getFixedAlarmInstance(Context context,AlarmInstance instance){
		ContentResolver resolver=context.getContentResolver();
		Alarm alarm=Alarm.getAlarm(resolver, instance.mAlarmId);
		Calendar currentTime = Calendar.getInstance();// the system's current time
		AlarmInstance newInstance=alarm.createInstanceAfter(currentTime);
		Calendar newTime=newInstance.getAlarmTime();
		
		instance.setAlarmTime(newTime);
		AlarmInstance.updateInstance(resolver, instance);

		return instance;
	}
	
	
	// ---------------- 全局id,每次开机，时间改变时更新id-----------------
	private static final String ALARM_GLOBAL_ID_EXTRA = "intent.extra.alarm.global.id";

	private static int getGlobalIntentId(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getInt(ALARM_GLOBAL_ID_EXTRA, -1);
	}

	public static void updateGlobalIntentId(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		int globalId = prefs.getInt(ALARM_GLOBAL_ID_EXTRA, -1) + 1;
		prefs.edit().putInt(ALARM_GLOBAL_ID_EXTRA, globalId).commit();
	}

}
