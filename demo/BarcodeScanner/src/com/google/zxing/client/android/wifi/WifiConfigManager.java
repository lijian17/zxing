package com.google.zxing.client.android.wifi;

import java.util.regex.Pattern;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.client.result.WifiParsedResult;

/**
 * Wifi配置管理器
 * 
 * @author lijian
 * @date 2017-9-3 下午10:14:49
 */
public final class WifiConfigManager extends
		AsyncTask<WifiParsedResult, Object, Object> {
	private static final String TAG = WifiConfigManager.class.getSimpleName();

	/** 十六进制数字 */
	private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

	private final WifiManager wifiManager;

	/**
	 * Wifi配置管理器
	 * 
	 * @param wifiManager
	 */
	public WifiConfigManager(WifiManager wifiManager) {
		this.wifiManager = wifiManager;
	}

	@Override
	protected Object doInBackground(WifiParsedResult... args) {
		WifiParsedResult theWifiResult = args[0];
		// 启动WiFi，否则无效
		if (!wifiManager.isWifiEnabled()) {
			Log.i(TAG, "启用WiFi...");
			if (wifiManager.setWifiEnabled(true)) {
				Log.i(TAG, "Wi-fi 启用");
			} else {
				Log.w(TAG, "Wi-fi 无法启用!");
				return null;
			}
			// 这很快发生，但需要等待启用。 有点忙等?
			int count = 0;
			while (!wifiManager.isWifiEnabled()) {
				if (count >= 10) {
					Log.i(TAG, "太长时间启用wifi，退出");
					return null;
				}
				Log.i(TAG, "仍在等待wifi启用...");
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException ie) {
					// continue
				}
				count++;
			}
		}
		String networkTypeString = theWifiResult.getNetworkEncryption();
		NetworkType networkType;
		try {
			networkType = NetworkType.forIntentValue(networkTypeString);
		} catch (IllegalArgumentException ignored) {
			Log.w(TAG, "网络类型不良 请参阅NetworkType值: " + networkTypeString);
			return null;
		}
		if (networkType == NetworkType.NO_PASSWORD) {
			changeNetworkUnEncrypted(wifiManager, theWifiResult);
		} else {
			String password = theWifiResult.getPassword();
			if (password != null && !password.isEmpty()) {
				if (networkType == NetworkType.WEP) {
					changeNetworkWEP(wifiManager, theWifiResult);
				} else if (networkType == NetworkType.WPA) {
					changeNetworkWPA(wifiManager, theWifiResult);
				}
			}
		}
		return null;
	}

	/**
	 * 更新网络：创建新网络或修改现有网络
	 * 
	 * @param wifiManager
	 *            wifi管理器
	 * @param config
	 *            新的网络配置
	 */
	private static void updateNetwork(WifiManager wifiManager,
			WifiConfiguration config) {
		Integer foundNetworkID = findNetworkInExistingConfig(wifiManager,
				config.SSID);
		if (foundNetworkID != null) {
			Log.i(TAG, "删除网络的旧配置 " + config.SSID);
			wifiManager.removeNetwork(foundNetworkID);
			wifiManager.saveConfiguration();
		}
		int networkId = wifiManager.addNetwork(config);
		if (networkId >= 0) {
			// 尝试禁用当前网络并启动新的网络
			if (wifiManager.enableNetwork(networkId, true)) {
				Log.i(TAG, "与网络关联 " + config.SSID);
				wifiManager.saveConfiguration();
			} else {
				Log.w(TAG, "无法启用网络 " + config.SSID);
			}
		} else {
			Log.w(TAG, "无法添加网络 " + config.SSID);
		}
	}

	/**
	 * 改变网络共同点
	 * 
	 * @param wifiResult
	 * @return
	 */
	private static WifiConfiguration changeNetworkCommon(
			WifiParsedResult wifiResult) {
		WifiConfiguration config = new WifiConfiguration();
		config.allowedAuthAlgorithms.clear();
		config.allowedGroupCiphers.clear();
		config.allowedKeyManagement.clear();
		config.allowedPairwiseCiphers.clear();
		config.allowedProtocols.clear();
		// Android API坚持认为必须引用ascii SSID来正确处理
		config.SSID = quoteNonHex(wifiResult.getSsid());
		config.hiddenSSID = wifiResult.isHidden();
		return config;
	}

	/**
	 * 添加WEP网络
	 * 
	 * @param wifiManager
	 * @param wifiResult
	 */
	private static void changeNetworkWEP(WifiManager wifiManager,
			WifiParsedResult wifiResult) {
		WifiConfiguration config = changeNetworkCommon(wifiResult);
		config.wepKeys[0] = quoteNonHex(wifiResult.getPassword(), 10, 26, 58);
		config.wepTxKeyIndex = 0;
		config.allowedAuthAlgorithms
				.set(WifiConfiguration.AuthAlgorithm.SHARED);
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		updateNetwork(wifiManager, config);
	}

	/**
	 * 添加WPA or WPA2网络
	 * 
	 * @param wifiManager
	 * @param wifiResult
	 */
	private static void changeNetworkWPA(WifiManager wifiManager,
			WifiParsedResult wifiResult) {
		WifiConfiguration config = changeNetworkCommon(wifiResult);
		// 64位长的十六进制密码不被引用
		config.preSharedKey = quoteNonHex(wifiResult.getPassword(), 64);
		config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
		config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.CCMP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		updateNetwork(wifiManager, config);
	}

	/**
	 * 添加一个开放的，不安全的网络(更改网络未加密)
	 * 
	 * @param wifiManager
	 * @param wifiResult
	 */
	private static void changeNetworkUnEncrypted(WifiManager wifiManager,
			WifiParsedResult wifiResult) {
		WifiConfiguration config = changeNetworkCommon(wifiResult);
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		updateNetwork(wifiManager, config);
	}

	/**
	 * 查找现有配置中的网络
	 * 
	 * @param wifiManager
	 * @param ssid
	 * @return
	 */
	private static Integer findNetworkInExistingConfig(WifiManager wifiManager,
			String ssid) {
		Iterable<WifiConfiguration> existingConfigs = wifiManager
				.getConfiguredNetworks();
		if (existingConfigs != null) {
			for (WifiConfiguration existingConfig : existingConfigs) {
				String existingSSID = existingConfig.SSID;
				if (existingSSID != null && existingSSID.equals(ssid)) {
					return existingConfig.networkId;
				}
			}
		}
		return null;
	}

	/**
	 * 引用十六进制
	 * 
	 * @param value
	 * @param allowedLengths
	 * @return
	 */
	private static String quoteNonHex(String value, int... allowedLengths) {
		return isHexOfLength(value, allowedLengths) ? value
				: convertToQuotedString(value);
	}

	/**
	 * 如果尚未引用，则将传入的字符串括在双引号内
	 * 
	 * @param s
	 *            输入字符串
	 * @return 引用的字符串，形式为“input”。 如果输入字符串为空，则返回null
	 */
	private static String convertToQuotedString(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		// 如果已经被引用，按原样返回
		if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
			return s;
		}
		return '\"' + s + '\"';
	}

	/**
	 * 是否十六进制长度
	 * 
	 * @param value
	 *            输入检查
	 * @param allowedLengths
	 *            允许的长度，如果有的话
	 * @return 如果value是十六进制数字的非null，非空字符串，并且如果允许的长度被赋予，则为true，具有允许的长度
	 */
	private static boolean isHexOfLength(CharSequence value,
			int... allowedLengths) {
		if (value == null || !HEX_DIGITS.matcher(value).matches()) {
			return false;
		}
		if (allowedLengths.length == 0) {
			return true;
		}
		for (int length : allowedLengths) {
			if (value.length() == length) {
				return true;
			}
		}
		return false;
	}

}
