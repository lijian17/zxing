package com.google.zxing.client.android.share;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * 此类仅需要，因为我无法成功发送ACTION_PICK意图到com.android.browser.BrowserBookmarksPage。
 * 如果将来开始工作，它可能会消失。
 * 
 * @author lijian
 * @date 2017-9-3 下午7:08:01
 */
public final class BookmarkPickerActivity extends ListActivity {
	private static final String TAG = BookmarkPickerActivity.class
			.getSimpleName();

	/** 书签投影 */
	private static final String[] BOOKMARK_PROJECTION = {//
	"title", // Browser.BookmarkColumns.TITLE
			"url", // Browser.BookmarkColumns.URL
	};
	// 复制来自android.provider.Browser.BOOKMARKS_URI:
	private static final Uri BOOKMARKS_URI = Uri
			.parse("content://browser/bookmarks");

	private static final String BOOKMARK_SELECTION = "bookmark = 1 AND url IS NOT NULL";

	private final List<String[]> titleURLs = new ArrayList<String[]>();

	@Override
	protected void onResume() {
		super.onResume();
		titleURLs.clear();
		Cursor cursor = getContentResolver().query(BOOKMARKS_URI,
				BOOKMARK_PROJECTION, BOOKMARK_SELECTION, null, null);
		if (cursor == null) {
			Log.w(TAG, "没有返回书签查询的游标");
			finish();
			return;
		}
		try {
			while (cursor.moveToNext()) {
				titleURLs.add(new String[] { cursor.getString(0),
						cursor.getString(1) });
			}
		} finally {
			cursor.close();
		}
		setListAdapter(new BookmarkAdapter(this, titleURLs));
	}

	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {
		String[] titleURL = titleURLs.get(position);
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra("title", titleURL[0]); // Browser.BookmarkColumns.TITLE
		intent.putExtra("url", titleURL[1]); // Browser.BookmarkColumns.URL
		setResult(RESULT_OK, intent);
		finish();
	}
}
