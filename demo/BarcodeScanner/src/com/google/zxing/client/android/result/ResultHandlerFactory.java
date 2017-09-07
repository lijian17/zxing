package com.google.zxing.client.android.result;

import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

/**
 * 根据条形码内容类型制作Android特定的处理程序
 * 
 * @author lijian
 * @date 2017-9-7 下午10:14:49
 */
public final class ResultHandlerFactory {
	private ResultHandlerFactory() {
	}

	/**
	 * 生产结果处理器
	 * 
	 * @param activity
	 * @param rawResult
	 * @return
	 */
	public static ResultHandler makeResultHandler(CaptureActivity activity,
			Result rawResult) {
		ParsedResult result = parseResult(rawResult);
		switch (result.getType()) {
		case ADDRESSBOOK:
			return new AddressBookResultHandler(activity, result);
		case EMAIL_ADDRESS:
			return new EmailAddressResultHandler(activity, result);
		case PRODUCT:
			return new ProductResultHandler(activity, result, rawResult);
		case URI:
			return new URIResultHandler(activity, result);
		case WIFI:
			return new WifiResultHandler(activity, result);
		case GEO:
			return new GeoResultHandler(activity, result);
		case TEL:
			return new TelResultHandler(activity, result);
		case SMS:
			return new SMSResultHandler(activity, result);
		case CALENDAR:
			return new CalendarResultHandler(activity, result);
		case ISBN:
			return new ISBNResultHandler(activity, result, rawResult);
		default:
			return new TextResultHandler(activity, result, rawResult);
		}
	}

	/**
	 * 解析结果
	 * 
	 * @param rawResult
	 * @return
	 */
	private static ParsedResult parseResult(Result rawResult) {
		return ResultParser.parseResult(rawResult);
	}
}
