package com.google.zxing.client.android.result;

import android.app.Activity;
import android.telephony.PhoneNumberUtils;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.TelParsedResult;

/**
 * 提供电话号码的相关行动
 * 
 * @author lijian
 * @date 2017-9-7 下午10:09:52
 */
public final class TelResultHandler extends ResultHandler {
	private static final int[] buttons = {//
	R.string.button_dial,// 拨打电话
			R.string.button_add_contact // 添加联系人
	};

	/**
	 * 
	 * 提供电话号码的相关行动
	 * 
	 * @param activity
	 * @param result
	 */
	public TelResultHandler(Activity activity, ParsedResult result) {
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
		TelParsedResult telResult = (TelParsedResult) getResult();
		switch (index) {
		case 0:
			dialPhoneFromUri(telResult.getTelURI());
			// 当拨号器出现时，它允许基础显示活动继续或某事，但应用程序无法在此状态下获取相机。 只有在电话号码的情况下才可以避免出现问题
			getActivity().finish();
			break;
		case 1:
			String[] numbers = new String[1];
			numbers[0] = telResult.getNumber();
			addPhoneOnlyContact(numbers, null);
			break;
		}
	}

	// 被覆盖，所以我们可以利用Android的手机号连字例程
	@Override
	public CharSequence getDisplayContents() {
		String contents = getResult().getDisplayResult();
		contents = contents.replace("\r", "");
		return PhoneNumberUtils.formatNumber(contents);
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_tel;// 找到电话号码
	}
}
