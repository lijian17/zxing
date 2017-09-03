package com.google.zxing.client.android.book;

import java.util.Locale;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.client.android.R;

/**
 * 显示此搜索结果的页码和片段的列表项
 * 
 * @author lijian
 * @date 2017-9-3 下午10:04:51
 */
public final class SearchBookContentsListItem extends LinearLayout {
	private TextView pageNumberView;
	private TextView snippetView;

	SearchBookContentsListItem(Context context) {
		super(context);
	}

	public SearchBookContentsListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		pageNumberView = (TextView) findViewById(R.id.page_number_view);
		snippetView = (TextView) findViewById(R.id.snippet_view);
	}

	public void set(SearchBookContentsResult result) {
		pageNumberView.setText(result.getPageNumber());
		String snippet = result.getSnippet();
		if (snippet.isEmpty()) {
			snippetView.setText("");
		} else {
			if (result.getValidSnippet()) {
				String lowerQuery = SearchBookContentsResult.getQuery()
						.toLowerCase(Locale.getDefault());
				String lowerSnippet = snippet.toLowerCase(Locale.getDefault());
				Spannable styledSnippet = new SpannableString(snippet);
				StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
				int queryLength = lowerQuery.length();
				int offset = 0;
				while (true) {
					int pos = lowerSnippet.indexOf(lowerQuery, offset);
					if (pos < 0) {
						break;
					}
					styledSnippet.setSpan(boldSpan, pos, pos + queryLength, 0);
					offset = pos + queryLength;
				}
				snippetView.setText(styledSnippet);
			} else {
				// 这可能是一个错误消息，因此请勿尝试粗体查询条款
				snippetView.setText(snippet);
			}
		}
	}
}
