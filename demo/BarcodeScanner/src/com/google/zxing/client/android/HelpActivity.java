package com.google.zxing.client.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import com.google.zxing.client.android.R;

/**
 * 基于HTML的帮助屏幕。
 * 
 * @author lijian
 * @date 2017-9-3 下午9:24:54
 */
public final class HelpActivity extends Activity {

  private static final String BASE_URL =
      "file:///android_asset/html-" + LocaleManager.getTranslatedAssetLanguage() + '/';

  private WebView webView;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.help);

    webView = (WebView) findViewById(R.id.help_contents);

    if (icicle == null) {
      webView.loadUrl(BASE_URL + "index.html");
    } else {
      webView.restoreState(icicle);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
      webView.goBack();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onSaveInstanceState(Bundle icicle) {
    super.onSaveInstanceState(icicle);
    webView.saveState(icicle);
  }
}
