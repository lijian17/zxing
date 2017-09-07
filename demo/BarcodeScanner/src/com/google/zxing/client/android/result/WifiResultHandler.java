package com.google.zxing.client.android.result;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.wifi.WifiConfigManager;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.WifiParsedResult;

/**
 * 处理Wifi上网信息
 * 
 * @author lijian
 * @date 2017-9-7 下午9:26:25
 */
public final class WifiResultHandler extends ResultHandler {
	private static final String TAG = WifiResultHandler.class.getSimpleName();

	private final CaptureActivity parent;

	/**
	 * 
	 * 处理Wifi上网信息
	 * 
	 * @param activity
	 * @param result
	 */
	public WifiResultHandler(CaptureActivity activity, ParsedResult result) {
		super(activity, result);
		parent = activity;
	}

	@Override
	public int getButtonCount() {
		// 我们只需要一个按钮，那就是配置无线。 这可能会在将来改变
		return 1;
	}

	@Override
	public int getButtonText(int index) {
		return R.string.button_wifi;// 连接到网络
	}

	@Override
	public void handleButtonPress(int index) {
		if (index == 0) {
			WifiParsedResult wifiResult = (WifiParsedResult) getResult();
			WifiManager wifiManager = (WifiManager) getActivity()
					.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager == null) {
				Log.w(TAG, "没有可用的设备wifiManager");
				return;
			}
			final Activity activity = getActivity();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 连接到 Wi-Fi...
					Toast.makeText(activity.getApplicationContext(),
							R.string.wifi_changing_network, Toast.LENGTH_SHORT)
							.show();
				}
			});
			new WifiConfigManager(wifiManager).executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, wifiResult);
			parent.restartPreviewAfterDelay(0L);
		}
	}

	// 向用户显示网络名称和网络类型
	@Override
	public CharSequence getDisplayContents() {
		WifiParsedResult wifiResult = (WifiParsedResult) getResult();
		return wifiResult.getSsid() + " (" + wifiResult.getNetworkEncryption()
				+ ')';
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_wifi;// 找到 Wi-Fi 配置
	}
}