package com.google.zxing.client.android.result;

import android.view.View;

/**
 * 在Android平台的上下文中处理条形码解码的结果，通过发送正确的意图来打开其他活动，如GMail，Maps等
 * 
 * @author lijian
 * @date 2017-9-7 下午10:18:42
 */
public final class ResultButtonListener implements View.OnClickListener {
	private final ResultHandler resultHandler;
	private final int index;

	public ResultButtonListener(ResultHandler resultHandler, int index) {
		this.resultHandler = resultHandler;
		this.index = index;
	}

	@Override
	public void onClick(View view) {
		resultHandler.handleButtonPress(index);
	}
}
