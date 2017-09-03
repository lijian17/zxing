package com.google.zxing.client.android.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

/**
 * 剪贴板<br>
 * 抽象在管理复制和粘贴的{@link ClipboardManager} API
 * 
 * @author lijian
 * @date 2017-9-3 下午7:55:54
 */
public final class ClipboardInterface {
	private static final String TAG = ClipboardInterface.class.getSimpleName();

	private ClipboardInterface() {
	}

	/**
	 * 从剪贴板获取文本
	 * 
	 * @param context
	 * @return
	 */
	public static CharSequence getText(Context context) {
		ClipboardManager clipboard = getManager(context);
		ClipData clip = clipboard.getPrimaryClip();
		return hasText(context) ? clip.getItemAt(0).coerceToText(context)
				: null;
	}

	/**
	 * 设置文本内容至剪贴板
	 * 
	 * @param text
	 * @param context
	 */
	public static void setText(CharSequence text, Context context) {
		if (text != null) {
			try {
				getManager(context).setPrimaryClip(
						ClipData.newPlainText(null, text));
			} catch (NullPointerException e) {
				// 在wild看到这个，很奇怪
				Log.w(TAG, "剪贴板bug", e);
			} catch (IllegalStateException e) {
				// 在wild看到这个，很奇怪
				Log.w(TAG, "剪贴板bug", e);
			}
		}
	}

	/**
	 * 剪切板是否有内容
	 * 
	 * @param context
	 * @return
	 */
	public static boolean hasText(Context context) {
		ClipboardManager clipboard = getManager(context);
		ClipData clip = clipboard.getPrimaryClip();
		return clip != null && clip.getItemCount() > 0;
	}

	/**
	 * 获得剪切板管理器
	 * 
	 * @param context
	 * @return
	 */
	private static ClipboardManager getManager(Context context) {
		return (ClipboardManager) context
				.getSystemService(Context.CLIPBOARD_SERVICE);
	}

}
