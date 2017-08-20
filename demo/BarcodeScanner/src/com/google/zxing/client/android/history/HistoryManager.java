package com.google.zxing.client.android.history;

import android.database.sqlite.SQLiteException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.result.ResultHandler;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 管理与扫描历史相关的功能。
 * 
 * @author lijian
 * @date 2017-8-16 下午5:03:06
 */
public final class HistoryManager {
	private static final String TAG = HistoryManager.class.getSimpleName();

	private static final int MAX_ITEMS = 2000;

	private static final String[] COLUMNS = { DBHelper.TEXT_COL,
			DBHelper.DISPLAY_COL, DBHelper.FORMAT_COL, DBHelper.TIMESTAMP_COL,
			DBHelper.DETAILS_COL, };

	private static final String[] COUNT_COLUMN = { "COUNT(1)" };

	private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };
	private static final String[] ID_DETAIL_COL_PROJECTION = { DBHelper.ID_COL,
			DBHelper.DETAILS_COL };

	private final Activity activity;
	/** 是否启用历史记录 */
	private final boolean enableHistory;

	public HistoryManager(Activity activity) {
		this.activity = activity;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		enableHistory = prefs.getBoolean(
				PreferencesActivity.KEY_ENABLE_HISTORY, true);
	}

	/**
	 * 是否有历史条目
	 * 
	 * @return
	 */
	public boolean hasHistoryItems() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, COUNT_COLUMN, null, null,
					null, null, null);
			cursor.moveToFirst();
			return cursor.getInt(0) > 0;
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * 构建历史条目
	 * 
	 * @return
	 */
	public List<HistoryItem> buildHistoryItems() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		List<HistoryItem> items = new ArrayList<HistoryItem>();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null,
					null, DBHelper.TIMESTAMP_COL + " DESC");
			while (cursor.moveToNext()) {
				String text = cursor.getString(0);
				String display = cursor.getString(1);
				String format = cursor.getString(2);
				long timestamp = cursor.getLong(3);
				String details = cursor.getString(4);
				Result result = new Result(text, null, null,
						BarcodeFormat.valueOf(format), timestamp);
				items.add(new HistoryItem(result, display, details));
			}
		} finally {
			close(cursor, db);
		}
		return items;
	}

	/**
	 * 根据指定编号构建一条历史条目
	 * 
	 * @param number
	 * @return
	 */
	public HistoryItem buildHistoryItem(int number) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getReadableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null,
					null, DBHelper.TIMESTAMP_COL + " DESC");
			cursor.move(number + 1);
			String text = cursor.getString(0);
			String display = cursor.getString(1);
			String format = cursor.getString(2);
			long timestamp = cursor.getLong(3);
			String details = cursor.getString(4);
			Result result = new Result(text, null, null,
					BarcodeFormat.valueOf(format), timestamp);
			return new HistoryItem(result, display, details);
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * 根据ID删除指定历史条目
	 * 
	 * @param number
	 */
	public void deleteHistoryItem(int number) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, ID_COL_PROJECTION, null,
					null, null, null, DBHelper.TIMESTAMP_COL + " DESC");
			cursor.move(number + 1);
			db.delete(DBHelper.TABLE_NAME,
					DBHelper.ID_COL + '=' + cursor.getString(0), null);
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * 添加历史条目
	 * @param result
	 * @param handler
	 */
	public void addHistoryItem(Result result, ResultHandler handler) {
		// 如果偏好设置已关闭，或内容被认为是安全的，请勿将此项目保存到历史记录中。
		if (!activity.getIntent().getBooleanExtra(Intents.Scan.SAVE_HISTORY,
				true)
				|| handler.areContentsSecure() || !enableHistory) {
			return;
		}

		// 是否要保存重复记录
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(activity);
		if (!prefs.getBoolean(PreferencesActivity.KEY_REMEMBER_DUPLICATES,
				false)) {
			deletePrevious(result.getText());
		}

		ContentValues values = new ContentValues();
		values.put(DBHelper.TEXT_COL, result.getText());
		values.put(DBHelper.FORMAT_COL, result.getBarcodeFormat().toString());
		values.put(DBHelper.DISPLAY_COL, handler.getDisplayContents()
				.toString());
		values.put(DBHelper.TIMESTAMP_COL, System.currentTimeMillis());

		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		try {
			db = helper.getWritableDatabase();
			// 将新条目插入数据库
			db.insert(DBHelper.TABLE_NAME, DBHelper.TIMESTAMP_COL, values);
		} finally {
			close(null, db);
		}
	}

	/**
	 * 添加历史条目详情
	 * @param itemID
	 * @param itemDetails
	 */
	public void addHistoryItemDetails(String itemID, String itemDetails) {
		// 因为我们只做一个更新，我们不需要担心的偏好; 如果该项目没有保存它不会被udpated
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, ID_DETAIL_COL_PROJECTION,
					DBHelper.TEXT_COL + "=?", new String[] { itemID }, null,
					null, DBHelper.TIMESTAMP_COL + " DESC", "1");
			String oldID = null;
			String oldDetails = null;
			if (cursor.moveToNext()) {
				oldID = cursor.getString(0);
				oldDetails = cursor.getString(1);
			}

			if (oldID != null) {
				String newDetails;
				if (oldDetails == null) {
					newDetails = itemDetails;
				} else if (oldDetails.contains(itemDetails)) {
					newDetails = null;
				} else {
					newDetails = oldDetails + " : " + itemDetails;
				}
				if (newDetails != null) {
					ContentValues values = new ContentValues();
					values.put(DBHelper.DETAILS_COL, newDetails);
					db.update(DBHelper.TABLE_NAME, values, DBHelper.ID_COL
							+ "=?", new String[] { oldID });
				}
			}

		} finally {
			close(cursor, db);
		}
	}

	private void deletePrevious(String text) {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		try {
			db = helper.getWritableDatabase();
			db.delete(DBHelper.TABLE_NAME, DBHelper.TEXT_COL + "=?",
					new String[] { text });
		} finally {
			close(null, db);
		}
	}

	public void trimHistory() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, ID_COL_PROJECTION, null,
					null, null, null, DBHelper.TIMESTAMP_COL + " DESC");
			cursor.move(MAX_ITEMS);
			while (cursor.moveToNext()) {
				String id = cursor.getString(0);
				Log.i(TAG, "Deleting scan history ID " + id);
				db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + id, null);
			}
		} catch (SQLiteException sqle) {
			// 在极少数情况下，在CaptureActivity.onCreate（）中调用时，我们看到错误，不明白。 第一个理论是它是短暂的，所以可以安全地忽略。
			Log.w(TAG, sqle);
			// 继续
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * <p>
	 * 构建扫描历史记录的文本表示。 每个扫描都在一行上编码，以换行符（\r\n）终止。 
	 * 每一行中的值都以逗号分隔，并加上双引号。 值中的双引号以两个双引号的顺序进行转义。 输出的字段有：
	 * </p>
	 * 
	 * <ol>
	 * <li>Raw text</li>
	 * <li>Display text</li>
	 * <li>Format (e.g. QR_CODE)</li>
	 * <li>Unix timestamp (milliseconds since the epoch)</li>
	 * <li>Formatted version of timestamp</li>
	 * <li>Supplemental info (e.g. price info for a product barcode)</li>
	 * </ol>
	 */
	CharSequence buildHistory() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = helper.getWritableDatabase();
			cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null,
					null, DBHelper.TIMESTAMP_COL + " DESC");

			DateFormat format = DateFormat.getDateTimeInstance(
					DateFormat.MEDIUM, DateFormat.MEDIUM);
			StringBuilder historyText = new StringBuilder(1000);
			while (cursor.moveToNext()) {

				historyText.append('"')
						.append(massageHistoryField(cursor.getString(0)))
						.append("\",");
				historyText.append('"')
						.append(massageHistoryField(cursor.getString(1)))
						.append("\",");
				historyText.append('"')
						.append(massageHistoryField(cursor.getString(2)))
						.append("\",");
				historyText.append('"')
						.append(massageHistoryField(cursor.getString(3)))
						.append("\",");

				// 再次添加时间戳，格式化
				long timestamp = cursor.getLong(3);
				historyText
						.append('"')
						.append(massageHistoryField(format.format(new Date(
								timestamp)))).append("\",");

				// 以上我们将保留格式化数据位置5的列的旧排序
				historyText.append('"')
						.append(massageHistoryField(cursor.getString(4)))
						.append("\"\r\n");
			}
			return historyText;
		} finally {
			close(cursor, db);
		}
	}

	/**
	 * 清除历史
	 */
	void clearHistory() {
		SQLiteOpenHelper helper = new DBHelper(activity);
		SQLiteDatabase db = null;
		try {
			db = helper.getWritableDatabase();
			db.delete(DBHelper.TABLE_NAME, null, null);
		} finally {
			close(null, db);
		}
	}

	/**
	 * 保存历史
	 * @param history
	 * @return
	 */
	static Uri saveHistory(String history) {
		File bsRoot = new File(Environment.getExternalStorageDirectory(),
				"BarcodeScanner");
		File historyRoot = new File(bsRoot, "History");
		if (!historyRoot.exists() && !historyRoot.mkdirs()) {
			Log.w(TAG, "Couldn't make dir " + historyRoot);
			return null;
		}
		File historyFile = new File(historyRoot, "history-"
				+ System.currentTimeMillis() + ".csv");
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(historyFile),
					Charset.forName("UTF-8"));
			out.write(history);
			return Uri.parse("file://" + historyFile.getAbsolutePath());
		} catch (IOException ioe) {
			Log.w(TAG, "Couldn't access file " + historyFile + " due to " + ioe);
			return null;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ioe) {
					// do nothing
				}
			}
		}
	}

	private static String massageHistoryField(String value) {
		return value == null ? "" : value.replace("\"", "\"\"");
	}

	private static void close(Cursor cursor, SQLiteDatabase database) {
		if (cursor != null) {
			cursor.close();
		}
		if (database != null) {
			database.close();
		}
	}

}
