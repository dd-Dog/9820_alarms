package com.flyscale.alarms.provider;

import com.flyscale.alarms.utils.DLog;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ClockProvider extends ContentProvider{
	private static final String TAG="ClockProvider";
	
	private ClockDatabaseHelper mOpenHelper;
	
	private static final int ALARMS=1;
	private static final int ALARMS_ID=2;
	private static final int INSTANCES=3;
	private static final int INSTANCES_ID=4;
	
	private static final UriMatcher sURLMather=new UriMatcher(UriMatcher.NO_MATCH);
	static{
		sURLMather.addURI(ClockContract.AUTHORITY,"alarms", ALARMS);
		sURLMather.addURI(ClockContract.AUTHORITY,"alarms/#",ALARMS_ID);
		sURLMather.addURI(ClockContract.AUTHORITY,"instances",INSTANCES);
		sURLMather.addURI(ClockContract.AUTHORITY,"instances/#",INSTANCES_ID);
	}

	@Override
	public boolean onCreate() {
		mOpenHelper=new ClockDatabaseHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb=new SQLiteQueryBuilder();
		
		int match=sURLMather.match(uri);
		switch(match){
		case ALARMS:
			qb.setTables(ClockDatabaseHelper.ALARMS_TABLE_NAME);
			break;
		case ALARMS_ID:
			qb.setTables(ClockDatabaseHelper.ALARMS_TABLE_NAME);
			qb.appendWhere(ClockContract.AlarmsColumns._ID+"=");
			qb.appendWhere(uri.getLastPathSegment());
			break;
		case INSTANCES:
			qb.setTables(ClockDatabaseHelper.INSTANCES_TABLE_NAME);
			break;
		case INSTANCES_ID:
			qb.setTables(ClockDatabaseHelper.INSTANCES_TABLE_NAME);
			qb.appendWhere(ClockContract.InstanceColumns._ID+"=");
			qb.appendWhere(uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URL "+uri);
			
		}
		
		SQLiteDatabase database=mOpenHelper.getReadableDatabase();
		Cursor cursor=qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
		if(cursor==null){
			DLog.i(TAG, "Alarms.query : failed");
		}else{
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		int match=sURLMather.match(uri);
		switch(match){
		case ALARMS:
			return "vnd.android.cursor.dir/alarms";
		case ALARMS_ID:
			return "vnd.android.cursor.item/alarms";
		case INSTANCES:
			return "vnd.android.cursor.dir/instances";
		case INSTANCES_ID:
			return "vnd.android.cursor.item/instances";
		default:
			throw new IllegalArgumentException("Unknown URL "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId;
		SQLiteDatabase database=mOpenHelper.getWritableDatabase();
		switch(sURLMather.match(uri)){
		case ALARMS:
			rowId=database.insert(ClockDatabaseHelper.ALARMS_TABLE_NAME, ClockContract.AlarmsColumns.RINGTONE, values);
			break;
		case INSTANCES:
			rowId=database.insert(ClockDatabaseHelper.INSTANCES_TABLE_NAME, ClockContract.InstanceColumns.RINGTONE, values);
			break;
		default:
			throw new IllegalArgumentException("Cannot insert from URL : "+uri);
		}
		
		Uri uriResult=ContentUris.withAppendedId(uri, rowId);
		getContext().getContentResolver().notifyChange(uriResult, null);
		return uriResult;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		int count;
		String primaryKey;
		SQLiteDatabase database=mOpenHelper.getWritableDatabase();
		switch(sURLMather.match(uri)){
		case ALARMS_ID:
			primaryKey=uri.getLastPathSegment();
			if(TextUtils.isEmpty(where)){
				where=ClockContract.AlarmsColumns._ID+"="+primaryKey;
			}else{
				where=ClockContract.AlarmsColumns._ID+"="+primaryKey+" AND ("+where+")";//加括号避免可能破坏原来运算顺序
			}
			count=database.delete(ClockDatabaseHelper.ALARMS_TABLE_NAME, where, whereArgs);
			break;
		case INSTANCES_ID:
			primaryKey=uri.getLastPathSegment();
			if(TextUtils.isEmpty(where)){
				where=ClockContract.InstanceColumns._ID+"="+primaryKey;
			}else{
				where=ClockContract.InstanceColumns._ID+"="+primaryKey+" AND ("+where+")";//加括号避免可能破坏原来运算顺序
			}
			count=database.delete(ClockDatabaseHelper.INSTANCES_TABLE_NAME, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Cannot delete from URL :"+uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		String id=uri.getLastPathSegment();
		SQLiteDatabase database=mOpenHelper.getWritableDatabase();
		switch(sURLMather.match(uri)){
		case ALARMS_ID:
			count=database.update(ClockDatabaseHelper.ALARMS_TABLE_NAME, values, 
					ClockContract.AlarmsColumns._ID+"="+id,null);
			break;
		case INSTANCES_ID:
			count=database.update(ClockDatabaseHelper.INSTANCES_TABLE_NAME, values,
					ClockContract.InstanceColumns._ID+"="+id, null);
			break;
		default:
			throw new IllegalArgumentException("Cannot update from URL :"+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
