package com.flyscale.alarms.alarms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.TextUtils;

import com.flyscale.alarms.R;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.AlarmUtils;

public class AlarmNotification {

	public static void showLowPriorityNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Resources res=context.getResources();
		
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(res.getString(R.string.alarm_alert_predismiss_title))
			.setContentText(AlarmUtils.getALarmText(context, instance))
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setOngoing(false)
			.setAutoCancel(false)
			.setPriority(Notification.PRIORITY_DEFAULT);
		
		//setup up hide notification
		Intent hideIntent=AlarmStateManager.createStateChangeIntent(context, "DELETE_TAG", instance, 
				AlarmInstance.HIDE_NOTIFICATION_STATE);
		notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(),
				hideIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
	}
	
	public static void showHighPriorityNofification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Resources res=context.getResources();
		
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(res.getString(R.string.alarm_alert_predismiss_title))
			.setContentText(AlarmUtils.getALarmText(context, instance))
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setOngoing(true)
			.setAutoCancel(false)
			.setPriority(Notification.PRIORITY_HIGH);
		
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
	}
	
	public static void showSnoozeNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Resources res=context.getResources();
		
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(instance.getLabelOrDefault(context))
			.setContentText(res.getString(R.string.alarm_alert_snooze_until, 
					AlarmUtils.getFormattedTime(context, instance.getAlarmTime())))
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setOngoing(true)
			.setAutoCancel(false)
			.setPriority(Notification.PRIORITY_MAX);
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
	}
	
	//闹钟响起后无操作
	public static void showMissedNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		String label=instance.mLabel;
		String alarmTime=AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
		String contentTextString=TextUtils.isEmpty(instance.mLabel)?alarmTime:
			context.getString(R.string.alarm_missed_text, alarmTime,label);
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(context.getString(R.string.alarm_missed_title))
			.setContentText(contentTextString)
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setPriority(Notification.PRIORITY_HIGH);
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
	}
	
	public static void showAlarmNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));//关闭当前打开窗口，效果类似按下home键
		Resources res=context.getResources();
		
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(instance.getLabelOrDefault(context))
			.setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setAutoCancel(false)
			.setDefaults(Notification.DEFAULT_LIGHTS)
			.setPriority(Notification.PRIORITY_MAX);
		
        // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context, "SNOOZE_TAG",
                instance, AlarmInstance.SNOOZE_STATE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.stat_notify_alarm,
                res.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);
        
        
        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                res.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);
		
		Intent fullScreenIntent=AlarmInstance.createIntent(context, AlarmActivity2.class,instance.mId);
		fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
				Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		notification.setFullScreenIntent(PendingIntent.getActivity(context, 
				instance.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT),true);
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
		
	}

	 /// M: Update the alarm's notification when alarm be set fired state directyly	
	public static void updateAlarmNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));//关闭当前打开窗口，效果类似按下home键
		Resources res=context.getResources();
		
		Notification.Builder notification=new Notification.Builder(context)
			.setContentTitle(instance.getLabelOrDefault(context))
			.setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))
			.setSmallIcon(R.drawable.stat_notify_alarm)
			.setAutoCancel(false)
			.setDefaults(Notification.DEFAULT_LIGHTS)
			.setPriority(Notification.PRIORITY_MAX);
		
		 // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_STATE_EXTRA,
                instance, AlarmInstance.SNOOZE_STATE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.stat_notify_alarm,
                res.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);
        
        
        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,  AlarmStateManager.ALARM_STATE_EXTRA,
                instance, AlarmInstance.DISMISSED_STATE);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                res.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);
		
		nm.cancel(instance.hashCode());
		nm.notify(instance.hashCode(), notification.build());
	}
	
	
	public static void clearNotification(Context context,AlarmInstance instance){
		NotificationManager nm=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(instance.hashCode());
	}
	
	/**
	 * It is used by the AlarmClock application and the StatusBar service.
     * 手机屏幕右上角status bar 中闹钟图标的显示（不是左边那个notification里的，注意区分）
     * 这个显示是在systemUI里；
	 */ 
	public static final String SYSTEM_ALARM_CHANGE_ACTION="android.intent.action.ALARM_CHANGED";
	public static void  broadcastNextAlarm(Context context,AlarmInstance instance){
		String timeString="";
		boolean showStatusIcon=false;
		if(instance!=null){
			timeString=AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
			showStatusIcon=true;
		}
		
		Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED,
				timeString);
		Intent alarmChanged=new Intent(SYSTEM_ALARM_CHANGE_ACTION);
		alarmChanged.putExtra("alarmSet", showStatusIcon);//在systemUI里接收这个广播
		context.sendBroadcast(alarmChanged);
	}
}
