package com.google.zxing.client.android.camera;

import android.content.SharedPreferences;
import com.google.zxing.client.android.PreferencesActivity;

/**
 * 前灯模式<br>
 * 列举控制前灯的偏好设置。
 */
public enum FrontLightMode {

	/** 始终打开 */
	ON,
	/** 仅在环境光线不足时才能使用. */
	AUTO,
	/** 始终关闭. */
	OFF;

	private static FrontLightMode parse(String modeString) {
		return modeString == null ? OFF : valueOf(modeString);
	}

	public static FrontLightMode readPref(SharedPreferences sharedPrefs) {
		return parse(sharedPrefs.getString(
				PreferencesActivity.KEY_FRONT_LIGHT_MODE, OFF.toString()));
	}

}
