package com.google.zxing.client.android.camera.open;

import android.hardware.Camera;
import android.util.Log;

/**
 * 对{@link Camera}API进行抽象，可帮助您打开它们并返回其元数据。
 * 
 * @author lijian
 * @date 2017-8-27 上午9:58:56
 */
public final class OpenCameraInterface {
	private static final String TAG = OpenCameraInterface.class.getName();

	private OpenCameraInterface() {
	}

	/** For {@link #open(int)}, means no preference for which camera to open. */
	public static final int NO_REQUESTED_CAMERA = -1;

	/**
	 * 用{@link Camera#open(int)}打开所需的相机，如果有的话。
	 * 
	 * @param cameraId
	 *            要使用的相机的相机ID。 负值或{@link #NO_REQUESTED_CAMERA}
	 *            表示“无偏好”，在这种情况下，如果可能，返回后置摄像头，否则任何摄像头
	 * @return 处理已打开的{@link OpenCamera}
	 */
	public static OpenCamera open(int cameraId) {

		int numCameras = Camera.getNumberOfCameras();
		if (numCameras == 0) {
			Log.w(TAG, "没有相!");
			return null;
		}

		boolean explicitRequest = cameraId >= 0;

		Camera.CameraInfo selectedCameraInfo = null;
		int index;
		if (explicitRequest) {
			index = cameraId;
			selectedCameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(index, selectedCameraInfo);
		} else {
			index = 0;
			while (index < numCameras) {
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(index, cameraInfo);
				CameraFacing reportedFacing = CameraFacing.values()[cameraInfo.facing];
				if (reportedFacing == CameraFacing.BACK) {
					selectedCameraInfo = cameraInfo;
					break;
				}
				index++;
			}
		}

		Camera camera;
		if (index < numCameras) {
			Log.i(TAG, "打开相机 #" + index);
			camera = Camera.open(index);
		} else {
			if (explicitRequest) {
				Log.w(TAG, "请求的相机不存在: " + cameraId);
				camera = null;
			} else {
				Log.i(TAG, "没有相机面对 " + CameraFacing.BACK + "; 返回相机 #0");
				camera = Camera.open(0);
				selectedCameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(0, selectedCameraInfo);
			}
		}

		if (camera == null) {
			return null;
		}
		return new OpenCamera(index, camera,
				CameraFacing.values()[selectedCameraInfo.facing],
				selectedCameraInfo.orientation);
	}

}
