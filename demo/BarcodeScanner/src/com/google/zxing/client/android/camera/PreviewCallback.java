package com.google.zxing.client.android.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 相机预览回调
 * 
 * @author lijian
 * @date 2017-8-27 上午10:13:43
 */
final class PreviewCallback implements Camera.PreviewCallback {
	private static final String TAG = PreviewCallback.class.getSimpleName();

	/** 相机配置管理器 */
	private final CameraConfigurationManager configManager;
	private Handler previewHandler;
	private int previewMessage;

	/**
	 * 相机预览回调
	 * 
	 * @param configManager
	 */
	PreviewCallback(CameraConfigurationManager configManager) {
		this.configManager = configManager;
	}

	/**
	 * 设置相机预览Handler
	 * 
	 * @param previewHandler
	 *            预览Handler
	 * 
	 * @param previewMessage
	 *            预览消息ID
	 */
	void setHandler(Handler previewHandler, int previewMessage) {
		this.previewHandler = previewHandler;
		this.previewMessage = previewMessage;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Point cameraResolution = configManager.getCameraResolution();
		Handler thePreviewHandler = previewHandler;
		if (cameraResolution != null && thePreviewHandler != null) {
			Message message = thePreviewHandler.obtainMessage(previewMessage,
					cameraResolution.x, cameraResolution.y, data);
			message.sendToTarget();
			previewHandler = null;
		} else {
			Log.d(TAG, "有预览回调，但没有处理程序或分辨率可用");
		}
	}

}
