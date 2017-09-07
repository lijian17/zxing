package com.google.zxing.client.android.result;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.graphics.Typeface;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * 处理地址簿条目
 * 
 * @author lijian
 * @date 2017-9-7 下午10:51:34
 */
public final class AddressBookResultHandler extends ResultHandler {

	private static final DateFormat[] DATE_FORMATS = {
			new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH),
			new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH),
			new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH), };
	static {
		for (DateFormat format : DATE_FORMATS) {
			format.setLenient(false);
		}
	}

	private static final int[] BUTTON_TEXTS = {//
	R.string.button_add_contact,// 添加联系人
			R.string.button_show_map,// 显示地图
			R.string.button_dial,// 拨打电话
			R.string.button_email,// 发送 E-mail
	};

	private final boolean[] fields;
	private int buttonCount;

	/**
	 * 这取决于所有的工作，以确定哪些按钮/动作应该在哪些位置，基于哪些字段存在于这个条形码
	 * 
	 * @param index
	 * @return
	 */
	private int mapIndexToAction(int index) {
		if (index < buttonCount) {
			int count = -1;
			for (int x = 0; x < MAX_BUTTON_COUNT; x++) {
				if (fields[x]) {
					count++;
				}
				if (count == index) {
					return x;
				}
			}
		}
		return -1;
	}

	/**
	 * 地址簿结果处理程序
	 * 
	 * @param activity
	 * @param result
	 */
	public AddressBookResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
		AddressBookParsedResult addressResult = (AddressBookParsedResult) result;
		String[] addresses = addressResult.getAddresses();
		String[] phoneNumbers = addressResult.getPhoneNumbers();
		String[] emails = addressResult.getEmails();

		fields = new boolean[MAX_BUTTON_COUNT];
		fields[0] = true; // 添加联系人始终可用
		fields[1] = addresses != null && addresses.length > 0
				&& addresses[0] != null && !addresses[0].isEmpty();
		fields[2] = phoneNumbers != null && phoneNumbers.length > 0;
		fields[3] = emails != null && emails.length > 0;

		buttonCount = 0;
		for (int x = 0; x < MAX_BUTTON_COUNT; x++) {
			if (fields[x]) {
				buttonCount++;
			}
		}
	}

	@Override
	public int getButtonCount() {
		return buttonCount;
	}

	@Override
	public int getButtonText(int index) {
		return BUTTON_TEXTS[mapIndexToAction(index)];
	}

	@Override
	public void handleButtonPress(int index) {
		AddressBookParsedResult addressResult = (AddressBookParsedResult) getResult();
		String[] addresses = addressResult.getAddresses();
		String address1 = addresses == null || addresses.length < 1 ? null
				: addresses[0];
		String[] addressTypes = addressResult.getAddressTypes();
		String address1Type = addressTypes == null || addressTypes.length < 1 ? null
				: addressTypes[0];
		int action = mapIndexToAction(index);
		switch (action) {
		case 0:
			addContact(addressResult.getNames(), addressResult.getNicknames(),
					addressResult.getPronunciation(),
					addressResult.getPhoneNumbers(),
					addressResult.getPhoneTypes(), addressResult.getEmails(),
					addressResult.getEmailTypes(), addressResult.getNote(),
					addressResult.getInstantMessenger(), address1,
					address1Type, addressResult.getOrg(),
					addressResult.getTitle(), addressResult.getURLs(),
					addressResult.getBirthday(), addressResult.getGeo());
			break;
		case 1:
			searchMap(address1);
			break;
		case 2:
			dialPhone(addressResult.getPhoneNumbers()[0]);
			break;
		case 3:
			sendEmail(addressResult.getEmails(), null, null, null, null);
			break;
		default:
			break;
		}
	}

	private static Date parseDate(String s) {
		for (DateFormat currentFormat : DATE_FORMATS) {
			try {
				return currentFormat.parse(s);
			} catch (ParseException e) {
				// continue
			}
		}
		return null;
	}

	// 被覆盖，所以我们可以连接电话号码，格式生日，并加粗名称
	@Override
	public CharSequence getDisplayContents() {
		AddressBookParsedResult result = (AddressBookParsedResult) getResult();
		StringBuilder contents = new StringBuilder(100);
		ParsedResult.maybeAppend(result.getNames(), contents);
		int namesLength = contents.length();

		String pronunciation = result.getPronunciation();
		if (pronunciation != null && !pronunciation.isEmpty()) {
			contents.append("\n(");
			contents.append(pronunciation);
			contents.append(')');
		}

		ParsedResult.maybeAppend(result.getTitle(), contents);
		ParsedResult.maybeAppend(result.getOrg(), contents);
		ParsedResult.maybeAppend(result.getAddresses(), contents);
		String[] numbers = result.getPhoneNumbers();
		if (numbers != null) {
			for (String number : numbers) {
				if (number != null) {
					ParsedResult.maybeAppend(
							PhoneNumberUtils.formatNumber(number), contents);
				}
			}
		}
		ParsedResult.maybeAppend(result.getEmails(), contents);
		ParsedResult.maybeAppend(result.getURLs(), contents);

		String birthday = result.getBirthday();
		if (birthday != null && !birthday.isEmpty()) {
			Date date = parseDate(birthday);
			if (date != null) {
				ParsedResult.maybeAppend(
						DateFormat.getDateInstance(DateFormat.MEDIUM).format(
								date.getTime()), contents);
			}
		}
		ParsedResult.maybeAppend(result.getNote(), contents);

		if (namesLength > 0) {
			// 大胆的全名，使它脱颖而出
			Spannable styled = new SpannableString(contents.toString());
			styled.setSpan(new StyleSpan(Typeface.BOLD), 0, namesLength, 0);
			return styled;
		} else {
			return contents.toString();
		}
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_address_book;// 找到联系人信息
	}
}
