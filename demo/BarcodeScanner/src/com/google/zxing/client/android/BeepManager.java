package com.google.zxing.client.android;

import java.io.Closeable;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 管理声音和震动{@link CaptureActivity}.
 * 
 * @author lijian
 * @date 2017-8-14 下午11:15:24
 */
final class BeepManager implements MediaPlayer.OnErrorListener, Closeable {
	private static final String TAG = BeepManager.class.getSimpleName();

	/** 音量 */
	private static final float BEEP_VOLUME = 0.10f;
	/** 震动时长 */
	private static final long VIBRATE_DURATION = 200L;

	private final Activity activity;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private boolean vibrate;

	/**
	 * 管理声音和震动
	 * 
	 * @param activity
	 */
	BeepManager(Activity activity) {
		this.activity = activity;
		this.mediaPlayer = null;
		updatePrefs();
	}

	/**
	 * 更新sp
	 */
	synchronized void updatePrefs() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		playBeep = shouldBeep(prefs, activity);
		vibrate = prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false);
		if (playBeep && mediaPlayer == null) {
			// 在STREAM_SYSTEM音量不可调，并且用户觉得太大声，所以我们现在的音乐流播放。
			activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = buildMediaPlayer(activity);
		}
	}

	/**
	 * 播放beep声和震动
	 */
	synchronized void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) activity
					.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * 是否应该播放beep声
	 * 
	 * @param prefs
	 * @param activity
	 * @return
	 */
	private static boolean shouldBeep(SharedPreferences prefs, Context activity) {
		boolean shouldPlayBeep = prefs.getBoolean(
				PreferencesActivity.KEY_PLAY_BEEP, true);
		if (shouldPlayBeep) {
			// 看看声音设置是否覆盖此
			AudioManager audioService = (AudioManager) activity
					.getSystemService(Context.AUDIO_SERVICE);
			if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
				shouldPlayBeep = false;
			}
		}
		return shouldPlayBeep;
	}

	/**
	 * 构建一个MediaPlayer
	 * 
	 * @param activity
	 * @return
	 */
	private MediaPlayer buildMediaPlayer(Context activity) {
		MediaPlayer mediaPlayer = new MediaPlayer();
		try {
			AssetFileDescriptor file = activity.getResources()
					.openRawResourceFd(R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
			} finally {
				file.close();
			}
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setLooping(false);
			mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
			mediaPlayer.prepare();
			return mediaPlayer;
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			mediaPlayer.release();
			return null;
		}
	}

	@Override
	public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			// 我们结束了，所以就把相应的错误吐司如果需要完成
			activity.finish();
		} else {
			// 可能是媒体播放器的错误，所以释放和重新创建
			close();
			updatePrefs();
		}
		return true;
	}

	@Override
	public synchronized void close() {
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

}
