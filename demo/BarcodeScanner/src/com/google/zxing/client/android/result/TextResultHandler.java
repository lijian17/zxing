package com.google.zxing.client.android.result;

import android.app.Activity;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;

/**
 * 此类处理TextParsedResult以及未知格式。 这是回退处理程序
 * 
 * @author lijian
 * @date 2017-9-7 下午10:12:52
 */
public final class TextResultHandler extends ResultHandler {

	private static final int[] buttons = {//
	R.string.button_web_search,// 网页搜索
			R.string.button_share_by_email,// 通过 E-mail 分享
			R.string.button_share_by_sms,// 通过短信分享
			R.string.button_custom_product_search,// 自定义搜索
	};

	/**
	 * 此类处理TextParsedResult以及未知格式。 这是回退处理程序
	 * 
	 * @param activity
	 * @param result
	 * @param rawResult
	 */
	public TextResultHandler(Activity activity, ParsedResult result,
			Result rawResult) {
		super(activity, result, rawResult);
	}

	@Override
	public int getButtonCount() {
		return hasCustomProductSearch() ? buttons.length : buttons.length - 1;
	}

	@Override
	public int getButtonText(int index) {
		return buttons[index];
	}

	@Override
	public void handleButtonPress(int index) {
		String text = getResult().getDisplayResult();
		switch (index) {
		case 0:
			webSearch(text);
			break;
		case 1:
			shareByEmail(text);
			break;
		case 2:
			shareBySMS(text);
			break;
		case 3:
			openURL(fillInCustomSearchURL(text));
			break;
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_text;// 找到纯文本
	}
}
