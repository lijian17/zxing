package com.google.zxing.client.android.result;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.zxing.Result;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.book.SearchBookContentsActivity;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;

/**
 * 结果处理器<br>
 * Android专用条形码处理程序的基础类。 这些允许应用程序多态地为每种数据类型建议适当的操作。 <br>
 * 此类还包含一些实用方法来执行常见操作，如打开URL。 它们可以很容易地被移动到辅助对象中，但它不能是静态的，因为Activity实例需要启动一个意图。
 * 
 * @author lijian
 * @date 2017-9-1 下午6:17:33
 */
/**
 * @author lijian
 * @date 2017-9-1 下午11:34:14
 */
public abstract class ResultHandler {
	private static final String TAG = ResultHandler.class.getSimpleName();

	/** email类型的字符串 */
	private static final String[] EMAIL_TYPE_STRINGS = { "home", "work",
			"mobile" };
	/** phone类型的字符串 */
	private static final String[] PHONE_TYPE_STRINGS = { "home", "work",
			"mobile", "fax", "pager", "main" };
	/** address类型的字符串 */
	private static final String[] ADDRESS_TYPE_STRINGS = { "home", "work" };
	/** email类型的值 */
	private static final int[] EMAIL_TYPE_VALUES = {
			ContactsContract.CommonDataKinds.Email.TYPE_HOME,
			ContactsContract.CommonDataKinds.Email.TYPE_WORK,
			ContactsContract.CommonDataKinds.Email.TYPE_MOBILE, };
	/** phone类型的值 */
	private static final int[] PHONE_TYPE_VALUES = {
			ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
			ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
			ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
			ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
			ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
			ContactsContract.CommonDataKinds.Phone.TYPE_MAIN, };
	/** address类型的值 */
	private static final int[] ADDRESS_TYPE_VALUES = {
			ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
			ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK, };
	/** no类型 */
	private static final int NO_TYPE = -1;

	/** 最大按钮计数 */
	public static final int MAX_BUTTON_COUNT = 4;

	/** 解析结果 */
	private final ParsedResult result;
	/** Activity */
	private final Activity activity;
	/** 源结果 */
	private final Result rawResult;
	/** 定制产品搜索 */
	private final String customProductSearch;

	/**
	 * 结果处理器
	 * 
	 * @param activity
	 * @param result
	 */
	ResultHandler(Activity activity, ParsedResult result) {
		this(activity, result, null);
	}

	/**
	 * 结果处理器
	 * 
	 * @param activity
	 * @param result
	 * @param rawResult
	 */
	ResultHandler(Activity activity, ParsedResult result, Result rawResult) {
		this.result = result;
		this.activity = activity;
		this.rawResult = rawResult;
		this.customProductSearch = parseCustomSearchURL();
	}

	/**
	 * 获取结果
	 * 
	 * @return
	 */
	public final ParsedResult getResult() {
		return result;
	}

	/**
	 * 是否有定制产品搜索
	 * 
	 * @return
	 */
	final boolean hasCustomProductSearch() {
		return customProductSearch != null;
	}

	final Activity getActivity() {
		return activity;
	}

	/**
	 * 指示派生类想要显示多少个按钮。
	 * 
	 * @return
	 */
	public abstract int getButtonCount();

	/**
	 * 第n个动作按钮的文本
	 * 
	 * @param index
	 *            取值范围(0~getButtonCount() - 1)
	 * @return
	 */
	public abstract int getButtonText(int index);

	/**
	 * 获取默认按钮ID
	 * 
	 * @return
	 */
	public Integer getDefaultButtonID() {
		return null;
	}

	/**
	 * 执行与第n个按钮对应的动作。
	 * 
	 * @param index
	 *            点击的按钮
	 */
	public abstract void handleButtonPress(int index);

	/**
	 * 一些条形码内容被认为是安全的，不应该保存到历史记录中，复制到剪贴板或以其他方式保存。
	 * 
	 * @return 如果为true，请勿创建任何这些内容的永久记录。
	 */
	public boolean areContentsSecure() {
		return false;
	}

