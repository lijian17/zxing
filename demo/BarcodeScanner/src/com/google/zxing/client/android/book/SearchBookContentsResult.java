package com.google.zxing.client.android.book;

/**
 * SBC结果的基础数据
 * 
 * @author lijian
 * @date 2017-9-3 上午11:21:30
 */
final class SearchBookContentsResult {

	/** 查询 */
	private static String query = null;

	/** 图书页ID */
	private final String pageId;
	/** 图书页编号 */
	private final String pageNumber;
	/** 摘录 */
	private final String snippet;
	/** 有效的摘录 */
	private final boolean validSnippet;

	/**
	 * SBC结果的基础数据
	 * 
	 * @param pageId
	 * @param pageNumber
	 * @param snippet
	 * @param validSnippet
	 */
	SearchBookContentsResult(String pageId, String pageNumber, String snippet,
			boolean validSnippet) {
		this.pageId = pageId;
		this.pageNumber = pageNumber;
		this.snippet = snippet;
		this.validSnippet = validSnippet;
	}

	public static void setQuery(String query) {
		SearchBookContentsResult.query = query;
	}

	public String getPageId() {
		return pageId;
	}

	public String getPageNumber() {
		return pageNumber;
	}

	public String getSnippet() {
		return snippet;
	}

	public boolean getValidSnippet() {
		return validSnippet;
	}

	public static String getQuery() {
		return query;
	}
}
