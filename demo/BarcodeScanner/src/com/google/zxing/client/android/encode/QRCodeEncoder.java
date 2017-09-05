package com.google.zxing.client.android.encode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitMatrix;

/**
 * 该类执行解码用户请求并提取要在条形码中编码的所有数据的工作
 * 
 * @author lijian
 * @date 2017-9-4 下午11:02:34
 */
final class QRCodeEncoder {
	private static final String TAG = QRCodeEncoder.class.getSimpleName();

	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	/** 上下文 */
	private final Context activity;
	/** 内容 */
	private String contents;
	/** 显示内容 */
	private String displayContents;
	/** 标题 */
	private String title;
	/** 条码格式化器 */
	private BarcodeFormat format;
	/** 尺寸 */
	private final int dimension;
	/** 使用名片 */
	private final boolean useVCard;

	/**
	 * QRCode编码器
	 * 
	 * @param activity
	 * @param intent
	 * @param dimension
	 * @param useVCard
	 * @throws WriterException
	 */
	QRCodeEncoder(Context activity, Intent intent, int dimension,
			boolean useVCard) throws WriterException {
		this.activity = activity;
		this.dimension = dimension;
		this.useVCard = useVCard;
		String action = intent.getAction();
		if (Intents.Encode.ACTION.equals(action)) {
			encodeContentsFromZXingIntent(intent);
		} else if (Intent.ACTION_SEND.equals(action)) {
			encodeContentsFromShareIntent(intent);
		}
	}

	/**
	 * 获取内容
	 * 
	 * @return
	 */
	String getContents() {
		return contents;
	}

	/**
	 * 显示内容
	 * 
	 * @return
	 */
	String getDisplayContents() {
		return displayContents;
	}

	/**
	 * 获取标题
	 * 
	 * @return
	 */
	String getTitle() {
		return title;
	}

	/**
	 * 使用名片
	 * 
	 * @return
	 */
	boolean isUseVCard() {
		return useVCard;
	}

	/**
	 * 如果字符串编码住在核心的ZXing库中，那么这个很好，但是我们使用平台特定的代码，如PhoneNumberUtils，所以它不能
	 * 
	 * @param intent
	 */
	private void encodeContentsFromZXingIntent(Intent intent) {
		// 如果没有给定格式，则默认为QR_CODE
		String formatString = intent.getStringExtra(Intents.Encode.FORMAT);
		format = null;
		if (formatString != null) {
			try {
				format = BarcodeFormat.valueOf(formatString);
			} catch (IllegalArgumentException iae) {
				// Ignore it then
			}
		}
		if (format == null || format == BarcodeFormat.QR_CODE) {
			String type = intent.getStringExtra(Intents.Encode.TYPE);
			if (type != null && !type.isEmpty()) {
				this.format = BarcodeFormat.QR_CODE;
				encodeQRCodeContents(intent, type);
			}
		} else {
			String data = intent.getStringExtra(Intents.Encode.DATA);
			if (data != null && !data.isEmpty()) {
				contents = data;
				displayContents = data;
				title = activity.getString(R.string.contents_text);
			}
		}
	}

	/**
	 * Handles从许多Android应用程序发送意图
	 * 
	 * @param intent
	 * @throws WriterException
	 */
	private void encodeContentsFromShareIntent(Intent intent)
			throws WriterException {
		// 检查这是否是纯文本编码，或联系人
		if (intent.hasExtra(Intent.EXTRA_STREAM)) {
			encodeFromStreamExtra(intent);
		} else {
			encodeFromTextExtras(intent);
		}
	}