	/**
	 * 为当前条形码的内容创建一个可能风格的字符串。
	 * 
	 * @return 要显示的文字
	 */
	public CharSequence getDisplayContents() {
		String contents = result.getDisplayResult();
		return contents.replace("\r", "");
	}

	/**
	 * 描述发现的条形码类型的字符串，例如。 “找到联系方式”。
	 * 
	 * @return 字符串的资源ID。
	 */
	public abstract int getDisplayTitle();

	/**
	 * 一个方便的方法来获取解析的类型。 不应该被覆盖。
	 * 
	 * @return 被解析的类型，例如 URI或ISBN
	 */
	public final ParsedResultType getType() {
		return result.getType();
	}

	/**
	 * 只添加电话联系
	 * 
	 * @param phoneNumbers
	 *            电话号码
	 * @param phoneTypes
	 *            手机类型
	 */
	final void addPhoneOnlyContact(String[] phoneNumbers, String[] phoneTypes) {
		addContact(null, null, null, phoneNumbers, phoneTypes, null, null,
				null, null, null, null, null, null, null, null, null);
	}

	/**
	 * 只添加电子邮件联系
	 * 
	 * @param emails
	 * @param emailTypes
	 */
	final void addEmailOnlyContact(String[] emails, String[] emailTypes) {
		addContact(null, null, null, null, null, emails, emailTypes, null,
				null, null, null, null, null, null, null, null);
	}

	/**
	 * 增加联系人
	 * 
	 * @param names
	 *            姓名
	 * @param nicknames
	 *            昵称
	 * @param pronunciation
	 * @param phoneNumbers
	 *            电话号码
	 * @param phoneTypes
	 *            电话类型（座机/手机）
	 * @param emails
	 *            email
	 * @param emailTypes
	 *            email类型
	 * @param note
	 *            备注
	 * @param instantMessenger
	 *            即时通讯
	 * @param address
	 *            地址
	 * @param addressType
	 *            地址类型（家庭住址/公司住址）
	 * @param org
	 *            组织
	 * @param title
	 *            标题
	 * @param urls
	 *            链接
	 * @param birthday
	 *            生日
	 * @param geo
	 *            地理坐标
	 */
	final void addContact(String[] names, String[] nicknames,
			String pronunciation, String[] phoneNumbers, String[] phoneTypes,
			String[] emails, String[] emailTypes, String note,
			String instantMessenger, String address, String addressType,
			String org, String title, String[] urls, String birthday,
			String[] geo) {

		// 只能使用阵列中的名字（如果存在）。
		Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT,
				ContactsContract.Contacts.CONTENT_URI);
		intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		putExtra(intent, ContactsContract.Intents.Insert.NAME,
				names != null ? names[0] : null);

		putExtra(intent, ContactsContract.Intents.Insert.PHONETIC_NAME,
				pronunciation);

		int phoneCount = Math.min(phoneNumbers != null ? phoneNumbers.length
				: 0, Contents.PHONE_KEYS.length);
		for (int x = 0; x < phoneCount; x++) {
			putExtra(intent, Contents.PHONE_KEYS[x], phoneNumbers[x]);
			if (phoneTypes != null && x < phoneTypes.length) {
				int type = toPhoneContractType(phoneTypes[x]);
				if (type >= 0) {
					intent.putExtra(Contents.PHONE_TYPE_KEYS[x], type);
				}
			}
		}

		int emailCount = Math.min(emails != null ? emails.length : 0,
				Contents.EMAIL_KEYS.length);
		for (int x = 0; x < emailCount; x++) {
			putExtra(intent, Contents.EMAIL_KEYS[x], emails[x]);
			if (emailTypes != null && x < emailTypes.length) {
				int type = toEmailContractType(emailTypes[x]);
				if (type >= 0) {
					intent.putExtra(Contents.EMAIL_TYPE_KEYS[x], type);
				}
			}
		}

