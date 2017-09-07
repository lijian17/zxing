package com.google.zxing.client.android.result;

import android.app.Activity;
import android.telephony.PhoneNumberUtils;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.SMSParsedResult;

/**
 * 处理短信地址，提供撰写新短信或彩信的选择
 * 
 * @author lijian
 * @date 2017-9-7 下午10:05:39
 */
public final class SMSResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_sms,// 发送短信
			R.string.button_mms // 发送彩信
	};

	/**
	 * 
	 * 处理短信地址，提供撰写新短信或彩信的选择
	 * 
	 * @param activity
	 * @param result
	 */
	public SMSResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
	}

	@Override
	public int getButtonCount() {
		return buttons.length;
	}

	@Override
	public int getButtonText(int index) {
		return buttons[index];
	}

	@Override
	public void handleButtonPress(int index) {
		SMSParsedResult smsResult = (SMSParsedResult) getResult();
		String number = smsResult.getNumbers()[0];
		switch (index) {
		case 0:
			// 不知道有多少收件人表达SENDTO意图的方式
			sendSMS(number, smsResult.getBody());
			break;
		case 1:
			sendMMS(number, smsResult.getSubject(), smsResult.getBody());
			break;
		}
	}

	@Override
	public CharSequence getDisplayContents() {
		SMSParsedResult smsResult = (SMSParsedResult) getResult();
		String[] rawNumbers = smsResult.getNumbers();
		String[] formattedNumbers = new String[rawNumbers.length];
		for (int i = 0; i < rawNumbers.length; i++) {
			formattedNumbers[i] = PhoneNumberUtils.formatNumber(rawNumbers[i]);
		}
		StringBuilder contents = new StringBuilder(50);
		ParsedResult.maybeAppend(formattedNumbers, contents);
		ParsedResult.maybeAppend(smsResult.getSubject(), contents);
		ParsedResult.maybeAppend(smsResult.getBody(), contents);
		return contents.toString();
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_sms;// 找到短信号码
	}
}
