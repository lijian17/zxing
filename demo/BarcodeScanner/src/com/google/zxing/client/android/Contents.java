package com.google.zxing.client.android;

import android.provider.ContactsContract;

/**
 * 当发送条形码扫描器要求对条形码进行编码的意图时使用的常量集合。
 * 
 * @author lijian
 * @date 2017-9-1 下午7:05:45
 */
public final class Contents {
	private Contents() {
	}

	/**
	 * 包含发送Intents时使用的类型常量。
	 */
	public static final class Type {
		/**
		 * 纯文本。 使用Intent.putExtra(DATA，string)。
		 * 这也可以用于URL，但字符串必须包含"http://"或"https://"。
		 */
		public static final String TEXT = "TEXT_TYPE";

		/**
		 * email类型. 使用Intent.putExtra(DATA, string) 其中string是email地址.
		 */
		public static final String EMAIL = "EMAIL_TYPE";

		/**
		 * 使用Intent.putExtra(DATA, string) 其中string是要拨打的电话号码.
		 */
		public static final String PHONE = "PHONE_TYPE";

		/**
		 * SMS类型. 使用Intent.putExtra(DATA, string)其中string是SMS的号码.
		 */
		public static final String SMS = "SMS_TYPE";

		/**
		 * 联系人 发送请求进行编码，如下所示: {@code
		 * import android.provider.Contacts;
		 * 
		 * Intent intent = new Intent(Intents.Encode.ACTION);
		 * intent.putExtra(Intents.Encode.TYPE, CONTACT);
		 * Bundle bundle = new Bundle();
		 * bundle.putString(ContactsContract.Intents.Insert.NAME, "李建");
		 * bundle.putString(ContactsContract.Intents.Insert.PHONE, "1234567");
		 * bundle.putString(ContactsContract.Intents.Insert.EMAIL, "374452668@qq.com");
		 * bundle.putString(ContactsContract.Intents.Insert.POSTAL, "中国北京");
		 * intent.putExtra(Intents.Encode.DATA, bundle);
		 * }
		 */
		public static final String CONTACT = "CONTACT_TYPE";

		/**
		 * 地理位置。 使用如下: Bundle bundle = new Bundle(); bundle.putFloat("LAT",
		 * latitude); bundle.putFloat("LONG", longitude);
		 * intent.putExtra(Intents.Encode.DATA, bundle);
		 */
		public static final String LOCATION = "LOCATION_TYPE";

		private Type() {
		}
	}

	public static final String URL_KEY = "URL_KEY";

	public static final String NOTE_KEY = "NOTE_KEY";

	/**
	 * 当使用Type.CONTACT时，这些数组提供添加或检索多个电话号码和地址的key
	 */
	public static final String[] PHONE_KEYS = {
			ContactsContract.Intents.Insert.PHONE,
			ContactsContract.Intents.Insert.SECONDARY_PHONE,
			ContactsContract.Intents.Insert.TERTIARY_PHONE };

	public static final String[] PHONE_TYPE_KEYS = {
			ContactsContract.Intents.Insert.PHONE_TYPE,
			ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
			ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE };

	public static final String[] EMAIL_KEYS = {
			ContactsContract.Intents.Insert.EMAIL,
			ContactsContract.Intents.Insert.SECONDARY_EMAIL,
			ContactsContract.Intents.Insert.TERTIARY_EMAIL };

	public static final String[] EMAIL_TYPE_KEYS = {
			ContactsContract.Intents.Insert.EMAIL_TYPE,
			ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE,
			ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE };

}
