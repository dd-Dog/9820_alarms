package com.flyscale.alarms.alarms;

import java.io.IOException;

import com.flyscale.alarms.provider.Alarm;
import com.flyscale.alarms.provider.AlarmInstance;
import com.flyscale.alarms.utils.AlarmUtils;
import com.flyscale.alarms.utils.DLog;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;


/**
 * 控制响铃，振动
 * @author Administrator
 *
 */
public class AlarmKlaxon {
	private static final String TAG="AlarmKlaxon";
	private static final long[] VIBRATE_PATTERN=new long[]{500,500};
	private static boolean sStarted=false;
	private static MediaPlayer sMediaPlayer=null;
	

	public static void start(final Context context,AlarmInstance instance,boolean inTelephoneCall){
		DLog.i(TAG, "start()  instanceId="+instance.mId+",inTelephoneCall="+inTelephoneCall);
		//make sure we are stop before starting
		stop(context);
		
		if(inTelephoneCall){
			//通话中什么也不做
			return;
		}
		
		if(!Alarm.NO_RINGTONE_URI.equals(instance.mRingtone)){
			Uri alarmNoise=instance.mRingtone;
			
			//if the alarm's uri exists but the real file is missing, roll back to default one
			if(alarmNoise==null || !AlarmUtils.isRingtoneExisted(context, alarmNoise.toString())){
				alarmNoise=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
				DLog.i(TAG, "Using default alarm "+alarmNoise);
			}
			
			sMediaPlayer=new MediaPlayer();
			sMediaPlayer.setOnErrorListener(new OnErrorListener() {
				
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					DLog.e(TAG, "ERROR occurred while playing audio.Stoping AlarmKlaxon.");
					AlarmKlaxon.stop(context);
					return false;
				}
			});
			
			try {
				sMediaPlayer.setDataSource(context, alarmNoise);
				startAlarm(context, sMediaPlayer);
			} catch (IllegalArgumentException e) {
				DLog.e(TAG, "IllegalArgumentException : ERROR occurred .");
				e.printStackTrace();
			} catch (SecurityException e) {
				DLog.e(TAG, "SecurityException : ERROR occurred .");
				e.printStackTrace();
			} catch (IllegalStateException e) {
				DLog.e(TAG, "IllegalStateException : ERROR occurred .");
				e.printStackTrace();
			} catch (IOException e) {
				DLog.e(TAG, "IOException : ERROR occurred.");
				e.printStackTrace();
			}	
		}
		
		if(instance.mVibrate){
			Vibrator vibrator=(Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_PATTERN, 0);
		}
		
		sStarted=true;
	}
	
	public static void stop(Context context){
		if(sStarted){
			sStarted=false;
			if(sMediaPlayer!=null){
				sMediaPlayer.stop();
				AudioManager am=(AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
				am.abandonAudioFocus(null);
				sMediaPlayer.release();
				sMediaPlayer=null;
			}
			((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
		}
	}
	
	
	private static void startAlarm(Context context,MediaPlayer player) throws IllegalStateException, IOException{
		AudioManager am=(AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if(am.getStreamVolume(AudioManager.STREAM_ALARM)!=0){
			player.setAudioStreamType(AudioManager.STREAM_ALARM);
			player.setLooping(true);
			player.prepare();
			am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			player.start();
		}
	}
}