	/**
	 * 从文本附加编码
	 * 
	 * @param intent
	 * @throws WriterException
	 */
	private void encodeFromTextExtras(Intent intent) throws WriterException {
		// 注意：Google地图在一个文本中共享URL和详细信息!
		String theContents = ContactEncoder.trim(intent
				.getStringExtra(Intent.EXTRA_TEXT));
		if (theContents == null) {
			theContents = ContactEncoder.trim(intent
					.getStringExtra("android.intent.extra.HTML_TEXT"));
			// Intent.EXTRA_HTML_TEXT
			if (theContents == null) {
				theContents = ContactEncoder.trim(intent
						.getStringExtra(Intent.EXTRA_SUBJECT));
				if (theContents == null) {
					String[] emails = intent
							.getStringArrayExtra(Intent.EXTRA_EMAIL);
					if (emails != null) {
						theContents = ContactEncoder.trim(emails[0]);
					} else {
						theContents = "?";
					}
				}
			}
		}

		// 修剪文字以避免网址破坏
		if (theContents == null || theContents.isEmpty()) {
			throw new WriterException("Empty EXTRA_TEXT");
		}
		contents = theContents;
		// 我们只做QR码
		format = BarcodeFormat.QR_CODE;
		if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
			displayContents = intent.getStringExtra(Intent.EXTRA_SUBJECT);
		} else if (intent.hasExtra(Intent.EXTRA_TITLE)) {
			displayContents = intent.getStringExtra(Intent.EXTRA_TITLE);
		} else {
			displayContents = contents;
		}
		title = activity.getString(R.string.contents_text);
	}

	/**
	 * 处理来自联系人应用程序发送的意图，获取联系人的VCARD
	 * 
	 * @param intent
	 * @throws WriterException
	 */
	private void encodeFromStreamExtra(Intent intent) throws WriterException {
		format = BarcodeFormat.QR_CODE;
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			throw new WriterException("没有额外的");
		}
		Uri uri = bundle.getParcelable(Intent.EXTRA_STREAM);
		if (uri == null) {
			throw new WriterException("No EXTRA_STREAM");
		}
		byte[] vcard;
		String vcardString;
		InputStream stream = null;
		try {
			stream = activity.getContentResolver().openInputStream(uri);
			if (stream == null) {
				throw new WriterException("无法打开流stream " + uri);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[2048];
			int bytesRead;
			while ((bytesRead = stream.read(buffer)) > 0) {
				baos.write(buffer, 0, bytesRead);
			}
			vcard = baos.toByteArray();
			vcardString = new String(vcard, 0, vcard.length, "UTF-8");
		} catch (IOException ioe) {
			throw new WriterException(ioe);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// continue
				}
			}
		}
		Log.d(TAG, "编码共享意图内容:");
		Log.d(TAG, vcardString);
		Result result = new Result(vcardString, vcard, null,
				BarcodeFormat.QR_CODE);
		ParsedResult parsedResult = ResultParser.parseResult(result);
		if (!(parsedResult instanceof AddressBookParsedResult)) {
			throw new WriterException("结果不是地址");
		}
		encodeQRCodeContents((AddressBookParsedResult) parsedResult);
		if (contents == null || contents.isEmpty()) {
			throw new WriterException("无内容编码");
		}
	}

	/**
	 * 编码QRCode内容
	 * 
	 * @param intent
	 * @param type
	 */
	private void encodeQRCodeContents(Intent intent, String type) {
		if (Contents.Type.TEXT.equals(type)) {
			String textData = intent.getStringExtra(Intents.Encode.DATA);
			if (textData != null && !textData.isEmpty()) {
				contents = textData;
				displayContents = textData;
				title = activity.getString(R.string.contents_text);
			}
		} else if (Contents.Type.EMAIL.equals(type)) {
			String emailData = ContactEncoder.trim(intent
					.getStringExtra(Intents.Encode.DATA));
			if (emailData != null) {
				contents = "mailto:" + emailData;
				displayContents = emailData;
				title = activity.getString(R.string.contents_email);
			}
		} else if (Contents.Type.PHONE.equals(type)) {
			String phoneData = ContactEncoder.trim(intent
					.getStringExtra(Intents.Encode.DATA));
			if (phoneData != null) {
				contents = "tel:" + phoneData;
				displayContents = PhoneNumberUtils.formatNumber(phoneData);
				title = activity.getString(R.string.contents_phone);
			}
		} else if (Contents.Type.SMS.equals(type)) {
			String smsData = ContactEncoder.trim(intent
					.getStringExtra(Intents.Encode.DATA));
			if (smsData != null) {
				contents = "sms:" + smsData;
				displayContents = PhoneNumberUtils.formatNumber(smsData);
				title = activity.getString(R.string.contents_sms);
			}
		} else if (Contents.Type.CONTACT.equals(type)) {
			Bundle contactBundle = intent.getBundleExtra(Intents.Encode.DATA);
			if (contactBundle != null) {

				String name = contactBundle
						.getString(ContactsContract.Intents.Insert.NAME);
				String organization = contactBundle
						.getString(ContactsContract.Intents.Insert.COMPANY);
				String address = contactBundle
						.getString(ContactsContract.Intents.Insert.POSTAL);
				List<String> phones = getAllBundleValues(contactBundle,
						Contents.PHONE_KEYS);
				List<String> phoneTypes = getAllBundleValues(contactBundle,
						Contents.PHONE_TYPE_KEYS);
				List<String> emails = getAllBundleValues(contactBundle,
						Contents.EMAIL_KEYS);
				String url = contactBundle.getString(Contents.URL_KEY);
				List<String> urls = url == null ? null : Collections
						.singletonList(url);
				String note = contactBundle.getString(Contents.NOTE_KEY);

				ContactEncoder encoder = useVCard ? new VCardContactEncoder()
						: new MECARDContactEncoder();
				String[] encoded = encoder.encode(
						Collections.singletonList(name), organization,
						Collections.singletonList(address), phones, phoneTypes,
						emails, urls, note);
				// 确保我们编码了至少一个字段
				if (!encoded[1].isEmpty()) {
					contents = encoded[0];
					displayContents = encoded[1];
					title = activity.getString(R.string.contents_contact);
				}

			}
		} else if (Contents.Type.LOCATION.equals(type)) {
			Bundle locationBundle = intent.getBundleExtra(Intents.Encode.DATA);
			if (locationBundle != null) {
				// 这些必须使用Bundle.getFloat()，而不是getDouble()，它是API的一部分。
				float latitude = locationBundle
						.getFloat("LAT", Float.MAX_VALUE);
				float longitude = locationBundle.getFloat("LONG",
						Float.MAX_VALUE);
				if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
					contents = "geo:" + latitude + ',' + longitude;
					displayContents = latitude + "," + longitude;
					title = activity.getString(R.string.contents_location);
				}
			}
		}
	}

	/**
	 * 获取所有bundle值
	 * 
	 * @param bundle
	 * @param keys
	 * @return
	 */
	private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
		List<String> values = new ArrayList<String>(keys.length);
		for (String key : keys) {
			Object value = bundle.get(key);
			values.add(value == null ? null : value.toString());
		}
		return values;
	}

	/**
	 * 编码QRCode内容
	 * 
	 * @param contact
	 */
	private void encodeQRCodeContents(AddressBookParsedResult contact) {
		ContactEncoder encoder = useVCard ? new VCardContactEncoder()
				: new MECARDContactEncoder();
		String[] encoded = encoder.encode(toList(contact.getNames()),
				contact.getOrg(), toList(contact.getAddresses()),
				toList(contact.getPhoneNumbers()), null,
				toList(contact.getEmails()), toList(contact.getURLs()), null);
		// 确保我们编码了至少一个字段
		if (!encoded[1].isEmpty()) {
			contents = encoded[0];
			displayContents = encoded[1];
			title = activity.getString(R.string.contents_contact);
		}
	}

	private static List<String> toList(String[] values) {
		return values == null ? null : Arrays.asList(values);
	}

	/**
	 * 编码为Bitmap
	 * 
	 * @return
	 * @throws WriterException
	 */
	Bitmap encodeAsBitmap() throws WriterException {
		String contentsToEncode = contents;
		if (contentsToEncode == null) {
			return null;
		}
		Map<EncodeHintType, Object> hints = null;
		String encoding = guessAppropriateEncoding(contentsToEncode);
		if (encoding != null) {
			hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
			hints.put(EncodeHintType.CHARACTER_SET, encoding);
		}
		BitMatrix result;
		try {
			result = new MultiFormatWriter().encode(contentsToEncode, format,
					dimension, dimension, hints);
		} catch (IllegalArgumentException iae) {
			// 不支持的格式
			return null;
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	/**
	 * 猜猜适当的编码
	 * 
	 * @param contents
	 * @return
	 */
	private static String guessAppropriateEncoding(CharSequence contents) {
		// 现在很粗糙
		for (int i = 0; i < contents.length(); i++) {
			if (contents.charAt(i) > 0xFF) {
				return "UTF-8";
			}
		}
		return null;
	}

}
