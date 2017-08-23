package com.google.zxing.client.android.camera;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 配置Android相机的工具类
 * 
 * @author lijian-pc
 * @date 2017-8-22 下午6:09:14
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public final class CameraConfigurationUtils {
	private static final String TAG = "CameraConfiguration";

	/** 分号(;) */
	private static final Pattern SEMICOLON = Pattern.compile(";");

	/** 最小预览像素 */
	private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
	/** 最大曝光补偿 */
	private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
	/** 最小曝光补偿 */
	private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
	/** 最大外形失真 */
	private static final double MAX_ASPECT_DISTORTION = 0.15;
	/** 最小帧频 */
	private static final int MIN_FPS = 10;
	/** 最大帧频 */
	private static final int MAX_FPS = 20;
	/** 面积单位 */
	private static final int AREA_PER_1000 = 400;

	/**
	 * 相机配置工具类
	 */
	private CameraConfigurationUtils() {
	}

	/**
	 * 
	 * @param parameters
	 *            相机参数
	 * @param autoFocus
	 *            是否自动对焦
	 * @param disableContinuous
	 *            禁用连续
	 * @param safeMode
	 *            安全模式
	 */
	public static void setFocus(Camera.Parameters parameters,
			boolean autoFocus, boolean disableContinuous, boolean safeMode) {
		// 支持的对焦模式
		List<String> supportedFocusModes = parameters.getSupportedFocusModes();
		String focusMode = null;
		// 如果是自动对焦
		if (autoFocus) {
			// 安全模式 或者 禁用连续
			if (safeMode || disableContinuous) {
				focusMode = findSettableValue("对焦模式", supportedFocusModes,
						Camera.Parameters.FOCUS_MODE_AUTO);
			} else {
				focusMode = findSettableValue("对焦模式", supportedFocusModes,
						Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,// 连续画面
						Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,// 连续视频
						Camera.Parameters.FOCUS_MODE_AUTO);// 自动对焦
			}
		}
		// 也许选择自动对焦但不可用，因此可以在这里：
		if (!safeMode && focusMode == null) {
			focusMode = findSettableValue("对焦模式", supportedFocusModes,
					Camera.Parameters.FOCUS_MODE_MACRO,
					Camera.Parameters.FOCUS_MODE_EDOF);
		}
		if (focusMode != null) {
			if (focusMode.equals(parameters.getFocusMode())) {
				Log.i(TAG, "对焦模式已设置为 " + focusMode);
			} else {
				parameters.setFocusMode(focusMode);
			}
		}
	}

	/**
	 * 设置闪光灯
	 * 
	 * @param parameters
	 *            相机参数
	 * @param on
	 *            是否开启
	 */
	public static void setTorch(Camera.Parameters parameters, boolean on) {
		List<String> supportedFlashModes = parameters.getSupportedFlashModes();
		String flashMode;
		if (on) {
			flashMode = findSettableValue("闪光模式", supportedFlashModes,
					Camera.Parameters.FLASH_MODE_TORCH,
					Camera.Parameters.FLASH_MODE_ON);
		} else {
			flashMode = findSettableValue("闪光模式", supportedFlashModes,
					Camera.Parameters.FLASH_MODE_OFF);
		}
		if (flashMode != null) {
			if (flashMode.equals(parameters.getFlashMode())) {
				Log.i(TAG, "闪光模式已设置为 " + flashMode);
			} else {
				Log.i(TAG, "设置闪光模式为 " + flashMode);
				parameters.setFlashMode(flashMode);
			}
		}
	}

	/**
	 * 设置最佳曝光
	 * 
	 * @param parameters
	 *            相机参数
	 * @param lightOn
	 *            是否开启闪光灯
	 */
	public static void setBestExposure(Camera.Parameters parameters,
			boolean lightOn) {
		// 获得最小曝光补偿指数。
		int minExposure = parameters.getMinExposureCompensation();
		// 获得最大曝光补偿指数。
		int maxExposure = parameters.getMaxExposureCompensation();
		// 获取曝光补偿步长
		float step = parameters.getExposureCompensationStep();
		if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
			// 指示灯亮时设为低电平
			float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION
					: MAX_EXPOSURE_COMPENSATION;
			int compensationSteps = Math.round(targetCompensation / step);
			float actualCompensation = step * compensationSteps;
			// Clamp value:
			compensationSteps = Math.max(
					Math.min(compensationSteps, maxExposure), minExposure);
			if (parameters.getExposureCompensation() == compensationSteps) {
				Log.i(TAG, "曝光补偿已设定为 " + compensationSteps + " / "
						+ actualCompensation);
			} else {
				Log.i(TAG, "设置曝光补偿 " + compensationSteps + " / "
						+ actualCompensation);
				parameters.setExposureCompensation(compensationSteps);
			}
		} else {
			Log.i(TAG, "相机不支持曝光补偿");
		}
	}

	/**
	 * 设置最佳预览FPS
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setBestPreviewFPS(Camera.Parameters parameters) {
		setBestPreviewFPS(parameters, MIN_FPS, MAX_FPS);
	}

	/**
	 * 设置最佳预览FPS
	 * 
	 * @param parameters
	 *            相机参数
	 * @param minFPS
	 *            最小FPS
	 * @param maxFPS
	 *            最大FPS
	 */
	public static void setBestPreviewFPS(Camera.Parameters parameters,
			int minFPS, int maxFPS) {
		List<int[]> supportedPreviewFpsRanges = parameters
				.getSupportedPreviewFpsRange();
		Log.i(TAG, "支持的FPS范围: " + toString(supportedPreviewFpsRanges));
		if (supportedPreviewFpsRanges != null
				&& !supportedPreviewFpsRanges.isEmpty()) {
			int[] suitableFPSRange = null;
			for (int[] fpsRange : supportedPreviewFpsRanges) {
				int thisMin = fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
				int thisMax = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
				if (thisMin >= minFPS * 1000 && thisMax <= maxFPS * 1000) {
					suitableFPSRange = fpsRange;
					break;
				}
			}
			if (suitableFPSRange == null) {
				Log.i(TAG, "没有合适的FPS范围?");
			} else {
				int[] currentFpsRange = new int[2];
				parameters.getPreviewFpsRange(currentFpsRange);
				if (Arrays.equals(currentFpsRange, suitableFPSRange)) {
					Log.i(TAG, "FPS范围已设置为 " + Arrays.toString(suitableFPSRange));
				} else {
					Log.i(TAG, "将FPS范围设置为 " + Arrays.toString(suitableFPSRange));
					parameters
							.setPreviewFpsRange(
									suitableFPSRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
									suitableFPSRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
				}
			}
		}
	}

	/**
	 * 设置焦点区域
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setFocusArea(Camera.Parameters parameters) {
		if (parameters.getMaxNumFocusAreas() > 0) {
			Log.i(TAG, "旧的焦点区域: " + toString(parameters.getFocusAreas()));
			List<Camera.Area> middleArea = buildMiddleArea(AREA_PER_1000);
			Log.i(TAG, "设置焦点区域 : " + toString(middleArea));
			parameters.setFocusAreas(middleArea);
		} else {
			Log.i(TAG, "设备不支持对焦区域");
		}
	}

	/**
	 * 设置测光区域
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setMetering(Camera.Parameters parameters) {
		if (parameters.getMaxNumMeteringAreas() > 0) {
			Log.i(TAG, "旧测光区域: " + parameters.getMeteringAreas());
			List<Camera.Area> middleArea = buildMiddleArea(AREA_PER_1000);
			Log.i(TAG, "将测光区域设置为 : " + toString(middleArea));
			parameters.setMeteringAreas(middleArea);
		} else {
			Log.i(TAG, "设备不支持测光区域");
		}
	}

	/**
	 * 构建中间区域
	 * 
	 * @param areaPer1000
	 * @return
	 */
	private static List<Camera.Area> buildMiddleArea(int areaPer1000) {
		return Collections.singletonList(new Camera.Area(new Rect(-areaPer1000,
				-areaPer1000, areaPer1000, areaPer1000), 1));
	}

	/**
	 * 设置视频稳定
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setVideoStabilization(Camera.Parameters parameters) {
		if (parameters.isVideoStabilizationSupported()) {
			if (parameters.getVideoStabilization()) {
				Log.i(TAG, "视频稳定已启用");
			} else {
				Log.i(TAG, "启用视频稳定...");
				parameters.setVideoStabilization(true);
			}
		} else {
			Log.i(TAG, "此设备不支持视频稳定");
		}
	}

	/**
	 * 设置条码场景模式
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setBarcodeSceneMode(Camera.Parameters parameters) {
		if (Camera.Parameters.SCENE_MODE_BARCODE.equals(parameters
				.getSceneMode())) {
			Log.i(TAG, "条码场景模式已设置");
			return;
		}
		String sceneMode = findSettableValue("场景模式",
				parameters.getSupportedSceneModes(),
				Camera.Parameters.SCENE_MODE_BARCODE);
		if (sceneMode != null) {
			parameters.setSceneMode(sceneMode);
		}
	}

	/**
	 * 设置缩放
	 * 
	 * @param parameters
	 *            相机参数
	 * @param targetZoomRatio
	 *            目标缩放比例
	 */
	public static void setZoom(Camera.Parameters parameters,
			double targetZoomRatio) {
		if (parameters.isZoomSupported()) {
			Integer zoom = indexOfClosestZoom(parameters, targetZoomRatio);
			if (zoom == null) {
				return;
			}
			if (parameters.getZoom() == zoom) {
				Log.i(TAG, "缩放已设置为 " + zoom);
			} else {
				Log.i(TAG, "设置缩放 " + zoom);
				parameters.setZoom(zoom);
			}
		} else {
			Log.i(TAG, "不支持缩放");
		}
	}

	/**
	 * 最接近的缩放indexOf
	 * 
	 * @param parameters
	 *            相机参数
	 * @param targetZoomRatio
	 *            目标缩放比例
	 * @return
	 */
	private static Integer indexOfClosestZoom(Camera.Parameters parameters,
			double targetZoomRatio) {
		List<Integer> ratios = parameters.getZoomRatios();
		Log.i(TAG, "缩放比例: " + ratios);
		int maxZoom = parameters.getMaxZoom();
		if (ratios == null || ratios.isEmpty() || ratios.size() != maxZoom + 1) {
			Log.w(TAG, "缩放倍数无效!");
			return null;
		}
		double target100 = 100.0 * targetZoomRatio;
		double smallestDiff = Double.POSITIVE_INFINITY;
		int closestIndex = 0;
		for (int i = 0; i < ratios.size(); i++) {
			double diff = Math.abs(ratios.get(i) - target100);
			if (diff < smallestDiff) {
				smallestDiff = diff;
				closestIndex = i;
			}
		}
		Log.i(TAG, "选择缩放比例 " + (ratios.get(closestIndex) / 100.0));
		return closestIndex;
	}

	/**
	 * 设置反转颜色
	 * 
	 * @param parameters
	 *            相机参数
	 */
	public static void setInvertColor(Camera.Parameters parameters) {
		if (Camera.Parameters.EFFECT_NEGATIVE.equals(parameters
				.getColorEffect())) {
			Log.i(TAG, "负效果已设定");
			return;
		}
		String colorMode = findSettableValue("颜色效果",
				parameters.getSupportedColorEffects(),
				Camera.Parameters.EFFECT_NEGATIVE);
		if (colorMode != null) {
			parameters.setColorEffect(colorMode);
		}
	}

	/**
	 * 找到最佳预览大小值
	 * 
	 * @param parameters
	 *            相机参数
	 * @param screenResolution
	 *            屏幕分辨率
	 * @return
	 */
	public static Point findBestPreviewSizeValue(Camera.Parameters parameters,
			Point screenResolution) {

		List<Camera.Size> rawSupportedSizes = parameters
				.getSupportedPreviewSizes();
		if (rawSupportedSizes == null) {
			Log.w(TAG, "设备返回不支持预览大小; 使用默认值");
			Camera.Size defaultSize = parameters.getPreviewSize();
			if (defaultSize == null) {
				throw new IllegalStateException("参数不包含预览大小!");
			}
			return new Point(defaultSize.width, defaultSize.height);
		}

		// 按尺寸排序，降序
		List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(
				rawSupportedSizes);
		Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size a, Camera.Size b) {
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels) {
					return -1;
				}
				if (bPixels > aPixels) {
					return 1;
				}
				return 0;
			}
		});

		if (Log.isLoggable(TAG, Log.INFO)) {
			StringBuilder previewSizesString = new StringBuilder();
			for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
				previewSizesString.append(supportedPreviewSize.width)
						.append('x').append(supportedPreviewSize.height)
						.append(' ');
			}
			Log.i(TAG, "支持的预览大小: " + previewSizesString);
		}

		double screenAspectRatio = screenResolution.x
				/ (double) screenResolution.y;

		// 删除不合适的大小
		Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
		while (it.hasNext()) {
			Camera.Size supportedPreviewSize = it.next();
			int realWidth = supportedPreviewSize.width;
			int realHeight = supportedPreviewSize.height;
			if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
				it.remove();
				continue;
			}

			boolean isCandidatePortrait = realWidth < realHeight;
			int maybeFlippedWidth = isCandidatePortrait ? realHeight
					: realWidth;
			int maybeFlippedHeight = isCandidatePortrait ? realWidth
					: realHeight;
			double aspectRatio = maybeFlippedWidth
					/ (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION) {
				it.remove();
				continue;
			}

			if (maybeFlippedWidth == screenResolution.x
					&& maybeFlippedHeight == screenResolution.y) {
				Point exactPoint = new Point(realWidth, realHeight);
				Log.i(TAG, "发现预览大小与屏幕尺寸完全匹配: " + exactPoint);
				return exactPoint;
			}
		}

		// 如果没有完全匹配，请使用最大的预览大小。 由于需要额外的计算，这对于旧设备来说不是一个好主意。 我们可能会在新的Android
		// 4+设备上使用，CPU的功能更强大。
		if (!supportedPreviewSizes.isEmpty()) {
			Camera.Size largestPreview = supportedPreviewSizes.get(0);
			Point largestSize = new Point(largestPreview.width,
					largestPreview.height);
			Log.i(TAG, "使用最大的合适预览大小: " + largestSize);
			return largestSize;
		}

		// 如果没有任何合适的，返回当前的预览大小
		Camera.Size defaultPreview = parameters.getPreviewSize();
		if (defaultPreview == null) {
			throw new IllegalStateException("参数不包含预览大小!");
		}
		Point defaultSize = new Point(defaultPreview.width,
				defaultPreview.height);
		Log.i(TAG, "没有合适的预览大小，默认使用: " + defaultSize);
		return defaultSize;
	}

	/**
	 * 查找可设置的值
	 * 
	 * @param name
	 * @param supportedValues
	 * @param desiredValues
	 * @return
	 */
	private static String findSettableValue(String name,
			Collection<String> supportedValues, String... desiredValues) {
		Log.i(TAG, "请求 " + name + " 来之: " + Arrays.toString(desiredValues));
		Log.i(TAG, "支持的 " + name + " 值: " + supportedValues);
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					Log.i(TAG, "可以设置 " + name + " 至: " + desiredValue);
					return desiredValue;
				}
			}
		}
		Log.i(TAG, "不支持的值匹配");
		return null;
	}

	private static String toString(Collection<int[]> arrays) {
		if (arrays == null || arrays.isEmpty()) {
			return "[]";
		}
		StringBuilder buffer = new StringBuilder();
		buffer.append('[');
		Iterator<int[]> it = arrays.iterator();
		while (it.hasNext()) {
			buffer.append(Arrays.toString(it.next()));
			if (it.hasNext()) {
				buffer.append(", ");
			}
		}
		buffer.append(']');
		return buffer.toString();
	}

	private static String toString(Iterable<Camera.Area> areas) {
		if (areas == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for (Camera.Area area : areas) {
			result.append(area.rect).append(':').append(area.weight)
					.append(' ');
		}
		return result.toString();
	}

	public static String collectStats(Camera.Parameters parameters) {
		return collectStats(parameters.flatten());
	}

	/**
	 * 收集统计
	 * 
	 * @param flattenedParams
	 * @return
	 */
	public static String collectStats(CharSequence flattenedParams) {
		StringBuilder sb = new StringBuilder(1000);

		sb.append("BOARD=").append(Build.BOARD).append('\n');
		sb.append("BRAND=").append(Build.BRAND).append('\n');
		sb.append("CPU_ABI=").append(Build.CPU_ABI).append('\n');
		sb.append("DEVICE=").append(Build.DEVICE).append('\n');
		sb.append("DISPLAY=").append(Build.DISPLAY).append('\n');
		sb.append("FINGERPRINT=").append(Build.FINGERPRINT).append('\n');
		sb.append("HOST=").append(Build.HOST).append('\n');
		sb.append("ID=").append(Build.ID).append('\n');
		sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append('\n');
		sb.append("MODEL=").append(Build.MODEL).append('\n');
		sb.append("PRODUCT=").append(Build.PRODUCT).append('\n');
		sb.append("TAGS=").append(Build.TAGS).append('\n');
		sb.append("TIME=").append(Build.TIME).append('\n');
		sb.append("TYPE=").append(Build.TYPE).append('\n');
		sb.append("USER=").append(Build.USER).append('\n');
		sb.append("VERSION.CODENAME=").append(Build.VERSION.CODENAME)
				.append('\n');
		sb.append("VERSION.INCREMENTAL=").append(Build.VERSION.INCREMENTAL)
				.append('\n');
		sb.append("VERSION.RELEASE=").append(Build.VERSION.RELEASE)
				.append('\n');
		sb.append("VERSION.SDK_INT=").append(Build.VERSION.SDK_INT)
				.append('\n');

		if (flattenedParams != null) {
			String[] params = SEMICOLON.split(flattenedParams);
			Arrays.sort(params);
			for (String param : params) {
				sb.append(param).append('\n');
			}
		}

		return sb.toString();
	}

}
