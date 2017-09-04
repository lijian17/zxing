package com.google.zxing.client.android.encode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * 联系人编码器<br>
 * 实现根据编码联系人信息的一些方案进行编码，如VCard或MECARD。
 * 
 * @author lijian
 * @date 2017-9-4 下午9:29:11
 */
abstract class ContactEncoder {

	/**
	 * 
	 * @param names
	 * @param organization
	 * @param addresses
	 * @param phones
	 * @param phoneTypes
	 * @param emails
	 * @param urls
	 * @param note
	 * @return 首先，以适当的格式对所有数据进行最好的编码; 第二，联系信息的显示适当版本
	 */
	abstract String[] encode(List<String> names, String organization,
			List<String> addresses, List<String> phones,
			List<String> phoneTypes, List<String> emails, List<String> urls,
			String note);

	/**
	 * 去除空格
	 * 
	 * @param s
	 * @return s为null、empty则返回null,否则返回s.trim();
	 */
	static String trim(String s) {
		if (s == null) {
			return null;
		}
		String result = s.trim();
		return result.isEmpty() ? null : result;
	}

	/**
	 * 
	 * @param newContents
	 *            新内容
	 * @param newDisplayContents
	 *            新的显示内容
	 * @param prefix
	 *            前缀
	 * @param value
	 *            值
	 * @param fieldFormatter
	 *            字段格式器
	 * @param terminator
	 *            分界线
	 */
	static void append(StringBuilder newContents,
			StringBuilder newDisplayContents, String prefix, String value,
			Formatter fieldFormatter, char terminator) {
		String trimmed = trim(value);
		if (trimmed != null) {
			newContents.append(prefix)
					.append(fieldFormatter.format(trimmed, 0))
					.append(terminator);
			newDisplayContents.append(trimmed).append('\n');
		}
	}

	/**
	 * 
	 * @param newContents
	 * @param newDisplayContents
	 * @param prefix
	 * @param values
	 * @param max
	 * @param displayFormatter
	 * @param fieldFormatter
	 * @param terminator
	 */
	static void appendUpToUnique(StringBuilder newContents,
			StringBuilder newDisplayContents, String prefix,
			List<String> values, int max, Formatter displayFormatter,
			Formatter fieldFormatter, char terminator) {
		if (values == null) {
			return;
		}
		int count = 0;
		Collection<String> uniques = new HashSet<String>(2);
		for (int i = 0; i < values.size(); i++) {
			String value = values.get(i);
			String trimmed = trim(value);
			if (trimmed != null && !trimmed.isEmpty()
					&& !uniques.contains(trimmed)) {
				newContents.append(prefix)
						.append(fieldFormatter.format(trimmed, i))
						.append(terminator);
				CharSequence display = displayFormatter == null ? trimmed
						: displayFormatter.format(trimmed, i);
				newDisplayContents.append(display).append('\n');
				if (++count == max) {
					break;
				}
				uniques.add(trimmed);
			}
		}
	}

}
