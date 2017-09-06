package com.google.zxing.client.android.result.supplement;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * 超类可以异步检索有关条形码扫描的更多信息
 * 
 * @author lijian
 * @date 2017-9-6 下午10:26:32
 */
public abstract class SupplementalInfoRetriever extends
		AsyncTask<Object, Object, Object> {
	private static final String TAG = "SupplementalInfo";

	/**
	 * 也许调用检索
	 * 
	 * @param textView
	 * @param result
	 * @param historyManager
	 * @param context
	 */
	public static void maybeInvokeRetrieval(TextView textView,
			ParsedResult result, HistoryManager historyManager, Context context) {
		try {
			if (result instanceof URIParsedResult) {
				SupplementalInfoRetriever uriRetriever = new URIResultInfoRetriever(
						textView, (URIParsedResult) result, historyManager,
						context);
				uriRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				SupplementalInfoRetriever titleRetriever = new TitleRetriever(
						textView, (URIParsedResult) result, historyManager);
				titleRetriever
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else if (result instanceof ProductParsedResult) {
				ProductParsedResult productParsedResult = (ProductParsedResult) result;
				String productID = productParsedResult.getProductID();
				SupplementalInfoRetriever productRetriever = new ProductResultInfoRetriever(
						textView, productID, historyManager, context);
				productRetriever
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else if (result instanceof ISBNParsedResult) {
				String isbn = ((ISBNParsedResult) result).getISBN();
				SupplementalInfoRetriever productInfoRetriever = new ProductResultInfoRetriever(
						textView, isbn, historyManager, context);
				productInfoRetriever
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				SupplementalInfoRetriever bookInfoRetriever = new BookResultInfoRetriever(
						textView, isbn, historyManager, context);
				bookInfoRetriever
						.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		} catch (RejectedExecutionException ree) {
			// do nothing
		}
	}

	/** 弱引用 */
	private final WeakReference<TextView> textViewRef;
	private final WeakReference<HistoryManager> historyManagerRef;
	private final Collection<Spannable> newContents;
	private final Collection<String[]> newHistories;

	/**
	 * 超类可以异步检索有关条形码扫描的更多信息
	 * 
	 * @param textView
	 * @param historyManager
	 */
	SupplementalInfoRetriever(TextView textView, HistoryManager historyManager) {
		textViewRef = new WeakReference<TextView>(textView);
		historyManagerRef = new WeakReference<HistoryManager>(historyManager);
		newContents = new ArrayList<Spannable>();
		newHistories = new ArrayList<String[]>();
	}

	@Override
	public final Object doInBackground(Object... args) {
		try {
			retrieveSupplementalInfo();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
		return null;
	}

	@Override
	protected final void onPostExecute(Object arg) {
		TextView textView = textViewRef.get();
		if (textView != null) {
			for (CharSequence content : newContents) {
				textView.append(content);
			}
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		HistoryManager historyManager = historyManagerRef.get();
		if (historyManager != null) {
			for (String[] text : newHistories) {
				historyManager.addHistoryItemDetails(text[0], text[1]);
			}
		}
	}

	abstract void retrieveSupplementalInfo() throws IOException;

	/**
	 * 添加
	 * 
	 * @param itemID
	 * @param source
	 * @param newTexts
	 * @param linkURL
	 */
	final void append(String itemID, String source, String[] newTexts,
			String linkURL) {

		StringBuilder newTextCombined = new StringBuilder();

		if (source != null) {
			newTextCombined.append(source).append(' ');
		}

		int linkStart = newTextCombined.length();

		boolean first = true;
		for (String newText : newTexts) {
			if (first) {
				newTextCombined.append(newText);
				first = false;
			} else {
				newTextCombined.append(" [");
				newTextCombined.append(newText);
				newTextCombined.append(']');
			}
		}

		int linkEnd = newTextCombined.length();

		String newText = newTextCombined.toString();
		Spannable content = new SpannableString(newText + "\n\n");
		if (linkURL != null) {
			// 奇怪的是，一些Android浏览器似乎没有注册来处理HTTP:// or HTTPS://.
			// 小写这些，因为它应该总是可以小写这些方案
			if (linkURL.startsWith("HTTP://")) {
				linkURL = "http" + linkURL.substring(4);
			} else if (linkURL.startsWith("HTTPS://")) {
				linkURL = "https" + linkURL.substring(5);
			}
			content.setSpan(new URLSpan(linkURL), linkStart, linkEnd,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		newContents.add(content);
		newHistories.add(new String[] { itemID, newText });
	}

	/**
	 * 也许添加文本
	 * 
	 * @param text
	 * @param texts
	 */
	static void maybeAddText(String text, Collection<String> texts) {
		if (text != null && !text.isEmpty()) {
			texts.add(text);
		}
	}

	/**
	 * 也许添加文本系列
	 * 
	 * @param textSeries
	 * @param texts
	 */
	static void maybeAddTextSeries(Collection<String> textSeries,
			Collection<String> texts) {
		if (textSeries != null && !textSeries.isEmpty()) {
			boolean first = true;
			StringBuilder authorsText = new StringBuilder();
			for (String author : textSeries) {
				if (first) {
					first = false;
				} else {
					authorsText.append(", ");
				}
				authorsText.append(author);
			}
			texts.add(authorsText.toString());
		}
	}

}
