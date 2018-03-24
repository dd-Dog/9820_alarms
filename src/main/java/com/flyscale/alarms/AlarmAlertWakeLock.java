package com.flyscale.alarms;

import com.flyscale.alarms.utils.DLog;

import android.content.Context;
import android.os.PowerManager;

public class AlarmAlertWakeLock {
	
	private static PowerManager.WakeLock sCpuWakeLock;
	
	public static PowerManager.WakeLock createPartialWakeLock(Context context){
		PowerManager  pw=(PowerManager) context.getSystemService(Context.POWER_SERVICE);
		return pw.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, DLog.LOG_TAG);
	}
	
	public static void acquireCpuWakeLock(Context context){
		if(sCpuWakeLock!=null){
			return;
		}
		
		sCpuWakeLock=createPartialWakeLock(context);
		sCpuWakeLock.acquire();;
	}
	
	public static void releaseCpuLock(){
		if(sCpuWakeLock!=null){
			sCpuWakeLock.release();
			sCpuWakeLock=null;
		}
	}

}
