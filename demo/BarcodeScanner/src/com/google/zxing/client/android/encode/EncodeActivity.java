package com.google.zxing.client.android.encode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

/**
 * 该类将来自Intent的数据编码为QR码，然后将其全屏显示，以便另一个人可以使用其设备进行扫描
 * 
 * @author lijian
 * @date 2017-9-5 下午11:01:04
 */
public final class EncodeActivity extends Activity {
	private static final String TAG = EncodeActivity.class.getSimpleName();

	private static final int MAX_BARCODE_FILENAME_LENGTH = 24;
	private static final Pattern NOT_ALPHANUMERIC = Pattern
			.compile("[^A-Za-z0-9]");
	private static final String USE_VCARD_KEY = "USE_VCARD";

	private QRCodeEncoder qrCodeEncoder;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Intent intent = getIntent();
		if (intent == null) {
			finish();
		} else {
			String action = intent.getAction();
			if (Intents.Encode.ACTION.equals(action)
					|| Intent.ACTION_SEND.equals(action)) {
				setContentView(R.layout.encode);
			} else {
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.encode, menu);
		boolean useVcard = qrCodeEncoder != null && qrCodeEncoder.isUseVCard();
		int encodeNameResource = useVcard ? R.string.menu_encode_mecard
				: R.string.menu_encode_vcard;
		MenuItem encodeItem = menu.findItem(R.id.menu_encode);
		encodeItem.setTitle(encodeNameResource);
		Intent intent = getIntent();
		if (intent != null) {
			String type = intent.getStringExtra(Intents.Encode.TYPE);
			encodeItem.setVisible(Contents.Type.CONTACT.equals(type));
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share:
			share();
			return true;
		case R.id.menu_encode:
			Intent intent = getIntent();
			if (intent == null) {
				return false;
			}
			intent.putExtra(USE_VCARD_KEY, !qrCodeEncoder.isUseVCard());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return true;
		default:
			return false;
		}
	}

	/**
	 * 分享
	 */
	private void share() {
		QRCodeEncoder encoder = qrCodeEncoder;
		if (encoder == null) { // Odd
			Log.w(TAG, "不存在要发送的条形码?");
			return;
		}

		String contents = encoder.getContents();
		if (contents == null) {
			Log.w(TAG, "不存在要发送的条形码?");
			return;
		}

		Bitmap bitmap;
		try {
			bitmap = encoder.encodeAsBitmap();
		} catch (WriterException we) {
			Log.w(TAG, we);
			return;
		}
		if (bitmap == null) {
			return;
		}

		File bsRoot = new File(Environment.getExternalStorageDirectory(),
				"BarcodeScanner");
		File barcodesRoot = new File(bsRoot, "Barcodes");
		if (!barcodesRoot.exists() && !barcodesRoot.mkdirs()) {
			Log.w(TAG, "不能mkdir " + barcodesRoot);
			showErrorMessage(R.string.msg_unmount_usb);
			return;
		}
		File barcodeFile = new File(barcodesRoot, makeBarcodeFileName(contents)
				+ ".png");
		if (!barcodeFile.delete()) {
			Log.w(TAG, "无法删除 " + barcodeFile);
			// 还是继续
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(barcodeFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
		} catch (FileNotFoundException fnfe) {
			Log.w(TAG, "无法访问文件 " + barcodeFile + " due to " + fnfe);
			showErrorMessage(R.string.msg_unmount_usb);
			return;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ioe) {
					// do nothing
				}
			}
		}

		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name)
				+ " - " + encoder.getTitle());
		intent.putExtra(Intent.EXTRA_TEXT, contents);
		intent.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + barcodeFile.getAbsolutePath()));
		intent.setType("image/png");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(Intent.createChooser(intent, null));
	}

	/**
	 * 创建条码文件名
	 * 
	 * @param contents
	 * @return
	 */
	private static CharSequence makeBarcodeFileName(CharSequence contents) {
		String fileName = NOT_ALPHANUMERIC.matcher(contents).replaceAll("_");
		if (fileName.length() > MAX_BARCODE_FILENAME_LENGTH) {
			fileName = fileName.substring(0, MAX_BARCODE_FILENAME_LENGTH);
		}
		return fileName;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 这是假设视图是全屏的，这是一个很好的假设
		WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		Point displaySize = new Point();
		display.getSize(displaySize);
		int width = displaySize.x;
		int height = displaySize.y;
		int smallerDimension = width < height ? width : height;
		smallerDimension = smallerDimension * 7 / 8;

		Intent intent = getIntent();
		if (intent == null) {
			return;
		}

		try {
			boolean useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
			qrCodeEncoder = new QRCodeEncoder(this, intent, smallerDimension,
					useVCard);
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			if (bitmap == null) {
				Log.w(TAG, "无法对条形码进行编码");
				showErrorMessage(R.string.msg_encode_contents_failed);
				qrCodeEncoder = null;
				return;
			}

			ImageView view = (ImageView) findViewById(R.id.image_view);
			view.setImageBitmap(bitmap);

			TextView contents = (TextView) findViewById(R.id.contents_text_view);
			if (intent.getBooleanExtra(Intents.Encode.SHOW_CONTENTS, true)) {
				contents.setText(qrCodeEncoder.getDisplayContents());
				setTitle(qrCodeEncoder.getTitle());
			} else {
				contents.setText("");
				setTitle("");
			}
		} catch (WriterException e) {
			Log.w(TAG, "无法对条形码进行编码", e);
			showErrorMessage(R.string.msg_encode_contents_failed);
			qrCodeEncoder = null;
		}
	}

	/**
	 * 显示错误消息
	 * 
	 * @param message
	 */
	private void showErrorMessage(int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}
}
