package com.google.zxing.client.android.history;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

/**
 * 数据库帮助类
 * 
 * @author lijian
 * @date 2017-8-16 下午5:01:44
 */
final class DBHelper extends SQLiteOpenHelper {

	private static final int DB_VERSION = 5;
	private static final String DB_NAME = "barcode_scanner_history.db";
	static final String TABLE_NAME = "history";
	static final String ID_COL = "id";
	static final String TEXT_COL = "text";
	static final String FORMAT_COL = "format";
	static final String DISPLAY_COL = "display";
	static final String TIMESTAMP_COL = "timestamp";
	static final String DETAILS_COL = "details";

	DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + TEXT_COL + " TEXT, " + FORMAT_COL
				+ " TEXT, " + DISPLAY_COL + " TEXT, " + TIMESTAMP_COL
				+ " INTEGER, " + DETAILS_COL + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion,
			int newVersion) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(sqLiteDatabase);
	}

}
