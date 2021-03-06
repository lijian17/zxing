package com.google.zxing.client.android.book;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.zxing.client.android.HttpHelper;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.R;

/**
 * 使用Google图书搜索来查找所需书籍中的单词或短语。
 * 
 * @author lijian
 * @date 2017-9-2 下午1:03:06
 */
public final class SearchBookContentsActivity extends Activity {
	private static final String TAG = SearchBookContentsActivity.class
			.getSimpleName();

	/** TAG模板 */
	private static final Pattern TAG_PATTERN = Pattern.compile("\\<.*?\\>");
	/** <模板 */
	private static final Pattern LT_ENTITY_PATTERN = Pattern.compile("&lt;");
	/** >模板 */
	private static final Pattern GT_ENTITY_PATTERN = Pattern.compile("&gt;");
	/** "模板 */
	private static final Pattern QUOTE_ENTITY_PATTERN = Pattern
			.compile("&#39;");
	/** "模板 */
	private static final Pattern QUOT_ENTITY_PATTERN = Pattern
			.compile("&quot;");

	private String isbn;
	private EditText queryTextView;
	private View queryButton;
	private ListView resultListView;
	private TextView headerView;
	private AsyncTask<String, ?, ?> networkTask;

	private final View.OnClickListener buttonListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			launchSearch();
		}
	};

	private final View.OnKeyListener keyListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View view, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				launchSearch();
				return true;
			}
			return false;
		}
	};

	String getISBN() {
		return isbn;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// 确保在启动时删除过期的Cookie。
		CookieSyncManager.createInstance(this);
		CookieManager.getInstance().removeExpiredCookie();

		Intent intent = getIntent();
		if (intent == null
				|| !Intents.SearchBookContents.ACTION
						.equals(intent.getAction())) {
			finish();
			return;
		}

		// Google 图书搜索
		isbn = intent.getStringExtra(Intents.SearchBookContents.ISBN);
		if (LocaleManager.isBookSearchUrl(isbn)) {
			setTitle(getString(R.string.sbc_name));
		} else {
			setTitle(getString(R.string.sbc_name) + ": ISBN " + isbn);
		}

		setContentView(R.layout.search_book_contents);
		queryTextView = (EditText) findViewById(R.id.query_text_view);

		String initialQuery = intent
				.getStringExtra(Intents.SearchBookContents.QUERY);
		if (initialQuery != null && !initialQuery.isEmpty()) {
			// 填充搜索框，但不要触发搜索
			queryTextView.setText(initialQuery);
		}
		queryTextView.setOnKeyListener(keyListener);

		queryButton = findViewById(R.id.query_button);
		queryButton.setOnClickListener(buttonListener);

		resultListView = (ListView) findViewById(R.id.result_list_view);
		LayoutInflater factory = LayoutInflater.from(this);
		headerView = (TextView) factory.inflate(
				R.layout.search_book_contents_header, resultListView, false);
		resultListView.addHeaderView(headerView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		queryTextView.selectAll();
	}

	@Override
	protected void onPause() {
		AsyncTask<?, ?, ?> oldTask = networkTask;
		if (oldTask != null) {
			oldTask.cancel(true);
			networkTask = null;
		}
		super.onPause();
	}

	/**
	 * 启动搜索
	 */
	private void launchSearch() {
		String query = queryTextView.getText().toString();
		if (query != null && !query.isEmpty()) {
			AsyncTask<?, ?, ?> oldTask = networkTask;
			if (oldTask != null) {
				oldTask.cancel(true);
			}
			networkTask = new NetworkTask();
			networkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					query, isbn);
			headerView.setText(R.string.msg_sbc_searching_book);
			resultListView.setAdapter(null);
			queryTextView.setEnabled(false);
			queryButton.setEnabled(false);
		}
	}

	private final class NetworkTask extends
			AsyncTask<String, Object, JSONObject> {

		@Override
		protected JSONObject doInBackground(String... args) {
			try {
				// 这些返回一个JSON结果，用于描述查询是否在何处以及何处找到。 此API可能会在将来的任何时候中断或消失。
				// 由于这是一个API调用而不是一个网站，因此我们不使用LocaleManager更改TLD。
				String theQuery = args[0];
				String theIsbn = args[1];
				String uri;
				if (LocaleManager.isBookSearchUrl(theIsbn)) {
					int equals = theIsbn.indexOf('=');
					String volumeId = theIsbn.substring(equals + 1);
					uri = "http://www.google.com/books?id=" + volumeId
							+ "&jscmd=SearchWithinVolume2&q=" + theQuery;
				} else {
					uri = "http://www.google.com/books?vid=isbn" + theIsbn
							+ "&jscmd=SearchWithinVolume2&q=" + theQuery;
				}
				CharSequence content = HttpHelper.downloadViaHttp(uri,
						HttpHelper.ContentType.JSON);
				return new JSONObject(content.toString());
			} catch (IOException ioe) {
				Log.w(TAG, "访问图书搜索时出错", ioe);
				return null;
			} catch (JSONException je) {
				Log.w(TAG, "访问图书搜索时出错-返回的JSON数据异常", je);
				return null;
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			if (result == null) {
				// 抱歉，搜索时遇到错误
				headerView.setText(R.string.msg_sbc_failed);
			} else {
				handleSearchResults(result);
			}
			queryTextView.setEnabled(true);
			queryTextView.selectAll();
			queryButton.setEnabled(true);
		}

		// 目前没有办法区分没有结果的查询和不可搜索的书 - 都返回0结果。
		private void handleSearchResults(JSONObject json) {
			try {
				int count = json.getInt("number_of_results");
				headerView.setText(getString(R.string.msg_sbc_results) + " : "
						+ count);
				if (count > 0) {
					JSONArray results = json.getJSONArray("search_results");
					SearchBookContentsResult.setQuery(queryTextView.getText()
							.toString());
					List<SearchBookContentsResult> items = new ArrayList<SearchBookContentsResult>(
							count);
					for (int x = 0; x < count; x++) {
						items.add(parseResult(results.getJSONObject(x)));
					}
					resultListView
							.setOnItemClickListener(new BrowseBookListener(
									SearchBookContentsActivity.this, items));
					resultListView.setAdapter(new SearchBookContentsAdapter(
							SearchBookContentsActivity.this, items));
				} else {
					String searchable = json.optString("searchable");
					if ("false".equals(searchable)) {
						headerView
								.setText(R.string.msg_sbc_book_not_searchable);
					}
					resultListView.setAdapter(null);
				}
			} catch (JSONException e) {
				Log.w(TAG, "图书搜索中的错误JSON", e);
				resultListView.setAdapter(null);
				headerView.setText(R.string.msg_sbc_failed);
			}
		}

		// 可用字段：page_id,page_number,snippet_text
		private SearchBookContentsResult parseResult(JSONObject json) {

			String pageId;
			String pageNumber;
			String snippet;
			try {
				pageId = json.getString("page_id");
				pageNumber = json.optString("page_number");
				snippet = json.optString("snippet_text");
			} catch (JSONException e) {
				Log.w(TAG, e);
				// 在野外从来没有见过，刚刚完成。
				return new SearchBookContentsResult(
						getString(R.string.msg_sbc_no_page_returned), "", "",
						false);
			}

			if (pageNumber == null || pageNumber.isEmpty()) {
				// 这可能发生在jacket上的文字，也可能是其他原因
				pageNumber = "";
			} else {
				pageNumber = getString(R.string.msg_sbc_page) + ' '
						+ pageNumber;
			}

			boolean valid = snippet != null && !snippet.isEmpty();
			if (valid) {
				// 删除所有HTML标签和编码字符
				snippet = TAG_PATTERN.matcher(snippet).replaceAll("");
				snippet = LT_ENTITY_PATTERN.matcher(snippet).replaceAll("<");
				snippet = GT_ENTITY_PATTERN.matcher(snippet).replaceAll(">");
				snippet = QUOTE_ENTITY_PATTERN.matcher(snippet).replaceAll("'");
				snippet = QUOT_ENTITY_PATTERN.matcher(snippet).replaceAll("\"");
			} else {
				// 摘录不可用
				snippet = '(' + getString(R.string.msg_sbc_snippet_unavailable) + ')';
			}

			return new SearchBookContentsResult(pageId, pageNumber, snippet,
					valid);
		}

	}

}
