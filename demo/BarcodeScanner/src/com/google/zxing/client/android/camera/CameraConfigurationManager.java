package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.camera.open.CameraFacing;
import com.google.zxing.client.android.camera.open.OpenCamera;

/**
 * 相机配置管理器<br>
 * 处理读取，解析和设置用于配置摄像机硬件的摄像机参数的类。
 * 
 * @author lijian-pc
 * @date 2017-8-22 下午2:36:14
 */
final class CameraConfigurationManager {
	private static final String TAG = "CameraConfiguration";

	private final Context context;
	/** 需要旋转 */
	private int cwNeededRotation;
	/** 从显示到相机的旋转 */
	private int cwRotationFromDisplayToCamera;
	/** 屏幕分辨率 */
	private Point screenResolution;
	/** 相机分辨率 */
	private Point cameraResolution;
	private Point bestPreviewSize;
	private Point previewSizeOnScreen;

	CameraConfigurationManager(Context context) {
		this.context = context;
	}

	/**
	 * 一次性读取相机所需的应用程序的值。
	 */
	void initFromCameraParameters(OpenCamera camera) {
		Camera.Parameters parameters = camera.getCamera().getParameters();
		// android获得屏幕高度和宽度（display.getSize(Point)）
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();

		// 屏幕的旋转角度
		int displayRotation = display.getRotation();
		int cwRotationFromNaturalToDisplay;
		switch (displayRotation) {
		case Surface.ROTATION_0:
			cwRotationFromNaturalToDisplay = 0;
			break;
		case Surface.ROTATION_90:
			cwRotationFromNaturalToDisplay = 90;
			break;
		case Surface.ROTATION_180:
			cwRotationFromNaturalToDisplay = 180;
			break;
		case Surface.ROTATION_270:
			cwRotationFromNaturalToDisplay = 270;
			break;
		default:
			// 看到这个返回不正确的值，如-90
			if (displayRotation % 90 == 0) {
				cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
			} else {
				throw new IllegalArgumentException("Bad rotation: "
						+ displayRotation);
			}
		}
		Log.i(TAG, "显示在: " + cwRotationFromNaturalToDisplay);

		// 屏幕的方向
		int cwRotationFromNaturalToCamera = camera.getOrientation();
		Log.i(TAG, "相机在: " + cwRotationFromNaturalToCamera);

		// 仍然不能100%肯定这个。 但是我们需要这样做的行为：
		if (camera.getFacing() == CameraFacing.FRONT) {
			cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
			Log.i(TAG, "前置摄像机覆盖到: " + cwRotationFromNaturalToCamera);
		}

		/*
		 * SharedPreferences prefs =
		 * PreferenceManager.getDefaultSharedPreferences(context); String
		 * overrideRotationString; if (camera.getFacing() == CameraFacing.FRONT)
		 * { overrideRotationString =
		 * prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION_FRONT
		 * , null); } else { overrideRotationString =
		 * prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION,
		 * null); } if (overrideRotationString != null &&
		 * !"-".equals(overrideRotationString)) { Log.i(TAG,
		 * "Overriding camera manually to " + overrideRotationString);
		 * cwRotationFromNaturalToCamera =
		 * Integer.parseInt(overrideRotationString); }
		 */

		cwRotationFromDisplayToCamera = (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
		Log.i(TAG, "最终显示方向: " + cwRotationFromDisplayToCamera);
		// 如果是正面
		if (camera.getFacing() == CameraFacing.FRONT) {
			Log.i(TAG, "补偿前置摄像头的旋转");
			cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
		} else {
			cwNeededRotation = cwRotationFromDisplayToCamera;
		}
		Log.i(TAG, "从显示器到相机顺时针旋转: " + cwNeededRotation);

		Point theScreenResolution = new Point();
		display.getSize(theScreenResolution);
		screenResolution = theScreenResolution;
		Log.i(TAG, "当前方向的屏幕分辨率: " + screenResolution);
		cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(
				parameters, screenResolution);
		Log.i(TAG, "相机分辨率: " + cameraResolution);
		bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(
				parameters, screenResolution);
		Log.i(TAG, "最佳可用预览大小: " + bestPreviewSize);

		boolean isScreenPortrait = screenResolution.x < screenResolution.y;
		boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

		if (isScreenPortrait == isPreviewSizePortrait) {
			previewSizeOnScreen = bestPreviewSize;
		} else {
			previewSizeOnScreen = new Point(bestPreviewSize.y,
					bestPreviewSize.x);
		}
		Log.i(TAG, "屏幕上的预览大小: " + previewSizeOnScreen);
	}

	/**
	 * 设置所需的相机参数
	 * 
	 * @param camera
	 *            一个打开的相机
	 * @param safeMode
	 *            安全模式
	 */
	void setDesiredCameraParameters(OpenCamera camera, boolean safeMode) {

		Camera theCamera = camera.getCamera();
		Camera.Parameters parameters = theCamera.getParameters();

		if (parameters == null) {
			Log.w(TAG, "设备错误：没有相机参数可用。 没有配置");
			return;
		}

		Log.i(TAG, "初始化摄像机参数: " + parameters.flatten());

		if (safeMode) {
			Log.w(TAG, "在相机配置安全模式下 - 大多数设置将无法保证");
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		initializeTorch(parameters, prefs, safeMode);

		CameraConfigurationUtils.setFocus(parameters, prefs.getBoolean(
				PreferencesActivity.KEY_AUTO_FOCUS, true), prefs.getBoolean(
				PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, true),
				safeMode);

		if (!safeMode) {
			if (prefs.getBoolean(PreferencesActivity.KEY_INVERT_SCAN, false)) {
				CameraConfigurationUtils.setInvertColor(parameters);
			}

			if (!prefs.getBoolean(
					PreferencesActivity.KEY_DISABLE_BARCODE_SCENE_MODE, true)) {
				CameraConfigurationUtils.setBarcodeSceneMode(parameters);
			}

			if (!prefs.getBoolean(PreferencesActivity.KEY_DISABLE_METERING,
					true)) {
				CameraConfigurationUtils.setVideoStabilization(parameters);
				CameraConfigurationUtils.setFocusArea(parameters);
				CameraConfigurationUtils.setMetering(parameters);
			}

		}

		parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

		theCamera.setParameters(parameters);

		theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

		Camera.Parameters afterParameters = theCamera.getParameters();
		Camera.Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null
				&& (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
			Log.w(TAG, "相机表示支持预览大小 " + bestPreviewSize.x + 'x'
					+ bestPreviewSize.y + ", 但经过设置，预览大小是 " + afterSize.width
					+ 'x' + afterSize.height);
			bestPreviewSize.x = afterSize.width;
			bestPreviewSize.y = afterSize.height;
		}
	}

	/**
	 * 获取最佳预览大小
	 * 
	 * @return
	 */
	Point getBestPreviewSize() {
		return bestPreviewSize;
	}

	/**
	 * 获得屏幕上的预览大小
	 * 
	 * @return
	 */
	Point getPreviewSizeOnScreen() {
		return previewSizeOnScreen;
	}

	/**
	 * 获取相机分辨率
	 * 
	 * @return
	 */
	Point getCameraResolution() {
		return cameraResolution;
	}

	/**
	 * 获得屏幕分辨率
	 * 
	 * @return
	 */
	Point getScreenResolution() {
		return screenResolution;
	}

	/**
	 * 获得CW需要旋转
	 * 
	 * @return
	 */
	int getCWNeededRotation() {
		return cwNeededRotation;
	}

	/**
	 * 得到闪光灯状态
	 * 
	 * @param camera
	 *            相机
	 * @return
	 */
	boolean getTorchState(Camera camera) {
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			if (parameters != null) {
				String flashMode = parameters.getFlashMode();
				return flashMode != null
						&& (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) || Camera.Parameters.FLASH_MODE_TORCH
								.equals(flashMode));
			}
		}
		return false;
	}

