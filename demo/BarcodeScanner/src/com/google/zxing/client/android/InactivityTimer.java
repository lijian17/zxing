package com.google.zxing.client.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

/**
 * 闲置计时器<br>
 * 如果设备处于电池供电状态，请在一段时间不活动后完成活动。
 */
final class InactivityTimer {
  private static final String TAG = InactivityTimer.class.getSimpleName();

  /** 闲置计数时间（5分钟后执行） */
  private static final long INACTIVITY_DELAY_MS = 5 * 60 * 1000L;

  private final Activity activity;
  private final BroadcastReceiver powerStatusReceiver;
  private boolean registered;
  private AsyncTask<Object,Object,Object> inactivityTask;

  InactivityTimer(Activity activity) {
    this.activity = activity;
    powerStatusReceiver = new PowerStatusReceiver();
    registered = false;
    onActivity();
  }

  /**
   * 活动状态（创建后台监听线程并运行）
   */
  synchronized void onActivity() {
    cancel();
    inactivityTask = new InactivityAsyncTask();
    inactivityTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  /**
   * 失去焦点，注销电池监听器
   */
  synchronized void onPause() {
    cancel();
    if (registered) {
      activity.unregisterReceiver(powerStatusReceiver);
      registered = false;
    } else {
      Log.w(TAG, "PowerStatusReceiver was never registered?");
    }
  }

  /**
   * 获得焦点时，注册电池监听器
   */
  synchronized void onResume() {
    if (registered) {
      Log.w(TAG, "PowerStatusReceiver was already registered?");
    } else {
      activity.registerReceiver(powerStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
      registered = true;
    }
    onActivity();
  }

  /**
   * 关闭并销毁计数器线程
   */
  private synchronized void cancel() {
    AsyncTask<?,?,?> task = inactivityTask;
    if (task != null) {
      task.cancel(true);
      inactivityTask = null;
    }
  }

  /**
   * 关闭
   */
  void shutdown() {
    cancel();
  }

  /**
   * 电源状态接收器
   * 
   * @author lijian
   * @date 2017-8-14 下午10:51:54
   */
  private final class PowerStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
        // 0表示我们正在使用电池
        boolean onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0;
        if (onBatteryNow) {
          InactivityTimer.this.onActivity();
        } else {
          InactivityTimer.this.cancel();
        }
      }
    }
  }

  private final class InactivityAsyncTask extends AsyncTask<Object,Object,Object> {
    @Override
    protected Object doInBackground(Object... objects) {
      try {
        Thread.sleep(INACTIVITY_DELAY_MS);
        Log.i(TAG, "Finishing activity due to inactivity");
        activity.finish();
      } catch (InterruptedException e) {
        // continue without killing
      }
      return null;
    }
  }

}
