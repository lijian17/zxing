package com.google.zxing.client.android;

import android.content.Intent;
import android.net.Uri;
import com.google.zxing.BarcodeFormat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 解码格式管理器
 * 
 * @author lijian
 * @date 2017-8-27 下午6:39:10
 */
final class DecodeFormatManager {
	/** 逗号模式 */
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");

	/** 产品格式 */
	static final Set<BarcodeFormat> PRODUCT_FORMATS;
	/** 工业格式 */
	static final Set<BarcodeFormat> INDUSTRIAL_FORMATS;
	/** 1D格式 */
	private static final Set<BarcodeFormat> ONE_D_FORMATS;
	/** QRcode格式 */
	static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet
			.of(BarcodeFormat.QR_CODE);
	/** dataMatrix格式 */
	static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet
			.of(BarcodeFormat.DATA_MATRIX);
	/** Aztec格式 */
	static final Set<BarcodeFormat> AZTEC_FORMATS = EnumSet
			.of(BarcodeFormat.AZTEC);
	/** PDF417格式 */
	static final Set<BarcodeFormat> PDF417_FORMATS = EnumSet
			.of(BarcodeFormat.PDF_417);
	static {
		// EnumSet 是一个与枚举类型一起使用的专用 Set实现。
		// 枚举set中所有元素都必须来自单个枚举类型（即必须是同类型，且该类型是Enum的子类）。
		// 枚举类型在创建 set 时显式或隐式地指定。枚举 set 在内部表示为位向量。 此表示形式非常紧凑且高效。此类的空间和时间性能应该很好，
		// 足以用作传统上基于 int 的“位标志”的替换形式，具有高品质、类型安全的优势。

		// EnumSet.of(E e1, E e2, E e3, E e4, E e5)创建一个最初包含指定元素的枚举 set。 用 1 到 5
		// 个元素重载此方法，从而初始化一个枚举 set。第 6
		// 次重载使用变量参数功能。此重载可能创建一个最初包含任意个元素的枚举 set，但是这样很可能比不使用变量参数的重载运行得慢。
		PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
				BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
				BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED);
		INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
				BarcodeFormat.CODE_93, BarcodeFormat.CODE_128,
				BarcodeFormat.ITF, BarcodeFormat.CODABAR);

		// EnumSet.copyOf(Collection<E> c)创建一个从指定 collection 初始化的枚举 set。如果指定的
		// collection 是一个 EnumSet
		// 实例，则此静态工厂方法的功能与 copyOf(EnumSet) 相同。否则，指定的 collection
		// 必须至少包含一个元素（以确定新枚举 set 的元素类型）。
		ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS);
		ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS);
	}

	/** 模式格式 */
	private static final Map<String, Set<BarcodeFormat>> FORMATS_FOR_MODE;
	static {
		FORMATS_FOR_MODE = new HashMap<String, Set<BarcodeFormat>>();
		FORMATS_FOR_MODE.put(Intents.Scan.ONE_D_MODE, ONE_D_FORMATS);
		FORMATS_FOR_MODE.put(Intents.Scan.PRODUCT_MODE, PRODUCT_FORMATS);
		FORMATS_FOR_MODE.put(Intents.Scan.QR_CODE_MODE, QR_CODE_FORMATS);
		FORMATS_FOR_MODE
				.put(Intents.Scan.DATA_MATRIX_MODE, DATA_MATRIX_FORMATS);
		FORMATS_FOR_MODE.put(Intents.Scan.AZTEC_MODE, AZTEC_FORMATS);
		FORMATS_FOR_MODE.put(Intents.Scan.PDF417_MODE, PDF417_FORMATS);
	}

	private DecodeFormatManager() {
	}

	/**
	 * 解析解码格式
	 * 
	 * @param intent
	 *            意图
	 * @return
	 */
	static Set<BarcodeFormat> parseDecodeFormats(Intent intent) {
		Iterable<String> scanFormats = null;
		CharSequence scanFormatsString = intent
				.getStringExtra(Intents.Scan.FORMATS);
		if (scanFormatsString != null) {
			scanFormats = Arrays.asList(COMMA_PATTERN.split(scanFormatsString));
		}
		return parseDecodeFormats(scanFormats,
				intent.getStringExtra(Intents.Scan.MODE));
	}

	/**
	 * 解析解码格式
	 * 
	 * @param inputUri
	 * @return
	 */
	static Set<BarcodeFormat> parseDecodeFormats(Uri inputUri) {
		// 项目有可能需要截取Url 链接中参数时，最好不要利用处理String的手段来做,可以方便地使用URI达到目的.
		// 步骤如下:
		// 1 将String类型的URL转变为URI
		// 2 利用URI的getQueryParameter方法获取参数
		//
		// 例如在一个URL中需要获取appid和userId
		// 过程如下:
		// Uri uri = Uri.parse(url);
		// String appid= uri.getQueryParameter("appid");
		// String userId= uri.getQueryParameter("userId");
		List<String> formats = inputUri
				.getQueryParameters(Intents.Scan.FORMATS);
		if (formats != null && formats.size() == 1 && formats.get(0) != null) {
			formats = Arrays.asList(COMMA_PATTERN.split(formats.get(0)));
		}
		return parseDecodeFormats(formats,
				inputUri.getQueryParameter(Intents.Scan.MODE));
	}

	/**
	 * 解析解码格式
	 * 
	 * @param scanFormats
	 * @param decodeMode
	 * @return
	 */
	private static Set<BarcodeFormat> parseDecodeFormats(
			Iterable<String> scanFormats, String decodeMode) {
		// 如果应用层有传递要解析的解码格式，这使用应用层的
		if (scanFormats != null) {
			Set<BarcodeFormat> formats = EnumSet.noneOf(BarcodeFormat.class);
			try {
				for (String format : scanFormats) {
					formats.add(BarcodeFormat.valueOf(format));
				}
				return formats;
			} catch (IllegalArgumentException iae) {
				// ignore it then
			}
		}
		// 否则使用默认的解码格式
		if (decodeMode != null) {
			return FORMATS_FOR_MODE.get(decodeMode);
		}
		return null;
	}

}
