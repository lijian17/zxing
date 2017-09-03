package com.google.zxing.client.android.wifi;

/**
 * 网络类型
 * 
 * @author lijian
 * @date 2017-9-3 下午10:13:34
 */
enum NetworkType {

  WEP,
  WPA,
  NO_PASSWORD;

  static NetworkType forIntentValue(String networkTypeString) {
    if (networkTypeString == null) {
      return NO_PASSWORD;
    }
    if ("WPA".equals(networkTypeString) ||
        "WPA2".equals(networkTypeString)) {
      return WPA;
    }
    if ("WEP".equals(networkTypeString)) {
      return WEP;
    }
    if ("nopass".equals(networkTypeString)) {
      return NO_PASSWORD;
    }
    throw new IllegalArgumentException(networkTypeString);
  }

}
