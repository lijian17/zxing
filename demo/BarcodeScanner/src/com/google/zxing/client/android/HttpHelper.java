package com.google.zxing.client.android;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import android.util.Log;

/**
 * 使用Android中更多支持的{@code java.net}类来检索HTTP内容的实用方法。
 * 
 * @author lijian
 * @date 2017-9-3 上午11:29:10
 */
public final class HttpHelper {
	private static final String TAG = HttpHelper.class.getSimpleName();

	/**
	 * 重定向域
	 */
	private static final Collection<String> REDIRECTOR_DOMAINS = new HashSet<String>(
			Arrays.asList("amzn.to", "bit.ly", "bitly.com", "fb.me", "goo.gl",
					"is.gd", "j.mp", "lnkd.in", "ow.ly", "R.BEETAGG.COM",
					"r.beetagg.com", "SCN.BY", "su.pr", "t.co", "tinyurl.com",
					"tr.im"));

	private HttpHelper() {
	}

	/**
	 * 枚举支持的HTTP内容类型
	 */
	public enum ContentType {
		/** 类似HTML的内容类型，包括HTML，XHTML等 */
		HTML,
		/** JSON内容 */
		JSON,
		/** XML内容 */
		XML,
		/** 纯文本内容 */
		TEXT,
	}

	/**
	 * 下载整个资源而不是部分资源
	 * 
	 * @param uri
	 *            要检索的URI
	 * @param type
	 *            预期的文字类型的MIME类型的内容
	 * @return 内容为{@code String}
	 * @throws IOException
	 *             如果由于URI不良，网络问题等原因无法检索到内容。
	 * @see #downloadViaHttp(String, HttpHelper.ContentType, int)
	 */
	public static CharSequence downloadViaHttp(String uri, ContentType type)
			throws IOException {
		return downloadViaHttp(uri, type, Integer.MAX_VALUE);
	}

	/**
	 * 下载整个资源而不是部分资
	 * 
	 * @param uri
	 *            要检索的URI
	 * @param type
	 *            预期的文字类型的MIME类型的内容
	 * @param maxChars
	 *            从源头读取的大致最大字符数
	 * @return 内容为{@code String}
	 * @throws IOException
	 *             如果由于URI不良，网络问题等原因无法检索到内容。
	 */
	public static CharSequence downloadViaHttp(String uri, ContentType type,
			int maxChars) throws IOException {
		String contentTypes;
		switch (type) {
		case HTML:
			contentTypes = "application/xhtml+xml,text/html,text/*,*/*";
			break;
		case JSON:
			contentTypes = "application/json,text/*,*/*";
			break;
		case XML:
			contentTypes = "application/xml,text/*,*/*";
			break;
		case TEXT:
		default:
			contentTypes = "text/*,*/*";
		}
		return downloadViaHttp(uri, contentTypes, maxChars);
	}

	/**
	 * 下载整个资源而不是部分资
	 * 
	 * @param uri
	 * @param contentTypes
	 * @param maxChars
	 * @return
	 * @throws IOException
	 */
	private static CharSequence downloadViaHttp(String uri,
			String contentTypes, int maxChars) throws IOException {
		int redirects = 0;
		while (redirects < 5) {
			URL url = new URL(uri);
			HttpURLConnection connection = safelyOpenConnection(url);
			connection.setInstanceFollowRedirects(true); // 不会使用HTTP ->
															// HTTPS，反之亦然
			connection.setRequestProperty("Accept", contentTypes);
			connection.setRequestProperty("Accept-Charset", "utf-8,*");
			connection.setRequestProperty("User-Agent", "ZXing (Android)");
			try {
				int responseCode = safelyConnect(connection);
				switch (responseCode) {
				case HttpURLConnection.HTTP_OK:
					return consume(connection, maxChars);
				case HttpURLConnection.HTTP_MOVED_TEMP:
					// 获取重定向地址
					String location = connection.getHeaderField("Location");
					if (location != null) {
						uri = location;
						redirects++;
						continue;
					}
					throw new IOException("没有重定向");
				default:
					throw new IOException("HTTP响应不良: " + responseCode);
				}
			} finally {
				connection.disconnect();
			}
		}
		throw new IOException("重定向太多");
	}

