package com.google.zxing.client.android.book;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;

import com.google.zxing.client.android.LocaleManager;

/**
 * 浏览书监听器
 * 
 * @author lijian
 * @date 2017-9-3 下午10:08:26
 */
final class BrowseBookListener implements AdapterView.OnItemClickListener {

	private final SearchBookContentsActivity activity;
	private final List<SearchBookContentsResult> items;

	/**
	 * 浏览书监听器
	 * 
	 * @param activity
	 * @param items
	 */
	BrowseBookListener(SearchBookContentsActivity activity,
			List<SearchBookContentsResult> items) {
		this.activity = activity;
		this.items = items;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (position < 1) {
			// 点击标题，忽略它
			return;
		}
		int itemOffset = position - 1;
		if (itemOffset >= items.size()) {
			return;
		}
		String pageId = items.get(itemOffset).getPageId();
		String query = SearchBookContentsResult.getQuery();
		if (LocaleManager.isBookSearchUrl(activity.getISBN())
				&& !pageId.isEmpty()) {
			String uri = activity.getISBN();
			int equals = uri.indexOf('=');
			String volumeId = uri.substring(equals + 1);
			String readBookURI = "http://books.google."
					+ LocaleManager.getBookSearchCountryTLD(activity)
					+ "/books?id=" + volumeId + "&pg=" + pageId + "&vq="
					+ query;
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(readBookURI));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			activity.startActivity(intent);
		}
	}
}