		ArrayList<ContentValues> data = new ArrayList<ContentValues>();
		if (urls != null) {
			for (String url : urls) {
				if (url != null && !url.isEmpty()) {
					ContentValues row = new ContentValues(2);
					row.put(ContactsContract.Data.MIMETYPE,
							ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
					row.put(ContactsContract.CommonDataKinds.Website.URL, url);
					data.add(row);
					break;
				}
			}
		}

		if (birthday != null) {
			ContentValues row = new ContentValues(3);
			row.put(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
			row.put(ContactsContract.CommonDataKinds.Event.TYPE,
					ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
			row.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday);
			data.add(row);
		}

		if (nicknames != null) {
			for (String nickname : nicknames) {
				if (nickname != null && !nickname.isEmpty()) {
					ContentValues row = new ContentValues(3);
					row.put(ContactsContract.Data.MIMETYPE,
							ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
					row.put(ContactsContract.CommonDataKinds.Nickname.TYPE,
							ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT);
					row.put(ContactsContract.CommonDataKinds.Nickname.NAME,
							nickname);
					data.add(row);
					break;
				}
			}
		}

		if (!data.isEmpty()) {
			intent.putParcelableArrayListExtra(
					ContactsContract.Intents.Insert.DATA, data);
		}

		StringBuilder aggregatedNotes = new StringBuilder();
		if (note != null) {
			aggregatedNotes.append('\n').append(note);
		}
		if (geo != null) {
			aggregatedNotes.append('\n').append(geo[0]).append(',')
					.append(geo[1]);
		}

		if (aggregatedNotes.length() > 0) {
			// 删除额外的'\n'
			putExtra(intent, ContactsContract.Intents.Insert.NOTES,
					aggregatedNotes.substring(1));
		}

		putExtra(intent, ContactsContract.Intents.Insert.IM_HANDLE,
				instantMessenger);
		putExtra(intent, ContactsContract.Intents.Insert.POSTAL, address);
		if (addressType != null) {
			int type = toAddressContractType(addressType);
			if (type >= 0) {
				intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE,
						type);
			}
		}
		putExtra(intent, ContactsContract.Intents.Insert.COMPANY, org);
		putExtra(intent, ContactsContract.Intents.Insert.JOB_TITLE, title);
		launchIntent(intent);
	}

	/**
	 * email转换
	 * 
	 * @param typeString
	 * @return
	 */
	private static int toEmailContractType(String typeString) {
		return doToContractType(typeString, EMAIL_TYPE_STRINGS,
				EMAIL_TYPE_VALUES);
	}

	/**
	 * 电话转换
	 * 
	 * @param typeString
	 * @return
	 */
	private static int toPhoneContractType(String typeString) {
		return doToContractType(typeString, PHONE_TYPE_STRINGS,
				PHONE_TYPE_VALUES);
	}

	/**
	 * 地址转换
	 * 
	 * @param typeString
	 * @return
	 */
	private static int toAddressContractType(String typeString) {
		return doToContractType(typeString, ADDRESS_TYPE_STRINGS,
				ADDRESS_TYPE_VALUES);
	}

	/**
	 * 执行电话本类型转换
	 * 
	 * @param typeString
	 * @param types
	 * @param values
	 * @return
	 */
	private static int doToContractType(String typeString, String[] types,
			int[] values) {
		if (typeString == null) {
			return NO_TYPE;
		}
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			if (typeString.startsWith(type)
					|| typeString.startsWith(type.toUpperCase(Locale.ENGLISH))) {
				return values[i];
			}
		}
		return NO_TYPE;
	}

	/**
	 * 分享到email
	 * 
	 * @param contents
	 */
	final void shareByEmail(String contents) {
		sendEmail(null, null, null, null, contents);
	}