	/**
	 * 获取编码格式
	 * 
	 * @param connection
	 * @return
	 */
	private static String getEncoding(URLConnection connection) {
		String contentTypeHeader = connection.getHeaderField("Content-Type");
		if (contentTypeHeader != null) {
			int charsetStart = contentTypeHeader.indexOf("charset=");
			if (charsetStart >= 0) {
				return contentTypeHeader.substring(charsetStart
						+ "charset=".length());
			}
		}
		return "UTF-8";
	}

	/**
	 * 获取HTTP的请求结果
	 * 
	 * @param connection
	 * @param maxChars
	 *            最大HTTP请求结果的字符长度(避免StringBuilder内存溢出)
	 * @return
	 * @throws IOException
	 */
	private static CharSequence consume(URLConnection connection, int maxChars)
			throws IOException {
		String encoding = getEncoding(connection);
		StringBuilder out = new StringBuilder();
		Reader in = null;
		try {
			in = new InputStreamReader(connection.getInputStream(), encoding);
			char[] buffer = new char[1024];
			int charsRead;
			while (out.length() < maxChars && (charsRead = in.read(buffer)) > 0) {
				out.append(buffer, 0, charsRead);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// continue
				} catch (NullPointerException ioe) {
					// continue
				}
			}
		}
		return out;
	}

	/**
	 * 不能重定向
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public static URI unredirect(URI uri) throws IOException {
		if (!REDIRECTOR_DOMAINS.contains(uri.getHost())) {
			return uri;
		}
		URL url = uri.toURL();
		HttpURLConnection connection = safelyOpenConnection(url);
		connection.setInstanceFollowRedirects(false);
		connection.setDoInput(false);
		connection.setRequestMethod("HEAD");
		connection.setRequestProperty("User-Agent", "ZXing (Android)");
		try {
			int responseCode = safelyConnect(connection);
			switch (responseCode) {
			case HttpURLConnection.HTTP_MULT_CHOICE:
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_SEE_OTHER:
			case 307: // 307临时重定向无常数 ?
				String location = connection.getHeaderField("Location");
				if (location != null) {
					try {
						return new URI(location);
					} catch (URISyntaxException e) {
						// 没关系
					}
				}
			}
			return uri;
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * 安全打开连接
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private static HttpURLConnection safelyOpenConnection(URL url)
			throws IOException {
		URLConnection conn;
		try {
			conn = url.openConnection();
		} catch (NullPointerException npe) {
			// Android中还有一个奇怪的bug？
			Log.w(TAG, "坏URI? " + url);
			throw new IOException(npe);
		}
		if (!(conn instanceof HttpURLConnection)) {
			throw new IOException();
		}
		return (HttpURLConnection) conn;
	}

	/**
	 * 安全连接
	 * 
	 * @param connection
	 * @return
	 * @throws IOException
	 */
	private static int safelyConnect(HttpURLConnection connection)
			throws IOException {
		try {
			connection.connect();
		} catch (NullPointerException e) {
			// 这是一个Android bug:
			// http://code.google.com/p/android/issues/detail?id=16895
			throw new IOException(e);
		} catch (IllegalArgumentException e) {
			// 这是一个Android bug:
			// http://code.google.com/p/android/issues/detail?id=16895
			throw new IOException(e);
		} catch (IndexOutOfBoundsException e) {
			// 这是一个Android bug:
			// http://code.google.com/p/android/issues/detail?id=16895
			throw new IOException(e);
		} catch (SecurityException e) {
			// 这是一个Android bug:
			// http://code.google.com/p/android/issues/detail?id=16895
			throw new IOException(e);
		}
		try {
			return connection.getResponseCode();
		} catch (NullPointerException e) {
			// 这可能是这个Android bug:
			// http://code.google.com/p/android/issues/detail?id=15554
			throw new IOException(e);
		} catch (StringIndexOutOfBoundsException e) {
			// 这可能是这个Android bug:
			// http://code.google.com/p/android/issues/detail?id=15554
			throw new IOException(e);
		} catch (IllegalArgumentException e) {
			// 这可能是这个Android bug:
			// http://code.google.com/p/android/issues/detail?id=15554
			throw new IOException(e);
		}
	}

}
