package com.google.zxing.client.android.result;

import android.app.Activity;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ExpandedProductParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;

/**
 * 处理不是书籍的通用产品
 * 
 * @author lijian
 * @date 2017-9-7 下午10:42:19
 */
public final class ProductResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_product_search,// 打开商品搜索
			R.string.button_web_search,// 网页搜索
			R.string.button_custom_product_search // 自定义搜索
	};

	/**
	 * 处理不是书籍的通用产品
	 * 
	 * @param activity
	 * @param result
	 * @param rawResult
	 */
	public ProductResultHandler(Activity activity, ParsedResult result,
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
		String productID = getProductIDFromResult(getResult());
		switch (index) {
		case 0:
			openProductSearch(productID);
			break;
		case 1:
			webSearch(productID);
			break;
		case 2:
			openURL(fillInCustomSearchURL(productID));
			break;
		}
	}

	/**
	 * 从结果获取产品ID
	 * 
	 * @param rawResult
	 * @return
	 */
	private static String getProductIDFromResult(ParsedResult rawResult) {
		if (rawResult instanceof ProductParsedResult) {
			return ((ProductParsedResult) rawResult).getNormalizedProductID();
		}
		if (rawResult instanceof ExpandedProductParsedResult) {
			return ((ExpandedProductParsedResult) rawResult).getRawText();
		}
		throw new IllegalArgumentException(rawResult.getClass().toString());
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_product;// 找到商品
	}
}
