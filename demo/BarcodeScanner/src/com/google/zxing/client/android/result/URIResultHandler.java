package com.google.zxing.client.android.result;

import java.util.Locale;

import android.app.Activity;

import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * 为URLS提供适当的操作
 * 
 * @author lijian
 * @date 2017-9-7 下午9:59:40
 */
public final class URIResultHandler extends ResultHandler {
	// 以此数组中的条目开头的URI将不会保存到历史记录中或复制到剪贴板以获取安全性
	private static final String[] SECURE_PROTOCOLS = { "otpauth:" };

	private static final int[] buttons = {//
	R.string.button_open_browser,// 打开浏览器
			R.string.button_share_by_email,// 通过 E-mail 分享
			R.string.button_share_by_sms,// 通过短信分享
			R.string.button_search_book_contents,// 搜索图书内容
	};

	/**
	 * 
	 * 为URLS提供适当的操作
	 * 
	 * @param activity
	 * @param result
	 */
	public URIResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
	}

	@Override
	public int getButtonCount() {
		if (LocaleManager.isBookSearchUrl(((URIParsedResult) getResult())
				.getURI())) {
			return buttons.length;
		}
		return buttons.length - 1;
	}

	@Override
	public int getButtonText(int index) {
		return buttons[index];
	}

	@Override
	public Integer getDefaultButtonID() {
		return 0;
	}

	@Override
	public void handleButtonPress(int index) {
		URIParsedResult uriResult = (URIParsedResult) getResult();
		String uri = uriResult.getURI();
		switch (index) {
		case 0:
			openURL(uri);
			break;
		case 1:
			shareByEmail(uri);
			break;
		case 2:
			shareBySMS(uri);
			break;
		case 3:
			searchBookContents(uri);
			break;
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_uri;// 找到 URL
	}

	@Override
	public boolean areContentsSecure() {
		URIParsedResult uriResult = (URIParsedResult) getResult();
		String uri = uriResult.getURI().toLowerCase(Locale.ENGLISH);
		for (String secure : SECURE_PROTOCOLS) {
			if (uri.startsWith(secure)) {
				return true;
			}
		}
		return false;
	}
}
