package com.google.zxing.client.android;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.DecodeHintType;

/**
 * 解码Hint管理器
 * 
 * @author lijian
 * @date 2017-8-27 下午10:24:26
 */
final class DecodeHintManager {
	private static final String TAG = DecodeHintManager.class.getSimpleName();

	// 该模式用于解码整数数组。
	private static final Pattern COMMA = Pattern.compile(",");

	private DecodeHintManager() {
	}

	/**
	 * <p>
	 * 将查询字符串拆分为键值对列表.
	 * </p>
	 * 
	 * <p>
	 * 这是{@link Uri#getQueryParameterNames()}和
	 * {@link Uri#getQueryParameters(String)}的替代方法，它们是古怪的，不适用于仅存在的Uri参数
	 * </p>
	 * 
	 * <p>
	 * 此方法忽略具有相同名称的多个参数，仅返回第一个参数。 这在技术上是不正确的，但被接受接受处理提示的方法：没有多个值提示。
	 * </p>
	 * 
	 * @param query
	 *            查询分割
	 * @return 键值对
	 */
	private static Map<String, String> splitQuery(String query) {
		Map<String, String> map = new HashMap<String, String>();
		int pos = 0;
		while (pos < query.length()) {
			if (query.charAt(pos) == '&') {
				// 跳过连续的&分隔符。
				pos++;
				continue;
			}
			int amp = query.indexOf('&', pos);
			int equ = query.indexOf('=', pos);
			if (amp < 0) {
				// 这是查询中的最后一个元素，不再有&符号元素。
				String name;
				String text;
				if (equ < 0) {
					// 没有=号
					name = query.substring(pos);
					name = name.replace('+', ' '); // 抢先解码+
					name = Uri.decode(name);
					text = "";
				} else {
					// 拆分名称和文字
					name = query.substring(pos, equ);
					name = name.replace('+', ' '); // 抢先解码+
					name = Uri.decode(name);
					text = query.substring(equ + 1);
					text = text.replace('+', ' '); // 抢先解码+
					text = Uri.decode(text);
				}
				if (!map.containsKey(name)) {
					map.put(name, text);
				}
				break;
			}
			if (equ < 0 || equ > amp) {
				// 没有=，直到&：这是一个没有价值的简单参数。
				String name = query.substring(pos, amp);
				name = name.replace('+', ' '); // 抢先解码+
				name = Uri.decode(name);
				if (!map.containsKey(name)) {
					map.put(name, "");
				}
				pos = amp + 1;
				continue;
			}
			String name = query.substring(pos, equ);
			name = name.replace('+', ' '); // 抢先解码+
			name = Uri.decode(name);
			String text = query.substring(equ + 1, amp);
			text = text.replace('+', ' '); // 抢先解码+
			text = Uri.decode(text);
			if (!map.containsKey(name)) {
				map.put(name, text);
			}
			pos = amp + 1;
		}
		return map;
	}

	/**
	 * 解析解码Hints
	 * 
	 * @param inputUri
	 * @return
	 */
	static Map<DecodeHintType, ?> parseDecodeHints(Uri inputUri) {
		String query = inputUri.getEncodedQuery();
		if (query == null || query.isEmpty()) {
			return null;
		}

		// 提取参数
		Map<String, String> parameters = splitQuery(query);

		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(
				DecodeHintType.class);

		for (DecodeHintType hintType : DecodeHintType.values()) {

			if (hintType == DecodeHintType.CHARACTER_SET
					|| hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK
					|| hintType == DecodeHintType.POSSIBLE_FORMATS) {
				continue; // 这个hint是以另一种方式指定的
			}

			String parameterName = hintType.name();
			String parameterText = parameters.get(parameterName);
			if (parameterText == null) {
				continue;
			}
			if (hintType.getValueType().equals(Object.class)) {
				// 这是一个未指定类型的hint内容。 按照原样使用该值。
				// TODO: 我们可以对此做出不同的假设吗？
				hints.put(hintType, parameterText);
				continue;
			}
			if (hintType.getValueType().equals(Void.class)) {
				// Void hints只是标志：使用DecodeHintType指定的常量
				hints.put(hintType, Boolean.TRUE);
				continue;
			}
			if (hintType.getValueType().equals(String.class)) {
				// 一个string hint:使用解码的值
				hints.put(hintType, parameterText);
				continue;
			}
			if (hintType.getValueType().equals(Boolean.class)) {
				// 一个boolean hint: 少数一些是false，其他大部分是true
				// 空参数只是一个标志样式的参数，假设为true
				if (parameterText.isEmpty()) {
					hints.put(hintType, Boolean.TRUE);
				} else if ("0".equals(parameterText)
						|| "false".equalsIgnoreCase(parameterText)
						|| "no".equalsIgnoreCase(parameterText)) {
					hints.put(hintType, Boolean.FALSE);
				} else {
					hints.put(hintType, Boolean.TRUE);
				}

				continue;
			}
			if (hintType.getValueType().equals(int[].class)) {
				// 整数数组。 用于指定有效长度。
				// 在Java风格的数组初始化器中分隔一个逗号。
				if (!parameterText.isEmpty()
						&& parameterText.charAt(parameterText.length() - 1) == ',') {
					parameterText = parameterText.substring(0,
							parameterText.length() - 1);
				}
				String[] values = COMMA.split(parameterText);
				int[] array = new int[values.length];
				for (int i = 0; i < values.length; i++) {
					try {
						array[i] = Integer.parseInt(values[i]);
					} catch (NumberFormatException ignored) {
						Log.w(TAG, "跳过integers类型的数组hint " + hintType
								+ " 由于无效的数值: '" + values[i] + '\'');
						array = null;
						break;
					}
				}
				if (array != null) {
					hints.put(hintType, array);
				}
				continue;
			}
			Log.w(TAG,
					"不支持的 hint type '" + hintType + "' of type "
							+ hintType.getValueType());
		}

		Log.i(TAG, "来自URI的Hints: " + hints);
		return hints;
	}

	/**
	 * 解析解码Hints
	 * 
	 * @param intent
	 * @return
	 */
	static Map<DecodeHintType, Object> parseDecodeHints(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras == null || extras.isEmpty()) {
			return null;
		}
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(
				DecodeHintType.class);

		for (DecodeHintType hintType : DecodeHintType.values()) {

			if (hintType == DecodeHintType.CHARACTER_SET
					|| hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK
					|| hintType == DecodeHintType.POSSIBLE_FORMATS) {
				continue; // 这个hint是以另一种方式指定的
			}

			String hintName = hintType.name();
			if (extras.containsKey(hintName)) {
				if (hintType.getValueType().equals(Void.class)) {
					// Void hints只是标志：使用DecodeHintType指定的常量
					hints.put(hintType, Boolean.TRUE);
				} else {
					Object hintData = extras.get(hintName);
					if (hintType.getValueType().isInstance(hintData)) {
						hints.put(hintType, hintData);
					} else {
						Log.w(TAG, "忽略 hint " + hintType + " 因为它不是从分配 "
								+ hintData);
					}
				}
			}
		}

		Log.i(TAG, "来自Intent的Hints: " + hints);
		return hints;
	}

}
