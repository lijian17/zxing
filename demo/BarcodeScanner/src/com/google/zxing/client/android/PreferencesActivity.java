package com.google.zxing.client.android;

import android.app.Activity;
import android.os.Bundle;

/**
 * 主设置activity(本地配置文件存取值用的key)
 * 
 * @author lijian
 * @date 2017-8-14 下午11:23:20
 */
public final class PreferencesActivity extends Activity {

	/** 解码1D产品 */
  public static final String KEY_DECODE_1D_PRODUCT = "preferences_decode_1D_product";
  /** 解码1D工业 */
  public static final String KEY_DECODE_1D_INDUSTRIAL = "preferences_decode_1D_industrial";
  /** 解码QR */
  public static final String KEY_DECODE_QR = "preferences_decode_QR";
  /** 解码DataMatrix */
  public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";
  /** 解码Aztec */
  public static final String KEY_DECODE_AZTEC = "preferences_decode_Aztec";
  /** 解码PDF417 */
  public static final String KEY_DECODE_PDF417 = "preferences_decode_PDF417";

  /** 定制产品搜索 */
  public static final String KEY_CUSTOM_PRODUCT_SEARCH = "preferences_custom_product_search";

  /** beep声 */
  public static final String KEY_PLAY_BEEP = "preferences_play_beep";
  /** 震动 */
  public static final String KEY_VIBRATE = "preferences_vibrate";
  /** 复制到剪贴板 */
  public static final String KEY_COPY_TO_CLIPBOARD = "preferences_copy_to_clipboard";
  /** 前灯模式 */
  public static final String KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode";
  /** 批量模式 */
  public static final String KEY_BULK_MODE = "preferences_bulk_mode";
  /** 记得重复 */
  public static final String KEY_REMEMBER_DUPLICATES = "preferences_remember_duplicates";
  /** 历史记录 */
  public static final String KEY_ENABLE_HISTORY = "preferences_history";
  /** 补充 */
  public static final String KEY_SUPPLEMENTAL = "preferences_supplemental";
  /** 自动对焦 */
  public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
  /** 反转扫描 */
  public static final String KEY_INVERT_SCAN = "preferences_invert_scan";  
  /** 搜索的国家 */
  public static final String KEY_SEARCH_COUNTRY = "preferences_search_country";
  /** 禁止自动定向 */
  public static final String KEY_DISABLE_AUTO_ORIENTATION = "preferences_orientation";

  /** 禁用连续对焦 */
  public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
  /** 禁用曝光 */
  public static final String KEY_DISABLE_EXPOSURE = "preferences_disable_exposure";
  /** 禁用计量 */
  public static final String KEY_DISABLE_METERING = "preferences_disable_metering";
  /** 禁用条形码场景模式 */
  public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "preferences_disable_barcode_scene_mode";
  /** 自动打开网页 */
  public static final String KEY_AUTO_OPEN_WEB = "preferences_auto_open_web";

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
  }

  // Apparently this will be necessary when targeting API 19+:
  /*
  @Override
  protected boolean isValidFragment(String fragmentName) {
    return true;
  }
   */

}
