package com.google.zxing.client.android;

import android.app.Activity;
import android.content.DialogInterface;

/**
 * 简单的监听器用于在几种情况下退出应用程序
 * 
 * @author lijian
 * @date 2017-9-3 下午6:27:24
 */
public final class FinishListener implements DialogInterface.OnClickListener,
		DialogInterface.OnCancelListener {

	private final Activity activityToFinish;

	public FinishListener(Activity activityToFinish) {
		this.activityToFinish = activityToFinish;
	}

	@Override
	public void onCancel(DialogInterface dialogInterface) {
		run();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		run();
	}

	private void run() {
		activityToFinish.finish();
	}

}
