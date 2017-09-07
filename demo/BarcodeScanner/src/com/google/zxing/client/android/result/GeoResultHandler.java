package com.google.zxing.client.android.result;

import android.app.Activity;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.GeoParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * 处理地理坐标（通常编码为geo：URLs）
 * 
 * @author lijian
 * @date 2017-9-7 下午10:37:08
 */
public final class GeoResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_show_map,// 显示地图
			R.string.button_get_directions // 获取地址
	};

	/**
	 * 处理地理坐标（通常编码为geo：URLs）
	 * 
	 * @param activity
	 * @param result
	 */
	public GeoResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
	}

	@Override
	public int getButtonCount() {
		return buttons.length;
	}

	@Override
	public int getButtonText(int index) {
		return buttons[index];
	}

	@Override
	public void handleButtonPress(int index) {
		GeoParsedResult geoResult = (GeoParsedResult) getResult();
		switch (index) {
		case 0:
			openMap(geoResult.getGeoURI());
			break;
		case 1:
			getDirections(geoResult.getLatitude(), geoResult.getLongitude());
			break;
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_geo;// 找到地理坐标
	}
}
