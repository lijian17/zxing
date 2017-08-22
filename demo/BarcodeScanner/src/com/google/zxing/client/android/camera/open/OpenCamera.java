package com.google.zxing.client.android.camera.open;

import android.hardware.Camera;

/**
 * 代表一个打开的相机{@link Camera}及其元数据，像面对的方向和定位。
 * 
 * @author lijian-pc
 * @date 2017-8-22 下午2:51:55
 */
public final class OpenCamera {

	private final int index;
	private final Camera camera;
	private final CameraFacing facing;
	private final int orientation;

	public OpenCamera(int index, Camera camera, CameraFacing facing,
			int orientation) {
		this.index = index;
		this.camera = camera;
		this.facing = facing;
		this.orientation = orientation;
	}

	public Camera getCamera() {
		return camera;
	}

	public CameraFacing getFacing() {
		return facing;
	}

	public int getOrientation() {
		return orientation;
	}

	@Override
	public String toString() {
		return "Camera #" + index + " : " + facing + ',' + orientation;
	}

}
