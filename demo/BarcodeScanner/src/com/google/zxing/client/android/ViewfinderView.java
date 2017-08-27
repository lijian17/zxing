package com.google.zxing.client.android;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * 取景器视图<br>
 * 该视图覆盖在相机预览的顶部。 它将取景器矩形和部分透明度添加到外部，以及激光扫描仪的动画和结果点。
 * 
 * @author lijian
 * @date 2017-8-27 上午11:48:44
 */
public final class ViewfinderView extends View {

	/** 扫描仪alpha */
	private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
			128, 64 };
	/** 动画延迟 */
	private static final long ANIMATION_DELAY = 80L;
	/** 当前点不透明度 */
	private static final int CURRENT_POINT_OPACITY = 0xA0;
	/** 最大结果点 */
	private static final int MAX_RESULT_POINTS = 20;
	/** 点大小 */
	private static final int POINT_SIZE = 6;

	/** 相机管理器 */
	private CameraManager cameraManager;
	private final Paint paint;
	private Bitmap resultBitmap;
	/** 面具的颜色 */
	private final int maskColor;
	/** 结果的颜色 */
	private final int resultColor;
	/** 激光的颜色 */
	private final int laserColor;
	/** 结果点的颜色 */
	private final int resultPointColor;
	private int scannerAlpha;
	private List<ResultPoint> possibleResultPoints;
	private List<ResultPoint> lastPossibleResultPoints;

	/**
	 * 当从XML资源构建类时使用此构造函数。
	 * 
	 * @param context
	 * @param attrs
	 */
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// 在这里做初始化，而不是在onDraw()中，以保证性能
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
		possibleResultPoints = new ArrayList<ResultPoint>(5);
		lastPossibleResultPoints = null;
	}

	/**
	 * 设置相机管理器
	 * 
	 * @param cameraManager
	 */
	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // 尚未准备好，在完成配置之前提前绘制
		}
		Rect frame = cameraManager.getFramingRect();
		Rect previewFrame = cameraManager.getFramingRectInPreview();
		if (frame == null || previewFrame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// 绘制外部（即框架直角外侧）变暗
		paint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		if (resultBitmap != null) {
			// 在扫描矩形上绘制不透明的结果位图
			paint.setAlpha(CURRENT_POINT_OPACITY);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {
			// 画一个红色的“激光扫描仪”线通过中间显示解码是活跃的
			paint.setColor(laserColor);
			paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
			scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
			int middle = frame.height() / 2 + frame.top;
			canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1,
					middle + 2, paint);

			float scaleX = frame.width() / (float) previewFrame.width();
			float scaleY = frame.height() / (float) previewFrame.height();

			List<ResultPoint> currentPossible = possibleResultPoints;
			List<ResultPoint> currentLast = lastPossibleResultPoints;
			int frameLeft = frame.left;
			int frameTop = frame.top;
			if (currentPossible.isEmpty()) {
				lastPossibleResultPoints = null;
			} else {
				possibleResultPoints = new ArrayList<ResultPoint>(5);
				lastPossibleResultPoints = currentPossible;
				paint.setAlpha(CURRENT_POINT_OPACITY);
				paint.setColor(resultPointColor);
				synchronized (currentPossible) {
					for (ResultPoint point : currentPossible) {
						canvas.drawCircle(frameLeft
								+ (int) (point.getX() * scaleX), frameTop
								+ (int) (point.getY() * scaleY), POINT_SIZE,
								paint);
					}
				}
			}
			if (currentLast != null) {
				paint.setAlpha(CURRENT_POINT_OPACITY / 2);
				paint.setColor(resultPointColor);
				synchronized (currentLast) {
					float radius = POINT_SIZE / 2.0f;
					for (ResultPoint point : currentLast) {
						canvas.drawCircle(frameLeft
								+ (int) (point.getX() * scaleX), frameTop
								+ (int) (point.getY() * scaleY), radius, paint);
					}
				}
			}

			// 请求动画间隔更新，但只能重新绘制激光线，而不是整个取景器遮罩。
			postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE,
					frame.top - POINT_SIZE, frame.right + POINT_SIZE,
					frame.bottom + POINT_SIZE);
		}
	}

	/**
	 * 绘制取景器
	 */
	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		// 使整个视图无效。 如果视图可见，将来会在某个时候调用onDraw(android.graphics.Canvas)。
		// 这必须从UI线程调用。 要从非UI线程调用，请调用postInvalidate()。
		invalidate();
	}

	/**
	 * 绘制一个位图，结果点突出显示，而不是实时扫描显示。
	 * 
	 * @param barcode
	 *            解码条形码的图像。
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	/**
	 * 添加可能的结果点
	 * 
	 * @param point
	 *            结果点
	 */
	public void addPossibleResultPoint(ResultPoint point) {
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// 修剪它
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}

}
