package com.flyscale.alarms;

import com.flyscale.alarms.alarms.AlarmStateManager;
import com.flyscale.alarms.utils.DLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;


/**
 * 开关机，时间时区发生改变时重新设置闹钟状态
 * @author Administrator
 *
 */
public class AlarmInitReceiver extends BroadcastReceiver{
	private static final String TAG="AlarmInitReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		final String action=intent.getAction();
		DLog.d(TAG, "action="+action);
		
		//This allows you to process the broadcast off of the main
	    //thread of your app.
		final PendingResult result=goAsync();
		final WakeLock wl=AlarmAlertWakeLock.createPartialWakeLock(context);
		wl.acquire();
		
		AlarmStateManager.updateGlobalIntentId(context);
		AsyncHandler.post(new Runnable() {
			
			@Override
			public void run() {
				AlarmStateManager.fixAlarmInstances(context);
				result.finish();
				wl.release();
			}
		});
	}

}
