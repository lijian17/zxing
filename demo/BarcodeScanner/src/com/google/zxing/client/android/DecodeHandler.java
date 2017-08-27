package com.google.zxing.client.android;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

/**
 * 解码处理器
 * 
 * @author lijian
 * @date 2017-8-27 下午4:41:36
 */
final class DecodeHandler extends Handler {
	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final CaptureActivity activity;
	/** 多格式阅读器 */
	private final MultiFormatReader multiFormatReader;
	/** 正在运行中 */
	private boolean running = true;

	/**
	 * 解码处理器
	 * 
	 * @param activity
	 * @param hints
	 *            设置要解码的类型
	 */
	DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message message) {
		if (!running) {
			return;
		}
		switch (message.what) {
		case R.id.decode:// 解码(循环解码通过CaptureActivityHandler的handleMessage循环调用)-相机管理器负责将对应数据传递过来(cameraManager.requestPreviewFrame)
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:// 通过CaptureActivityHandler.quitSynchronously()调用
			running = false;
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * 解码取景器矩形中的数据，以及花费多长时间。 为了效率，将相同的读取对象从一个解码重用到下一个解码。
	 * 
	 * @param data
	 *            YUV预览框中的数据
	 * @param width
	 *            预览框的宽度
	 * @param height
	 *            预览框的高度
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		Result rawResult = null;
		// 平面YUV亮度源
		PlanarYUVLuminanceSource source = activity.getCameraManager()
				.buildLuminanceSource(data, width, height);
		if (source != null) {
			// BinaryBitmap二进制位图;HybridBinarizer混合二值化
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				rawResult = multiFormatReader.decodeWithState(bitmap);
			} catch (ReaderException re) {
				// continue
			} finally {
				multiFormatReader.reset();
			}
		}

		Handler handler = activity.getHandler();
		if (rawResult != null) {
			// 为了安全起见，请勿记录、打印log条形码内容。
			long end = System.currentTimeMillis();
			Log.d(TAG, "找到条码 " + (end - start) + " ms");
			if (handler != null) {
				// 将扫码后得出的条码内容及条码图一并返回给展示层
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, rawResult);
				Bundle bundle = new Bundle();
				bundleThumbnail(source, bundle);
				message.setData(bundle);
				message.sendToTarget();
			}
		} else {
			if (handler != null) {
				Message message = Message.obtain(handler, R.id.decode_failed);
				message.sendToTarget();
			}
		}
	}

	/**
	 * 打包缩略图
	 * 
	 * @param source
	 *            源数据
	 * @param bundle
	 */
	private static void bundleThumbnail(PlanarYUVLuminanceSource source,
			Bundle bundle) {
		// 获得缩略图数据
		int[] pixels = source.renderThumbnail();
		int width = source.getThumbnailWidth();
		int height = source.getThumbnailHeight();
		Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height,
				Bitmap.Config.ARGB_8888);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		// boolean compress(Bitmap.CompressFormat format, int quality,
		// OutputStream stream)
		// 把位图的压缩信息写入到一个指定的输出流中。如果返回true，可以通过传递一个相应的输出流到BitmapFactory.decodeStream()来重构该位图。
		// 注意：并非所有的格式都直接支持位图结构，所以通过BitmapFactory返回的位图很可能有不同的位深度，或许会丢失每个象素的alpha值(例如，JPEG只支持不透明像素)。
		//
		// （译者注：色深(color
		// depth),也称色位深度(bitdePth),是指在一定分辨率下一个像素能够接受的颜色数量范围。通常,色深用2的n次方来表示。例如,8bit的色深包含2的8次方）
		//
		// 参数
		// format 图像的压缩格式；
		// quality 图像压缩比的值，0-100。
		// 0意味着小尺寸压缩，100意味着高质量压缩。对于有些格式，比如无损压缩的PNG，它就会忽视quality这个参数设置。
		// stream 写入压缩数据的输出流
		//
		// 返回值
		// 如果成功地把压缩数据写入输出流，则返回true。

		// 压缩
		bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
		bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
		bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width
				/ source.getWidth());
	}

}
