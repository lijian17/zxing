package com.google.zxing.client.android.book;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.zxing.client.android.R;

/**
 * 制作代表SBC结果的列表项
 * 
 * @author lijian
 * @date 2017-9-3 下午10:06:54
 */
final class SearchBookContentsAdapter extends
		ArrayAdapter<SearchBookContentsResult> {

	/**
	 * 制作代表SBC结果的列表项
	 * 
	 * @param context
	 * @param items
	 */
	SearchBookContentsAdapter(Context context,
			List<SearchBookContentsResult> items) {
		super(context, R.layout.search_book_contents_list_item, 0, items);
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		SearchBookContentsListItem listItem;

		if (view == null) {
			LayoutInflater factory = LayoutInflater.from(getContext());
			listItem = (SearchBookContentsListItem) factory.inflate(
					R.layout.search_book_contents_list_item, viewGroup, false);
		} else {
			if (view instanceof SearchBookContentsListItem) {
				listItem = (SearchBookContentsListItem) view;
			} else {
				return view;
			}
		}

		SearchBookContentsResult result = getItem(position);
		listItem.set(result);
		return listItem;
	}
}
