package com.google.zxing.client.android;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;

/**
 * 解码线程<br>
 * 这个线程完成了对图像进行解码的所有繁重的工作。
 * 
 * @author lijian
 * @date 2017-8-27 下午4:10:51
 */
final class DecodeThread extends Thread {

	/** 条码位图 */
	public static final String BARCODE_BITMAP = "barcode_bitmap";
	/** 条码缩放因子 */
	public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

	private final CaptureActivity activity;
	private final Map<DecodeHintType, Object> hints;
	private Handler handler;
	/**
	 * CountDownLatch类是一个同步计数器,构造时传入int参数,该参数就是计数器的初始值，每调用一次countDown()方法，计数器减1,
	 * 计数器大于0 时，await()方法会阻塞程序继续执行<br>
	 * CountDownLatch如其所写，是一个倒计数的锁存器，当计数减至0时触发特定的事件。利用这种特性 ，可以让主线程等待子线程的结束。
	 */
	private final CountDownLatch handlerInitLatch;

	/**
	 * 解码线程
	 * 
	 * @param activity CaptureActivity对象
	 * @param decodeFormats 解码要支持的解码格式化器（一维工业条码、一维商品条码、QR二维码、PDF417二维码等）
	 * @param baseHints 解码器参数基础参数设置器
	 * @param characterSet 解码字符集
	 * @param resultPointCallback 扫描结果点回调
	 */
	DecodeThread(CaptureActivity activity,
			Collection<BarcodeFormat> decodeFormats,
			Map<DecodeHintType, ?> baseHints, String characterSet,
			ResultPointCallback resultPointCallback) {

		this.activity = activity;
		handlerInitLatch = new CountDownLatch(1);

		// 什么是EnumMap
		// Map接口的实现，其key-value映射中的key是Enum类型；
		// 其原理就是一个对象数组，数组的下标索引就是根据Map中的key直接获取，即枚举中的ordinal值；
		// 效率比HashMap高，可以直接获取数组下标索引并访问到元素；
		hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		if (baseHints != null) {
			hints.putAll(baseHints);
		}

		// 线程运行时，prefs不能更改，因此在此处接收它们
		if (decodeFormats == null || decodeFormats.isEmpty()) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(activity);
			decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D_PRODUCT,
					true)) {
				decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
			}
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL,
					true)) {
				decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
			}
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_QR, true)) {
				decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
			}
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_DATA_MATRIX,
					true)) {
				decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
			}
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_AZTEC, false)) {
				decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
			}
			if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_PDF417, false)) {
				decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
			}
		}
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

		if (characterSet != null) {
			hints.put(DecodeHintType.CHARACTER_SET, characterSet);
		}
		hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK,
				resultPointCallback);
		Log.i("DecodeThread", "Hints: " + hints);
	}

	Handler getHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
			// 继续?
		}
		return handler;
	}

	@Override
	public void run() {
		Looper.prepare();
		handler = new DecodeHandler(activity, hints);
		handlerInitLatch.countDown();
		Looper.loop();
	}

}
