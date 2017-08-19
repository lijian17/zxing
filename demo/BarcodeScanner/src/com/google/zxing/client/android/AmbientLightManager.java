package com.google.zxing.client.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.camera.FrontLightMode;

/**
 * 检测环境光，并在非常暗的情况下打开前灯，并在足够亮时再次关闭。
 * 
 * @author lijian
 * @date 2017-8-16 下午3:39:19
 */
final class AmbientLightManager implements SensorEventListener {

	private static final float TOO_DARK_LUX = 45.0f;
	private static final float BRIGHT_ENOUGH_LUX = 450.0f;

	private final Context context;
	private CameraManager cameraManager;
	private Sensor lightSensor;

	AmbientLightManager(Context context) {
		this.context = context;
	}

	/**
	 * 启动光感应传感器监听
	 * 
	 * @param cameraManager
	 */
	void start(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (FrontLightMode.readPref(sharedPrefs) == FrontLightMode.AUTO) {
			SensorManager sensorManager = (SensorManager) context
					.getSystemService(Context.SENSOR_SERVICE);
			lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			if (lightSensor != null) {
				sensorManager.registerListener(this, lightSensor,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}

	/**
	 * 关闭光感应传感器监听
	 */
	void stop() {
		if (lightSensor != null) {
			SensorManager sensorManager = (SensorManager) context
					.getSystemService(Context.SENSOR_SERVICE);
			sensorManager.unregisterListener(this);
			cameraManager = null;
			lightSensor = null;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		float ambientLightLux = sensorEvent.values[0];
		if (cameraManager != null) {
			// 当亮度低时，开启闪光灯
			if (ambientLightLux <= TOO_DARK_LUX) {
				cameraManager.setTorch(true);
				// 当亮度高时，关闭闪光灯
			} else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
				cameraManager.setTorch(false);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// do nothing
	}

}