	/**
	 * 发送email
	 * 
	 * @param to
	 *            邮件将发送至该地址
	 * @param cc
	 *            邮件将抄送至地址
	 * @param bcc
	 *            邮件将密送至该地址
	 * @param subject
	 *            邮件主题
	 * @param body
	 *            邮件内容
	 */
	final void sendEmail(String[] to, String[] cc, String[] bcc,
			String subject, String body) {
		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
		if (to != null && to.length != 0) {
			intent.putExtra(Intent.EXTRA_EMAIL, to);
		}
		if (cc != null && cc.length != 0) {
			intent.putExtra(Intent.EXTRA_CC, cc);
		}
		if (bcc != null && bcc.length != 0) {
			intent.putExtra(Intent.EXTRA_BCC, bcc);
		}
		putExtra(intent, Intent.EXTRA_SUBJECT, subject);
		putExtra(intent, Intent.EXTRA_TEXT, body);
		intent.setType("text/plain");
		launchIntent(intent);
	}

	/**
	 * 分享至SMS
	 * 
	 * @param contents
	 */
	final void shareBySMS(String contents) {
		sendSMSFromUri("smsto:", contents);
	}

	/**
	 * 发送至SMS
	 * 
	 * @param phoneNumber
	 * @param body
	 */
	final void sendSMS(String phoneNumber, String body) {
		sendSMSFromUri("smsto:" + phoneNumber, body);
	}

	/**
	 * 发送
	 * 
	 * @param uri
	 * @param body
	 */
	private void sendSMSFromUri(String uri, String body) {
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
		putExtra(intent, "sms_body", body);
		// 短信发送后退出应用程序
		intent.putExtra("compose_mode", true);
		launchIntent(intent);
	}

	/**
	 * 发送彩信
	 * 
	 * @param phoneNumber
	 * @param subject
	 * @param body
	 */
	final void sendMMS(String phoneNumber, String subject, String body) {
		sendMMSFromUri("mmsto:" + phoneNumber, subject, body);
	}

	/**
	 * 从Uri发送彩信
	 * 
	 * @param uri
	 * @param subject
	 * @param body
	 */
	private void sendMMSFromUri(String uri, String subject, String body) {
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
		// 消息应用程序需要查看有效的主题，否则会将其视为短信。
		if (subject == null || subject.isEmpty()) {
			putExtra(intent, "subject",
					activity.getString(R.string.msg_default_mms_subject));
		} else {
			putExtra(intent, "subject", subject);
		}
		putExtra(intent, "sms_body", body);
		intent.putExtra("compose_mode", true);
		launchIntent(intent);
	}

