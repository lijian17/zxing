package com.google.zxing.client.android.encode;

/**
 * 封装一些简单的格式化逻辑，以帮助重构{@link ContactEncoder}
 * 
 * @author lijian
 * @date 2017-9-3 下午10:42:55
 */
interface Formatter {

  /**
   * 格式化
   * 
   * @param value 值进行格式化
   * @param index 要格式化的值列表中的值索引
   * @return 格式化值
   */
  CharSequence format(CharSequence value, int index);
  
}
