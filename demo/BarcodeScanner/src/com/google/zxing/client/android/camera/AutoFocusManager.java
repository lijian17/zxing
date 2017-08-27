package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

import com.google.zxing.client.android.PreferencesActivity;

/**
 * 相机自动对焦管理器
 * 
 * @author lijian
 * @date 2017-8-27 上午9:40:45
 */
final class AutoFocusManager implements Camera.AutoFocusCallback {
	private static final String TAG = AutoFocusManager.class.getSimpleName();

	/** 自动对焦间隔时间 */
	private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;
	/** 对焦模式 */
	private static final Collection<String> FOCUS_MODES_CALLING_AF;
	static {
		FOCUS_MODES_CALLING_AF = new ArrayList<String>(2);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
	}

	/** 对焦停止的 */
	private boolean stopped;
	private boolean focusing;
	/** 使用自动对焦模式 */
	private final boolean useAutoFocus;
	private final Camera camera;
	private AsyncTask<?, ?, ?> outstandingTask;

	/**
	 * 相机自动对焦管理器
	 * 
	 * @param context
	 *            上下文
	 * @param camera
	 *            相机
	 */
	AutoFocusManager(Context context, Camera camera) {
		this.camera = camera;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String currentFocusMode = camera.getParameters().getFocusMode();
		useAutoFocus = sharedPrefs.getBoolean(
				PreferencesActivity.KEY_AUTO_FOCUS, true)
				&& FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
		Log.i(TAG, "当前对焦模式 '" + currentFocusMode + "'; 使用自动对焦? " + useAutoFocus);
		start();
	}

	@Override
	public synchronized void onAutoFocus(boolean success, Camera theCamera) {
		focusing = false;
		autoFocusAgainLater();
	}

	/**
	 * 自动对焦循环尝试
	 */
	private synchronized void autoFocusAgainLater() {
		if (!stopped && outstandingTask == null) {
			AutoFocusTask newTask = new AutoFocusTask();
			try {
				newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				outstandingTask = newTask;
			} catch (RejectedExecutionException ree) {
				Log.w(TAG, "无法请求自动对焦", ree);
			}
		}
	}

	/**
	 * 开始执行自动对焦
	 */
	synchronized void start() {
		if (useAutoFocus) {
			outstandingTask = null;
			if (!stopped && !focusing) {
				try {
					camera.autoFocus(this);
					focusing = true;
				} catch (RuntimeException re) {
					// 听说过Android 4.0.x +中报告的RuntimeException; 继续？
					Log.w(TAG, "聚焦时出现异常", re);
					// 稍后重试以保持循环
					autoFocusAgainLater();
				}
			}
		}
	}

	/**
	 * 清除自动对焦任务
	 */
	private synchronized void cancelOutstandingTask() {
		if (outstandingTask != null) {
			if (outstandingTask.getStatus() != AsyncTask.Status.FINISHED) {
				outstandingTask.cancel(true);
			}
			outstandingTask = null;
		}
	}

	/**
	 * 停止自动对焦
	 */
	synchronized void stop() {
		stopped = true;
		if (useAutoFocus) {
			cancelOutstandingTask();
			// 如果没有聚焦，不执行下面步骤，也无关紧要
			try {
				camera.cancelAutoFocus();
			} catch (RuntimeException re) {
				// 听说过Android 4.0.x +中报告的RuntimeException; 继续？
				Log.w(TAG, "取消对焦时出现异常", re);
			}
		}
	}

	/**
	 * 自动对焦任务
	 * 
	 * @author lijian
	 * @date 2017-8-27 上午9:48:18
	 */
	private final class AutoFocusTask extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... voids) {
			try {
				Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
			} catch (InterruptedException e) {
				// continue
			}
			start();
			return null;
		}
	}

}
