package com.flyscale.alarms.provider;

import com.flyscale.alarms.utils.DLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ClockDatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG="ClockDatabaseHelper";

	private static final String DATABASE_NAME="alarms.db";
	private static final int DATABASE_VERSION=1;
	static final String ALARMS_TABLE_NAME="alarm_templates";
	static final String INSTANCES_TABLE_NAME="alarm_instances";
	
	// This creates a default alarm at 8:30 for every Mon,Tue,Wed,Thu,Fri
    private static final String DEFAULT_ALARM_1 = "(0,8, 30, 31, 0, '', NULL, 0);";
    // This creates a default alarm at 9:30 for every Sat,Sun
    private static final String DEFAULT_ALARM_2 = "(0,9, 00, 96, 0, '', NULL, 0);";

	
	
	public ClockDatabaseHelper(Context context){
		super(context,DATABASE_NAME,null,DATABASE_VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		DLog.d(TAG,"onCreate()");
		createAlarmsTable(db);
		createInstanceTable(db);
		
		
		String cs=", ";
		String insertMe="INSERT INTO "+ALARMS_TABLE_NAME+"( "+
				ClockContract.AlarmsColumns.ENABLED+cs+
				ClockContract.AlarmsColumns.HOUR+cs+
				ClockContract.AlarmsColumns.MINUTES+cs+
				ClockContract.AlarmsColumns.DAYS_OF_WEEK+cs+
				ClockContract.AlarmsColumns.VIBRATE+cs+
				ClockContract.AlarmsColumns.LABEL+cs+
				ClockContract.AlarmsColumns.RINGTONE+cs+
				ClockContract.AlarmsColumns.DELETE_AFTER_USE+") VALUES";
				
		db.execSQL(insertMe+DEFAULT_ALARM_1);
		db.execSQL(insertMe+DEFAULT_ALARM_2);

	}
	
	private void createAlarmsTable(SQLiteDatabase db){
		db.execSQL("CREATE TABLE "+ALARMS_TABLE_NAME+" ("+
				ClockContract.AlarmsColumns._ID+" INTEGER PRIMARY KEY, "+
				ClockContract.AlarmsColumns.ENABLED+" INTEGER NOT NULL, "+
				ClockContract.AlarmsColumns.HOUR+" INTEGER NOT NULL, "+
				ClockContract.AlarmsColumns.MINUTES+" INTEGER NOT NULL, "+
				ClockContract.AlarmsColumns.DAYS_OF_WEEK+" INTEGER NOT NULL, "+
				ClockContract.AlarmsColumns.VIBRATE+" INTEGER NOT NULL, "+
				ClockContract.AlarmsColumns.LABEL+" TEXT NOT NULL, "+
				ClockContract.AlarmsColumns.RINGTONE+" TEXT, "+
				ClockContract.AlarmsColumns.DELETE_AFTER_USE+" INTEGER NOT NULL DEFAULT 0);"				
				);
	}
	
	//alarmid 
	private void createInstanceTable(SQLiteDatabase db){
		db.execSQL("CREATE TABLE "+INSTANCES_TABLE_NAME+" ("+
				ClockContract.InstanceColumns._ID+" INTEGER PRIMARY KEY, "+
				ClockContract.InstanceColumns.YEAR+"  INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.MONTH+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.DAY+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.HOUR+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.MINUTES+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.LABEL+" TEXT NOT NULL, "+
				ClockContract.InstanceColumns.VIBRATE+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.RINGTONE+" TEXT, "+
				ClockContract.InstanceColumns.ALARM_STATE+" INTEGER NOT NULL, "+
				ClockContract.InstanceColumns.ALARM_ID+" INTEGER REFERENCES "+
					ALARMS_TABLE_NAME+"("+ClockContract.AlarmsColumns._ID+")"+
					"ON UPDATE CASCADE ON DELETE CASCADE"+
				");"	
				);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
