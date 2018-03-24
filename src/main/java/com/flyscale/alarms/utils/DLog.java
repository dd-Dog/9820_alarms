package com.flyscale.alarms.utils;

import android.util.Log;

public class DLog  {
	
	public static final boolean DEBUG_ENABLE=true;
	public static final String LOG_TAG="MyAlarmClock";
	
	public static void  d(String tag,String msg){
		if(DEBUG_ENABLE){
		 Log.d(LOG_TAG+"---"+tag,msg);
		}
	}
	
	public static void i(String tag,String msg){
		if(DEBUG_ENABLE){
			Log.i(LOG_TAG+"---"+tag,msg);
		}
	}
	
	public static void  w(String tag,String msg){
		if(DEBUG_ENABLE){
			Log.w(LOG_TAG+"---"+tag,msg);
		}
	}
	
	public static void  e(String tag,String msg){
		if(DEBUG_ENABLE){
			Log.e(LOG_TAG+"---"+tag,msg);
		}
	}


}
