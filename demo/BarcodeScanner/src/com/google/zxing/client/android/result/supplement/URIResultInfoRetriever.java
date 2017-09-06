package com.google.zxing.client.android.result.supplement;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.widget.TextView;

import com.google.zxing.client.android.HttpHelper;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.result.URIParsedResult;

/**
 * URI结果信息检索器
 * 
 * @author lijian
 * @date 2017-9-6 下午10:00:38
 */
final class URIResultInfoRetriever extends SupplementalInfoRetriever {

	private static final int MAX_REDIRECTS = 5;

	private final URIParsedResult result;
	private final String redirectString;

	/**
	 * 
	 * URI结果信息检索器
	 * 
	 * @param textView
	 * @param result
	 * @param historyManager
	 * @param context
	 */
	URIResultInfoRetriever(TextView textView, URIParsedResult result,
			HistoryManager historyManager, Context context) {
		super(textView, historyManager);
		redirectString = context.getString(R.string.msg_redirect);
		this.result = result;
	}

	@Override
	void retrieveSupplementalInfo() throws IOException {
		URI oldURI;
		try {
			oldURI = new URI(result.getURI());
		} catch (URISyntaxException ignored) {
			return;
		}
		URI newURI = HttpHelper.unredirect(oldURI);
		int count = 0;
		while (count++ < MAX_REDIRECTS && !oldURI.equals(newURI)) {
			append(result.getDisplayResult(), null,
					new String[] { redirectString + " : " + newURI },
					newURI.toString());
			oldURI = newURI;
			newURI = HttpHelper.unredirect(newURI);
		}
	}

}
