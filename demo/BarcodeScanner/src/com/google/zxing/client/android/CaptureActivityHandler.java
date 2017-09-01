package com.google.zxing.client.android;

import java.util.Collection;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * CaptureActivity的处理器
 * 
 * @author lijian
 * @date 2017-8-27 下午4:09:17
 */
public final class CaptureActivityHandler extends Handler {
	private static final String TAG = CaptureActivityHandler.class
			.getSimpleName();

	private final CaptureActivity activity;
	private final DecodeThread decodeThread;
	private State state;
	private final CameraManager cameraManager;

	private enum State {
		/** 相机预览 */
		PREVIEW,
		/** 成功 */
		SUCCESS,
		/** 完成 */
		DONE
	}

	/**
	 * 
	 * @param activity
	 *            CaptureActivity实例
	 * @param decodeFormats
	 *            解码格式集
	 * @param baseHints
	 *            解码Hints
	 * @param characterSet
	 *            解码字符集
	 * @param cameraManager
	 *            相机管理器
	 */
	CaptureActivityHandler(CaptureActivity activity,
			Collection<BarcodeFormat> decodeFormats,
			Map<DecodeHintType, ?> baseHints, String characterSet,
			CameraManager cameraManager) {
		this.activity = activity;
		decodeThread = new DecodeThread(activity, decodeFormats, baseHints,
				characterSet, new ViewfinderResultPointCallback(
						activity.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		// 开始捕捉预览和解码
		this.cameraManager = cameraManager;
		cameraManager.startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.restart_preview:// 重启预览
			restartPreviewAndDecode();
			break;
		case R.id.decode_succeeded:// 解码成功
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			Bitmap barcode = null;
			float scaleFactor = 1.0f;
			if (bundle != null) {
				byte[] compressedBitmap = bundle
						.getByteArray(DecodeThread.BARCODE_BITMAP);
				if (compressedBitmap != null) {
					barcode = BitmapFactory.decodeByteArray(compressedBitmap,
							0, compressedBitmap.length, null);
					// Mutable copy:主要目的是将该bitmap设置为isMutable=true即他的像素可被修改
					barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
				}
				scaleFactor = bundle
						.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
			}
			activity.handleDecode((Result) message.obj, barcode, scaleFactor);
			break;
		case R.id.decode_failed:// 解码失败
			// 我们尽可能快地进行解码，所以当一个解码失败时，启动另一个解码。
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
			break;
		case R.id.return_scan_result:// 返回扫码结果
			activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
			activity.finish();
			break;
		case R.id.launch_product_query:// 发射产品查询
			String url = (String) message.obj;

			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setData(Uri.parse(url));

			ResolveInfo resolveInfo = activity.getPackageManager()
					.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
			String browserPackageName = null;
			if (resolveInfo != null && resolveInfo.activityInfo != null) {
				browserPackageName = resolveInfo.activityInfo.packageName;
				Log.d(TAG, "在package中使用浏览器 " + browserPackageName);
			}

			// 只需要默认Android浏览器/Chrome才可以
			if ("com.android.browser".equals(browserPackageName)
					|| "com.android.chrome".equals(browserPackageName)) {
				intent.setPackage(browserPackageName);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(Browser.EXTRA_APPLICATION_ID,
						browserPackageName);
			}

			try {
				activity.startActivity(intent);
			} catch (ActivityNotFoundException ignored) {
				Log.w(TAG, "找不到任何可以处理URI的VIEW的东西 " + url);
			}
			break;
		}
	}

	/**
	 * 同时退出
	 */
	public void quitSynchronously() {
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			// 等待最多半秒钟; 应该有足够的时间，onPause()会很快超时
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// 绝对确定我们不会发送任何排队的消息
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	/**
	 * 重新启动预览和解码
	 */
	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
			activity.drawViewfinder();
		}
	}

}