	/**
	 * 拨电话
	 * 
	 * @param phoneNumber
	 */
	final void dialPhone(String phoneNumber) {
		launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
				+ phoneNumber)));
	}

	/**
	 * 从Uri拨打电话
	 * 
	 * @param uri
	 */
	final void dialPhoneFromUri(String uri) {
		launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse(uri)));
	}

	/**
	 * 打开地图
	 * 
	 * @param geoURI
	 */
	final void openMap(String geoURI) {
		launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)));
	}

	/**
	 * 搜索地图<br>
	 * 使用地址作为查询进行地理搜索。
	 * 
	 * @param address
	 *            要找的地址
	 */
	final void searchMap(String address) {
		launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="
				+ Uri.encode(address))));
	}

	/**
	 * 获取路线
	 * 
	 * @param latitude
	 * @param longitude
	 */
	final void getDirections(double latitude, double longitude) {
		launchIntent(new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://maps.google."
						+ LocaleManager.getCountryTLD(activity)
						+ "/maps?f=d&daddr=" + latitude + ',' + longitude)));
	}

	/**
	 * 打开产品搜索<br>
	 * 使用针对小屏幕进行格式化的移动专用版产品搜索。
	 * 
	 * @param upc
	 */
	final void openProductSearch(String upc) {
		Uri uri = Uri.parse("http://www.google."
				+ LocaleManager.getProductSearchCountryTLD(activity)
				+ "/m/products?q=" + upc + "&source=zxing");
		launchIntent(new Intent(Intent.ACTION_VIEW, uri));
	}

	/**
	 * 打开图书搜索
	 * 
	 * @param isbn
	 */
	final void openBookSearch(String isbn) {
		Uri uri = Uri.parse("http://books.google."
				+ LocaleManager.getBookSearchCountryTLD(activity)
				+ "/books?vid=isbn" + isbn);
		launchIntent(new Intent(Intent.ACTION_VIEW, uri));
	}

	/**
	 * 搜索书籍内容
	 * 
	 * @param isbnOrUrl
	 */
	final void searchBookContents(String isbnOrUrl) {
		Intent intent = new Intent(Intents.SearchBookContents.ACTION);
		intent.setClassName(activity,
				SearchBookContentsActivity.class.getName());
		putExtra(intent, Intents.SearchBookContents.ISBN, isbnOrUrl);
		launchIntent(intent);
	}

	/**
	 * 打开URL
	 * 
	 * @param url
	 */
	final void openURL(String url) {
		// 奇怪的是，一些Android浏览器似乎没有注册来处理HTTP://或HTTPS:// 小写这些，因为它应该总是可以小写这些方案。
		if (url.startsWith("HTTP://")) {
			url = "http" + url.substring(4);
		} else if (url.startsWith("HTTPS://")) {
			url = "https" + url.substring(5);
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			launchIntent(intent);
		} catch (ActivityNotFoundException ignored) {
			Log.w(TAG, "没有什么可以处理的 " + intent);
		}
	}

	/**
	 * web搜索
	 * 
	 * @param query
	 */
	final void webSearch(String query) {
		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
		intent.putExtra("query", query);
		launchIntent(intent);
	}

	/**
	 * 像{@link #launchIntent(Intent)}，但会告诉你，如果它不能通过
	 * {@link ActivityNotFoundException}处理。
	 * 
	 * @param intent
	 * @throws ActivityNotFoundException
	 *             如果Intent无法处理
	 */
	final void rawLaunchIntent(Intent intent) {
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			Log.d(TAG, "启动意图: " + intent + " 与额外: " + intent.getExtras());
			activity.startActivity(intent);
		}
	}

	/**
	 * 像原始的启动意图{@link #rawLaunchIntent(Intent)}，但是如果没有可用的话可以显示一个用户对话框。
	 * 
	 * @param intent
	 */
	final void launchIntent(Intent intent) {
		try {
			rawLaunchIntent(intent);
		} catch (ActivityNotFoundException ignored) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.app_name);
			builder.setMessage(R.string.msg_intent_failed);
			builder.setPositiveButton(R.string.button_ok, null);
			builder.show();
		}
	}

	/**
	 * 给意图添加数据，过滤空值数据
	 * 
	 * @param intent
	 * @param key
	 * @param value
	 */
	private static void putExtra(Intent intent, String key, String value) {
		if (value != null && !value.isEmpty()) {
			intent.putExtra(key, value);
		}
	}

	/**
	 * 解析自定义搜索URL
	 * 
	 * @return
	 */
	private String parseCustomSearchURL() {
		// 从本地sp获取，如果没有则返回null
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		String customProductSearch = prefs.getString(
				PreferencesActivity.KEY_CUSTOM_PRODUCT_SEARCH, null);
		if (customProductSearch != null && customProductSearch.trim().isEmpty()) {
			return null;
		}
		return customProductSearch;
	}

	/**
	 * 填写自定义搜索网址
	 * 
	 * @param text
	 * @return
	 */
	final String fillInCustomSearchURL(String text) {
		if (customProductSearch == null) {
			return text; // ?
		}
		try {
			text = URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// 不能发生; 始终支持UTF-8。 继续，我猜，没有编码
		}
		String url = customProductSearch;
		if (rawResult != null) {
			// 替换%f，但仅当它似乎不是十六进制转义序列时。 这仍然是有问题的，但避免了更令人惊讶的突破性逃脱问题
			url = url.replaceFirst("%f(?![0-9a-f])", rawResult
					.getBarcodeFormat().toString());
			if (url.contains("%t")) {
				ParsedResult parsedResultAgain = ResultParser
						.parseResult(rawResult);
				url = url.replace("%t", parsedResultAgain.getType().toString());
			}
		}
		// 最后替换%s，因为它可能包含自己的%f或%t
		return url.replace("%s", text);
	}

}
