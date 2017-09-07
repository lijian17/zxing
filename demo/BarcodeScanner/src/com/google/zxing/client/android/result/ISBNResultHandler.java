package com.google.zxing.client.android.result;

import android.app.Activity;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * 处理其ISBN值编码的书籍
 * 
 * @author lijian
 * @date 2017-9-7 下午10:23:37
 */
public final class ISBNResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_product_search,// 打开商品搜索
			R.string.button_book_search,// 打开图书搜索
			R.string.button_search_book_contents,// 搜索图书内容
			R.string.button_custom_product_search // 自定义搜索
	};

	/**
	 * 处理其ISBN值编码的书籍
	 * 
	 * @param activity
	 * @param result
	 * @param rawResult
	 */
	public ISBNResultHandler(Activity activity, ParsedResult result,
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
		ISBNParsedResult isbnResult = (ISBNParsedResult) getResult();
		switch (index) {
		case 0:
			openProductSearch(isbnResult.getISBN());
			break;
		case 1:
			openBookSearch(isbnResult.getISBN());
			break;
		case 2:
			searchBookContents(isbnResult.getISBN());
			break;
		case 3:
			openURL(fillInCustomSearchURL(isbnResult.getISBN()));
			break;
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_isbn;// 找到图书
	}
}
