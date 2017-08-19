package com.google.zxing.client.android.history;

import com.google.zxing.Result;

/**
 * 扫描历史条目实体类
 */
public final class HistoryItem {

	/** 扫描到的结果 */
	private final Result result;
	/** 要展示的文本内容 */
	private final String display;
	/** 详细信息 */
	private final String details;

	HistoryItem(Result result, String display, String details) {
		this.result = result;
		this.display = display;
		this.details = details;
	}

	/**
	 * 获取扫描结果
	 * 
	 * @return
	 */
	public Result getResult() {
		return result;
	}

	/**
	 * 获取扫描内容和内容详情
	 * 
	 * @return
	 */
	public String getDisplayAndDetails() {
		StringBuilder displayResult = new StringBuilder();
		if (display == null || display.isEmpty()) {
			displayResult.append(result.getText());
		} else {
			displayResult.append(display);
		}
		if (details != null && !details.isEmpty()) {
			displayResult.append(" : ").append(details);
		}
		return displayResult.toString();
	}

}
