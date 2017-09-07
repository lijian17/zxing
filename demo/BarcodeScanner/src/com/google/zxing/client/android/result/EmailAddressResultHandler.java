package com.google.zxing.client.android.result;

import android.app.Activity;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.EmailAddressParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * 处理email地址
 * 
 * @author lijian
 * @date 2017-9-7 下午10:39:27
 */
public final class EmailAddressResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_email,// 发送 E-mail
			R.string.button_add_contact // 添加联系人
	};

	/**
	 * 处理email地址
	 * 
	 * @param activity
	 * @param result
	 */
	public EmailAddressResultHandler(Activity activity, ParsedResult result) {
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
		EmailAddressParsedResult emailResult = (EmailAddressParsedResult) getResult();
		switch (index) {
		case 0:
			sendEmail(emailResult.getTos(), emailResult.getCCs(),
					emailResult.getBCCs(), emailResult.getSubject(),
					emailResult.getBody());
			break;
		case 1:
			addEmailOnlyContact(emailResult.getTos(), null);
			break;
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_email_address;// 找到电子邮件地址
	}
}
