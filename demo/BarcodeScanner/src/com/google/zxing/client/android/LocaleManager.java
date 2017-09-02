package com.google.zxing.client.android;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 处理客户端的任何特定于语言环境的逻辑。
 * 
 * @author lijian
 * @date 2017-9-2 上午10:50:02
 */
public final class LocaleManager {

	/** 公司 */
	private static final String DEFAULT_TLD = "com";
	/** 国家 */
	private static final String DEFAULT_COUNTRY = "US";
	/** 语言 */
	private static final String DEFAULT_LANGUAGE = "en";

	/**
	 * Google网页搜索可用的区域设置（以及国家/地区）。 这些应与我们的翻译保持同步。
	 */
	private static final Map<String, String> GOOGLE_COUNTRY_TLD;
	static {
		GOOGLE_COUNTRY_TLD = new HashMap<String, String>();
		GOOGLE_COUNTRY_TLD.put("AR", "com.ar"); // 阿根廷
		GOOGLE_COUNTRY_TLD.put("AU", "com.au"); // 澳大利亚
		GOOGLE_COUNTRY_TLD.put("BR", "com.br"); // 巴西
		GOOGLE_COUNTRY_TLD.put("BG", "bg"); // 保加利亚
		GOOGLE_COUNTRY_TLD.put(Locale.CANADA.getCountry(), "ca");
		GOOGLE_COUNTRY_TLD.put(Locale.CHINA.getCountry(), "cn");
		GOOGLE_COUNTRY_TLD.put("CZ", "cz"); // 捷克共和国
		GOOGLE_COUNTRY_TLD.put("DK", "dk"); // 丹麦
		GOOGLE_COUNTRY_TLD.put("FI", "fi"); // 芬兰
		GOOGLE_COUNTRY_TLD.put(Locale.FRANCE.getCountry(), "fr");
		GOOGLE_COUNTRY_TLD.put(Locale.GERMANY.getCountry(), "de");
		GOOGLE_COUNTRY_TLD.put("GR", "gr"); // 希腊
		GOOGLE_COUNTRY_TLD.put("HU", "hu"); // 匈牙利
		GOOGLE_COUNTRY_TLD.put("ID", "co.id"); // 印度尼西亚
		GOOGLE_COUNTRY_TLD.put("IL", "co.il"); // 以色列
		GOOGLE_COUNTRY_TLD.put(Locale.ITALY.getCountry(), "it");
		GOOGLE_COUNTRY_TLD.put(Locale.JAPAN.getCountry(), "co.jp");
		GOOGLE_COUNTRY_TLD.put(Locale.KOREA.getCountry(), "co.kr");
		GOOGLE_COUNTRY_TLD.put("NL", "nl"); // 荷兰
		GOOGLE_COUNTRY_TLD.put("PL", "pl"); // 波兰
		GOOGLE_COUNTRY_TLD.put("PT", "pt"); // 葡萄牙
		GOOGLE_COUNTRY_TLD.put("RO", "ro"); // 罗马尼亚
		GOOGLE_COUNTRY_TLD.put("RU", "ru"); // 俄国
		GOOGLE_COUNTRY_TLD.put("SK", "sk"); // 斯洛伐克共和国
		GOOGLE_COUNTRY_TLD.put("SI", "si"); // 斯洛文尼亚
		GOOGLE_COUNTRY_TLD.put("ES", "es"); // 西班牙
		GOOGLE_COUNTRY_TLD.put("SE", "se"); // 瑞典
		GOOGLE_COUNTRY_TLD.put("CH", "ch"); // 瑞士
		GOOGLE_COUNTRY_TLD.put(Locale.TAIWAN.getCountry(), "tw");
		GOOGLE_COUNTRY_TLD.put("TR", "com.tr"); // 土耳其
		GOOGLE_COUNTRY_TLD.put("UA", "com.ua"); // 乌克兰
		GOOGLE_COUNTRY_TLD.put(Locale.UK.getCountry(), "co.uk");
		GOOGLE_COUNTRY_TLD.put(Locale.US.getCountry(), "com");
	}

