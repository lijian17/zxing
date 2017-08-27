package com.google.zxing.client.android.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.camera.open.OpenCamera;
import com.google.zxing.client.android.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
 * 相机管理器<br>
 * 该对象包装Camera服务对象，并期望是唯一一个与之对话的对象。 该实现封装了预览大小的图像所需的步骤，这些图像用于预览和解码。
 * 
 * @author lijian-pc
 * @date 2017-8-22 下午2:15:12
 */
public final class CameraManager {
	private static final String TAG = CameraManager.class.getSimpleName();

	/** 最小帧宽 */
	private static final int MIN_FRAME_WIDTH = 240;
	/** 最小帧高 */
	private static final int MIN_FRAME_HEIGHT = 240;
	/** 最大帧宽 */
	private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
	/** 最大帧高 */
	private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

	private final Context context;
	/** 相机配置管理器 */
	private final CameraConfigurationManager configManager;
	/** 一个打开的相机 */
	private OpenCamera camera;
	/** 自动对焦管理器 */
	private AutoFocusManager autoFocusManager;
	/** 预览矩形 */
	private Rect framingRect;
	/** 在预览框中构建预览矩形 */
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	/** 请求要打开的相机ID */
	private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
	private int requestedFramingRectWidth;
	private int requestedFramingRectHeight;

	/**
	 * 预览框架在这里传递，我们传递给注册的处理程序。 确保清除处理程序，因此它只会收到一条消息
	 */
	private final PreviewCallback previewCallback;

	/**
	 * 相机管理器
	 * 
	 * @param context
	 */
	public CameraManager(Context context) {
		this.context = context;
		this.configManager = new CameraConfigurationManager(context);
		previewCallback = new PreviewCallback(configManager);
	}

	/**
	 * 打开相机驱动程序并初始化硬件参数。
	 * 
	 * @param holder
	 *            相机将绘制预览框架的surface
	 * @throws IOException
	 *             表示相机驱动程序无法打开
	 */
	public synchronized void openDriver(SurfaceHolder holder)
			throws IOException {
		OpenCamera theCamera = camera;
		if (theCamera == null) {
			theCamera = OpenCameraInterface.open(requestedCameraId);
			if (theCamera == null) {
				throw new IOException("Camera.open() 无法从驱动程序返回对象");
			}
			camera = theCamera;
		}

		if (!initialized) {
			initialized = true;
			configManager.initFromCameraParameters(theCamera);
			if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
				setManualFramingRect(requestedFramingRectWidth,
						requestedFramingRectHeight);
				requestedFramingRectWidth = 0;
				requestedFramingRectHeight = 0;
			}
		}

