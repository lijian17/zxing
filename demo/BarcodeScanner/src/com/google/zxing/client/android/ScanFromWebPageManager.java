package com.google.zxing.client.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.net.Uri;

import com.google.zxing.Result;
import com.google.zxing.client.android.result.ResultHandler;

/**
 * 从网页管理器扫描<br>
 * 管理与从Web页面中的HTTP链接进行扫描的请求相关的功能。 请参阅<a
 * href="https://github.com/zxing/zxing/wiki/ScanningFromWebPages"
 * >ScanningFromWebPages</a>
 * 
 * @author lijian
 * @date 2017-9-1 下午6:00:17
 */
final class ScanFromWebPageManager {

	/** 占位符-CODE */
	private static final CharSequence CODE_PLACEHOLDER = "{CODE}";
	/** 占位符-RAWCODE */
	private static final CharSequence RAW_CODE_PLACEHOLDER = "{RAWCODE}";
	/** 占位符-META */
	private static final CharSequence META_PLACEHOLDER = "{META}";
	/** 占位符-FORMAT */
	private static final CharSequence FORMAT_PLACEHOLDER = "{FORMAT}";
	/** 占位符-TYPE */
	private static final CharSequence TYPE_PLACEHOLDER = "{TYPE}";

	/** 返回URL参数 */
	private static final String RETURN_URL_PARAM = "ret";
	/** 原参数 */
	private static final String RAW_PARAM = "raw";

	/** 返回Url模板 */
	private final String returnUrlTemplate;
	/** 返回源 */
	private final boolean returnRaw;

	/**
	 * 从网页管理器扫描
	 * 
	 * @param inputUri
	 */
	ScanFromWebPageManager(Uri inputUri) {
		returnUrlTemplate = inputUri.getQueryParameter(RETURN_URL_PARAM);
		returnRaw = inputUri.getQueryParameter(RAW_PARAM) != null;
	}

	/**
	 * 是从网页扫描
	 * 
	 * @return
	 */
	boolean isScanFromWebPage() {
		return returnUrlTemplate != null;
	}

	/**
	 * 构建回复URL
	 * 
	 * @param rawResult
	 *            原始结果
	 * @param resultHandler
	 *            结果处理器
	 * @return
	 */
	String buildReplyURL(Result rawResult, ResultHandler resultHandler) {
		String result = returnUrlTemplate;
		result = replace(CODE_PLACEHOLDER, returnRaw ? rawResult.getText()
				: resultHandler.getDisplayContents(), result);
		result = replace(RAW_CODE_PLACEHOLDER, rawResult.getText(), result);
		result = replace(FORMAT_PLACEHOLDER, rawResult.getBarcodeFormat()
				.toString(), result);
		result = replace(TYPE_PLACEHOLDER, resultHandler.getType().toString(),
				result);
		result = replace(META_PLACEHOLDER,
				String.valueOf(rawResult.getResultMetadata()), result);
		return result;
	}

	/**
	 * 替换
	 * 
	 * @param placeholder
	 *            占位符
	 * @param with
	 * @param pattern
	 *            模式
	 * @return
	 */
	private static String replace(CharSequence placeholder, CharSequence with,
			String pattern) {
		CharSequence escapedWith = with == null ? "" : with;
		try {
			// 编码
			escapedWith = URLEncoder.encode(escapedWith.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// 不能发生; 始终支持UTF-8。 继续，我猜，没有编码
		}
		return pattern.replace(placeholder, escapedWith);
	}

}
