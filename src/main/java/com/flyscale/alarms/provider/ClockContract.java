package com.flyscale.alarms.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class ClockContract {
	
	public static final String AUTHORITY="com.flyscale.alarms";
	
	
	//���ܱ�ʵ����
	private ClockContract(){}
	
	private interface AlarmSettingColumns extends BaseColumns{
		public static final long INVALID_ID = -1;
		
		public static String VIBRATE="vibrate"; //�Ƿ���
		public static String LABEL="label";  //���ӱ�ǩ
		public static String RINGTONE="ringtone";//��������
	}
	
	
	// �û�������������
	protected  interface AlarmsColumns extends AlarmSettingColumns{
		public static Uri CONTENT_URI=Uri.parse("content://"+AUTHORITY+"/alarms");
		
		public static final Uri NO_RINGTONE_URI=Uri.EMPTY;
		public static final String NO_RINGTONE=NO_RINGTONE_URI.toString();
		
		public static final String HOUR="hour";			// 0-23
		public static final String MINUTES="minutes";	//0-59
		public static final String DAYS_OF_WEEK="daysofweek";
		public static final String ENABLED="enabled";    //�Ƿ񼤻�״̬
		public static final String DELETE_AFTER_USE="delete_after_use"; //������Ƿ�ɾ��
	}
	
	protected interface InstanceColumns extends AlarmSettingColumns{
		public static Uri CONTENT_URI=Uri.parse("content://"+AUTHORITY+"/instances");
		
		public static final int SILENT_STATE=0;
		public static final int LOW_NOTIFICATION_STATE=1;
		public static final int HIDE_NOTIFICATION_STATE=2;
		public static final int HIGH_NOTIFICATION_STATE=3;
		public static final int SNOOZE_STATE=4;
		public static final int FIRED_STATE=5;
		public static final int MISSED_STATE=6;
		public static final int DISMISSED_STATE=7;
		
		public static final String YEAR="year";
		public static final String MONTH="month";
		public static final String DAY="day";
		public static final String HOUR="hour";
		public static final String MINUTES="minutes";
		public static final String ALARM_ID="alarm_id";
		public static final String ALARM_STATE="alarm_state";
	}
	
}
