package com.google.zxing.client.android.share;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.clipboard.ClipboardInterface;

/**
 * 条形码扫描器可以通过在屏幕上显示QR码来共享数据，如联系人和书签，以便其他用户可以使用手机扫描条形码
 * 
 * @author lijian
 * @date 2017-9-3 下午6:59:52
 */
public final class ShareActivity extends Activity {
	private static final String TAG = ShareActivity.class.getSimpleName();

	/** 选择书签 */
	private static final int PICK_BOOKMARK = 0;
	/** 选择联系 */
	private static final int PICK_CONTACT = 1;
	/** 选择APP */
	private static final int PICK_APP = 2;

	/** 剪贴板按钮 */
	private View clipboardButton;

	private final View.OnClickListener contactListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK,
					ContactsContract.Contacts.CONTENT_URI);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivityForResult(intent, PICK_CONTACT);
		}
	};

	private final View.OnClickListener bookmarkListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(ShareActivity.this,
					BookmarkPickerActivity.class.getName());
			startActivityForResult(intent, PICK_BOOKMARK);
		}
	};

	private final View.OnClickListener appListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(ShareActivity.this,
					AppPickerActivity.class.getName());
			startActivityForResult(intent, PICK_APP);
		}
	};

	private final View.OnClickListener clipboardListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// 应该永远是真的，因为我们灰色的onResume()的剪贴板按钮是空的
			CharSequence text = ClipboardInterface.getText(ShareActivity.this);
			if (text != null) {
				launchSearch(text.toString());
			}
		}
	};

	private final View.OnKeyListener textListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View view, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				String text = ((TextView) view).getText().toString();
				if (text != null && !text.isEmpty()) {
					launchSearch(text);
				}
				return true;
			}
			return false;
		}
	};

	/**
	 * 发起搜索
	 * 
	 * @param text
	 */
	private void launchSearch(String text) {
		Intent intent = new Intent(Intents.Encode.ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
		intent.putExtra(Intents.Encode.DATA, text);
		intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
		startActivity(intent);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.share);

		findViewById(R.id.share_contact_button).setOnClickListener(
				contactListener);
		if (Build.VERSION.SDK_INT >= 23) { // Marshmallow / 6.0
			// 无法访问6.0+以上的书签
			findViewById(R.id.share_bookmark_button).setEnabled(false);
		} else {
			findViewById(R.id.share_bookmark_button).setOnClickListener(
					bookmarkListener);
		}
		findViewById(R.id.share_app_button).setOnClickListener(appListener);
		clipboardButton = findViewById(R.id.share_clipboard_button);
		clipboardButton.setOnClickListener(clipboardListener);
		findViewById(R.id.share_text_view).setOnKeyListener(textListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		clipboardButton.setEnabled(ClipboardInterface.hasText(this));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case PICK_BOOKMARK:
			case PICK_APP:
				showTextAsBarcode(intent.getStringExtra("url")); // Browser.BookmarkColumns.URL
				break;
			case PICK_CONTACT:
				// 数据字段是 content://contacts/people/984
				showContactAsBarcode(intent.getData());
				break;
			}
		}
	}

	/**
	 * 将文本显示为条形码
	 * 
	 * @param text
	 */
	private void showTextAsBarcode(String text) {
		Log.i(TAG, "显示文字为条形码: " + text);
		if (text == null) {
			return; // 显示错误?
		}
		Intent intent = new Intent(Intents.Encode.ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
		intent.putExtra(Intents.Encode.DATA, text);
		intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
		startActivity(intent);
	}

	/**
	 * 联系Uri并进行必要的数据库查找以检索该人的信息，然后发送一个Encode意图将其作为QR码。
	 * 
	 * @param contactUri
	 *            A Uri of the form content://contacts/people/17
	 */
	private void showContactAsBarcode(Uri contactUri) {
		Log.i(TAG, "显示联系URI作为条形码: " + contactUri);
		if (contactUri == null) {
			return; // 显示错误?
		}
		ContentResolver resolver = getContentResolver();

		Cursor cursor;
		try {
			// 尽管我不明白为什么，我们每周都会看到六个报告
			cursor = resolver.query(contactUri, null, null, null, null);
		} catch (IllegalArgumentException ignored) {
			return;
		}
		if (cursor == null) {
			return;
		}

		String id;
		String name;
		boolean hasPhone;
		try {
			if (!cursor.moveToFirst()) {
				return;
			}

			id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
			name = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			hasPhone = cursor
					.getInt(cursor
							.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;

		} finally {
			cursor.close();
		}

		// 不要求名字存在，这个联系人可能只是一个电话号码
		Bundle bundle = new Bundle();
		if (name != null && !name.isEmpty()) {
			bundle.putString(ContactsContract.Intents.Insert.NAME,
					massageContactData(name));
		}

		if (hasPhone) {
			Cursor phonesCursor = resolver.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + '='
							+ id, null, null);
			if (phonesCursor != null) {
				try {
					int foundPhone = 0;
					int phonesNumberColumn = phonesCursor
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
					int phoneTypeColumn = phonesCursor
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
					while (phonesCursor.moveToNext()
							&& foundPhone < Contents.PHONE_KEYS.length) {
						String number = phonesCursor
								.getString(phonesNumberColumn);
						if (number != null && !number.isEmpty()) {
							bundle.putString(Contents.PHONE_KEYS[foundPhone],
									massageContactData(number));
						}
						int type = phonesCursor.getInt(phoneTypeColumn);
						bundle.putInt(Contents.PHONE_TYPE_KEYS[foundPhone],
								type);
						foundPhone++;
					}
				} finally {
					phonesCursor.close();
				}
			}
		}

		Cursor methodsCursor = resolver.query(
				ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
				null,
				ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID
						+ '=' + id, null, null);
		if (methodsCursor != null) {
			try {
				if (methodsCursor.moveToNext()) {
					String data = methodsCursor
							.getString(methodsCursor
									.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
					if (data != null && !data.isEmpty()) {
						bundle.putString(
								ContactsContract.Intents.Insert.POSTAL,
								massageContactData(data));
					}
				}
			} finally {
				methodsCursor.close();
			}
		}

		Cursor emailCursor = resolver.query(
				ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
				ContactsContract.CommonDataKinds.Email.CONTACT_ID + '=' + id,
				null, null);
		if (emailCursor != null) {
			try {
				int foundEmail = 0;
				int emailColumn = emailCursor
						.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
				while (emailCursor.moveToNext()
						&& foundEmail < Contents.EMAIL_KEYS.length) {
					String email = emailCursor.getString(emailColumn);
					if (email != null && !email.isEmpty()) {
						bundle.putString(Contents.EMAIL_KEYS[foundEmail],
								massageContactData(email));
					}
					foundEmail++;
				}
			} finally {
				emailCursor.close();
			}
		}

		Intent intent = new Intent(Intents.Encode.ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra(Intents.Encode.TYPE, Contents.Type.CONTACT);
		intent.putExtra(Intents.Encode.DATA, bundle);
		intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());

		Log.i(TAG, "发送bundle编码: " + bundle);
		startActivity(intent);
	}

	/**
	 * 处理联系人数据中的换行问题
	 * 
	 * @param data
	 * @return
	 */
	private static String massageContactData(String data) {
		// 现在 - 确保我们不要在共享联系人数据中添加换行符。 它弄乱了任何已知的联系人数据编码。 用空格替换
		if (data.indexOf('\n') >= 0) {
			data = data.replace("\n", " ");
		}
		if (data.indexOf('\r') >= 0) {
			data = data.replace("\r", " ");
		}
		return data;
	}
}