		Camera cameraObject = theCamera.getCamera();
		Camera.Parameters parameters = cameraObject.getParameters();
		String parametersFlattened = parameters == null ? null : parameters
				.flatten(); // 保存这些，暂时
		try {
			configManager.setDesiredCameraParameters(theCamera, false);
		} catch (RuntimeException re) {
			// 驱动程序失败
			Log.w(TAG, "相机拒绝参数。 仅设置最小安全模式参数");
			Log.i(TAG, "重置为保存的相机参数: " + parametersFlattened);
			// 重启:
			if (parametersFlattened != null) {
				parameters = cameraObject.getParameters();
				parameters.unflatten(parametersFlattened);
				try {
					cameraObject.setParameters(parameters);
					configManager.setDesiredCameraParameters(theCamera, true);
				} catch (RuntimeException re2) {
					// 好吧， 放弃
					Log.w(TAG, "相机甚至拒绝安全模式参数！ 无配置");
				}
			}
		}
		cameraObject.setPreviewDisplay(holder);

	}

	/**
	 * 相机是打开吗
	 * 
	 * @return
	 */
	public synchronized boolean isOpen() {
		return camera != null;
	}

	/**
	 * 关闭相机驱动程序，如果仍然在使用
	 */
	public synchronized void closeDriver() {
		if (camera != null) {
			camera.getCamera().release();
			camera = null;
			// 确保在每次关闭相机时清除这些信息，以便意图请求的任何扫描直方图被遗忘。
			framingRect = null;
			framingRectInPreview = null;
		}
	}

	/**
	 * 请求相机硬件开始将预览画面绘制到屏幕上
	 */
	public synchronized void startPreview() {
		OpenCamera theCamera = camera;
		if (theCamera != null && !previewing) {
			theCamera.getCamera().startPreview();
			previewing = true;
			autoFocusManager = new AutoFocusManager(context,
					theCamera.getCamera());
		}
	}

	/**
	 * 告诉相机停止绘制预览画面.
	 */
	public synchronized void stopPreview() {
		if (autoFocusManager != null) {
			autoFocusManager.stop();
			autoFocusManager = null;
		}
		if (camera != null && previewing) {
			camera.getCamera().stopPreview();
			previewCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	/**
	 * 方便方法 {@link com.google.zxing.client.android.CaptureActivity}
	 * 
	 * @param newSetting
	 *            true:开启闪光灯；false:关闭闪光灯
	 */
	public synchronized void setTorch(boolean newSetting) {
		OpenCamera theCamera = camera;
		if (theCamera != null) {
			if (newSetting != configManager
					.getTorchState(theCamera.getCamera())) {
				boolean wasAutoFocusManager = autoFocusManager != null;
				if (wasAutoFocusManager) {
					autoFocusManager.stop();
					autoFocusManager = null;
				}
				configManager.setTorch(theCamera.getCamera(), newSetting);
				if (wasAutoFocusManager) {
					autoFocusManager = new AutoFocusManager(context,
							theCamera.getCamera());
					autoFocusManager.start();
				}
			}
		}
	}

	/**
	 * 单个预览框将返回给提供的处理程序。
	 * 数据将在message.obj字段中作为byte[]到达，宽度和高度分别编码为message.arg1和message.arg2。
	 * 
	 * @param handler
	 *            发送消息的处理程序
	 * @param message
	 *            要发送的消息的ID
	 */
	public synchronized void requestPreviewFrame(Handler handler, int message) {
		OpenCamera theCamera = camera;
		if (theCamera != null && previewing) {
			previewCallback.setHandler(handler, message);
			theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
		}
	}

	/**
	 * 计算UI应绘制的框架矩形，以向用户显示放置条形码的位置。 该目标有助于对齐，并强制用户将设备保持足够远，以确保图像处于关注状态。
	 * 
	 * @return 在窗口坐标中画屏幕的矩形。
	 */
	public synchronized Rect getFramingRect() {
		if (framingRect == null) {
			if (camera == null) {
				return null;
			}
			Point screenResolution = configManager.getScreenResolution();
			if (screenResolution == null) {
				// 早点调用，在init之前完成
				return null;
			}

			int width = findDesiredDimensionInRange(screenResolution.x,
					MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
			int height = findDesiredDimensionInRange(screenResolution.y,
					MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			Log.d(TAG, "计算框架矩形: " + framingRect);
		}
		return framingRect;
	}

	/**
	 * 找到想要的尺寸在范围内
	 * 
	 * @param resolution
	 * @param hardMin
	 * @param hardMax
	 * @return
	 */
	private static int findDesiredDimensionInRange(int resolution, int hardMin,
			int hardMax) {
		int dim = 5 * resolution / 8; // Target 5/8 of each dimension
		if (dim < hardMin) {
			return hardMin;
		}
		if (dim > hardMax) {
			return hardMax;
		}
		return dim;
	}

	/**
	 * 类似{@link #getFramingRect}，但是坐标是在预览框，而不是UI/屏幕。
	 * 
	 * @return {@link Rect}表示条形码扫描区域的预览大小
	 */
	public synchronized Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect framingRect = getFramingRect();
			if (framingRect == null) {
				return null;
			}
			Rect rect = new Rect(framingRect);
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();
			if (cameraResolution == null || screenResolution == null) {
				// 早点调用，在init之前完成
				return null;
			}
			rect.left = rect.left * cameraResolution.x / screenResolution.x;
			rect.right = rect.right * cameraResolution.x / screenResolution.x;
			rect.top = rect.top * cameraResolution.y / screenResolution.y;
			rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

	/**
	 * 允许第三方应用程序指定摄像机ID，而不是根据可用摄像头及其方向自动确定摄像机ID。
	 * 
	 * @param cameraId
	 *            要使用的相机的相机ID。 负值表示“无偏好”。
	 */
	public synchronized void setManualCameraId(int cameraId) {
		requestedCameraId = cameraId;
	}

	/**
	 * 允许第三方应用程序指定扫描矩形尺寸，而不是根据屏幕分辨率自动确定它们。
	 * 
	 * @param width
	 *            要扫描的像素宽度。
	 * @param height
	 *            要扫描的像素高度。
	 */
	public synchronized void setManualFramingRect(int width, int height) {
		if (initialized) {
			Point screenResolution = configManager.getScreenResolution();
			if (width > screenResolution.x) {
				width = screenResolution.x;
			}
			if (height > screenResolution.y) {
				height = screenResolution.y;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			Log.d(TAG, "计算手动框架: " + framingRect);
			framingRectInPreview = null;
		} else {
			requestedFramingRectWidth = width;
			requestedFramingRectHeight = height;
		}
	}

	/**
	 * 基于Camera.Parameters描述的基于预览缓冲区的格式的工厂方法来构建适当的LuminanceSource对象。
	 * 
	 * @param data
	 *            预览框
	 * @param width
	 *            图像的宽度
	 * @param height
	 *            图像的高度
	 * @return 一个PlanarYUVLuminanceSource实例
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Rect rect = getFramingRectInPreview();
		if (rect == null) {
			return null;
		}
		// 继续，假设它是YUV而不是死亡。
		return new PlanarYUVLuminanceSource(data, width, height, rect.left,
				rect.top, rect.width(), rect.height(), false);
	}

}