	/**
	 * 谷歌搜索移动产品的可能性比网络搜索的国家少。 See here:
	 * http://support.google.com/merchants/bin/answer.py?hl=en-GB&answer=160619
	 */
	private static final Map<String, String> GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD;
	static {
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD = new HashMap<String, String>();
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("AU", "com.au"); // 澳大利亚
		// GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.CHINA.getCountry(),
		// "cn");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.FRANCE.getCountry(), "fr");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD
				.put(Locale.GERMANY.getCountry(), "de");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.ITALY.getCountry(), "it");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.JAPAN.getCountry(),
				"co.jp");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("NL", "nl"); // 荷兰
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("ES", "es"); // 西班牙
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("CH", "ch"); // 瑞士
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.UK.getCountry(), "co.uk");
		GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.US.getCountry(), "com");
	}

	/**
	 * 书籍搜索提供无处不在的网页搜索。
	 */
	private static final Map<String, String> GOOGLE_BOOK_SEARCH_COUNTRY_TLD = GOOGLE_COUNTRY_TLD;

	/** 翻译帮助资料语言 */
	private static final Collection<String> TRANSLATED_HELP_ASSET_LANGUAGES = Arrays
			.asList("de", "en", "es", "fr", "it", "ja", "ko", "nl", "pt", "ru",
					"uk", "zh-rCN", "zh-rTW", "zh-rHK");

	private LocaleManager() {
	}

	/**
	 * @param context
	 *            application's {@link Context}
	 * @return 针对当前默认语言环境的国家/地区特定TLD后缀 (e.g. "co.uk"为英国)
	 */
	public static String getCountryTLD(Context context) {
		return doGetTLD(GOOGLE_COUNTRY_TLD, context);
	}

	/**
	 * 与上述相同，但专门针对Google产品搜索
	 * 
	 * @param context
	 *            应用程序的上下文{@link Context}
	 * @return 要使用的顶级域名。
	 */
	public static String getProductSearchCountryTLD(Context context) {
		return doGetTLD(GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD, context);
	}

	/**
	 * 与上述相同，但专门用于Google图书搜索
	 * 
	 * @param context
	 *            应用程序的上下文{@link Context}
	 * @return 要使用的顶级域名。
	 */
	public static String getBookSearchCountryTLD(Context context) {
		return doGetTLD(GOOGLE_BOOK_SEARCH_COUNTRY_TLD, context);
	}

	/**
	 * 给定的网址指向Google图书搜索，不管域名
	 * 
	 * @param url
	 *            要检查的地址
	 * @return 如果这是图书搜索网址，则为true
	 */
	public static boolean isBookSearchUrl(String url) {
		return url.startsWith("http://google.com/books")
				|| url.startsWith("http://books.google.");
	}

	/**
	 * 获取系统国家
	 * 
	 * @return
	 */
	private static String getSystemCountry() {
		Locale locale = Locale.getDefault();
		return locale == null ? DEFAULT_COUNTRY : locale.getCountry();
	}

	/**
	 * 获取系统语言
	 * 
	 * @return
	 */
	private static String getSystemLanguage() {
		Locale locale = Locale.getDefault();
		if (locale == null) {
			return DEFAULT_LANGUAGE;
		}
		String language = locale.getLanguage();
		// 特殊情况中文
		if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(language)) {
			return language + "-r" + getSystemCountry();
		}
		return language;
	}

	/**
	 * 获得翻译资产语言
	 * 
	 * @return
	 */
	static String getTranslatedAssetLanguage() {
		String language = getSystemLanguage();
		return TRANSLATED_HELP_ASSET_LANGUAGES.contains(language) ? language
				: DEFAULT_LANGUAGE;
	}

	/**
	 * 执行获取TLD(国家域名后缀)
	 * 
	 * @param map
	 * @param context
	 * @return
	 */
	private static String doGetTLD(Map<String, String> map, Context context) {
		String tld = map.get(getCountry(context));
		return tld == null ? DEFAULT_TLD : tld;
	}

	/**
	 * 获得国家(从本地sp中获取)
	 * 
	 * @param context
	 * @return
	 */
	private static String getCountry(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String countryOverride = prefs.getString(
				PreferencesActivity.KEY_SEARCH_COUNTRY, "-");
		if (countryOverride != null && !countryOverride.isEmpty()
				&& !"-".equals(countryOverride)) {
			return countryOverride;
		}
		return getSystemCountry();
	}

}
