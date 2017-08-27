package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * 取景器结果点回调
 * 
 * @author lijian
 * @date 2017-8-27 下午5:51:11
 */
final class ViewfinderResultPointCallback implements ResultPointCallback {

	private final ViewfinderView viewfinderView;

	/**
	 * 取景器结果点回调
	 * 
	 * @param viewfinderView
	 *            取景器对象
	 */
	ViewfinderResultPointCallback(ViewfinderView viewfinderView) {
		this.viewfinderView = viewfinderView;
	}

	// 找到可能的结果点
	@Override
	public void foundPossibleResultPoint(ResultPoint point) {
		// 将结果点绘制在取景器上
		viewfinderView.addPossibleResultPoint(point);
	}

}