	/**
	 * 设置闪光灯
	 * 
	 * @param camera
	 *            相机
	 * @param newSetting
	 */
	void setTorch(Camera camera, boolean newSetting) {
		Camera.Parameters parameters = camera.getParameters();
		doSetTorch(parameters, newSetting, false);
		camera.setParameters(parameters);
	}

	/**
	 * 初始化闪光灯
	 * 
	 * @param parameters
	 *            相机参数
	 * @param prefs
	 *            本地sp
	 * @param safeMode
	 *            安全模式
	 */
	private void initializeTorch(Camera.Parameters parameters,
			SharedPreferences prefs, boolean safeMode) {
		boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
		doSetTorch(parameters, currentSetting, safeMode);
	}

	/**
	 * 执行设置闪光灯参数
	 * 
	 * @param parameters
	 *            相机参数
	 * @param newSetting
	 *            是否开启闪光灯
	 * @param safeMode
	 *            安全模式
	 */
	private void doSetTorch(Camera.Parameters parameters, boolean newSetting,
			boolean safeMode) {
		CameraConfigurationUtils.setTorch(parameters, newSetting);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (!safeMode
				&& !prefs.getBoolean(PreferencesActivity.KEY_DISABLE_EXPOSURE,
						true)) {
			CameraConfigurationUtils.setBestExposure(parameters, newSetting);
		}
	}

}
