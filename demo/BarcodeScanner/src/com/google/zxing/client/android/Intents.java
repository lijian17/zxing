package com.google.zxing.client.android;

/**
 * 该类提供了在将条目扫描程序发送到Intent时使用的常量。 这些字符串是有效的API，无法更改。
 * 
 * @author lijian
 * @date 2017-8-27 下午9:36:35
 */
public final class Intents {
	private Intents() {
	}

	/**
	 * 与 {@link Scan#ACTION} Intent相关的常量.
	 */
	public static final class Scan {
		/**
		 * 发送此意图以扫描模式打开条形码应用程序，查找条形码并返回结果。
		 */
		public static final String ACTION = "com.google.zxing.client.android.SCAN";

		/**
		 * 默认情况下，发送这将解码我们理解的所有条形码。 但是，将扫描限制为某些格式可能是有用的。 使用以下值之一的
		 * {@link android.content.Intent#putExtra(String, String)}
		 * 
		 * 设置这是使用{@link #FORMATS}设置显式格式的有效简写。 它被这个设置所覆盖。
		 */
		public static final String MODE = "SCAN_MODE";

		/**
		 * 仅解码UPC和EAN条形码。 这是购买应用程序的正确选择，可以获得产品的价格，评论等。
		 */
		public static final String PRODUCT_MODE = "PRODUCT_MODE";

		/**
		 * 仅解码1D条形码.
		 */
		public static final String ONE_D_MODE = "ONE_D_MODE";

		/**
		 * 仅解码QR码。
		 */
		public static final String QR_CODE_MODE = "QR_CODE_MODE";

		/**
		 * 仅解码DataMatrix码.
		 */
		public static final String DATA_MATRIX_MODE = "DATA_MATRIX_MODE";

		/**
		 * 仅解码Aztec码.
		 */
		public static final String AZTEC_MODE = "AZTEC_MODE";

		/**
		 * 仅解码PDF417码.
		 */
		public static final String PDF417_MODE = "PDF417_MODE";

		/**
		 * 逗号分隔的扫描格式列表。 值必须与{@link com.google.zxing.BarcodeFormat}s的名称相匹配。 e.g.
		 * {@link com.google.zxing.BarcodeFormat#EAN_13}。
		 * 示例："EAN_13,EAN_8,QR_CODE"。 这将覆盖{@link #MODE}。
		 */
		public static final String FORMATS = "SCAN_FORMATS";

		/**
		 * 可选参数，用于指定从中识别条形码的相机的ID。 覆盖默认的相机，否则将被选中。 如果提供，应该是一个int。
		 */
		public static final String CAMERA_ID = "SCAN_CAMERA_ID";

		/**
		 * @see com.google.zxing.DecodeHintType#CHARACTER_SET
		 */
		public static final String CHARACTER_SET = "CHARACTER_SET";

		/**
		 * 指定扫描矩形的宽度和高度的可选参数，以像素为单位。 该应用程序将尝试尊重这些，但会将它们夹在预览框架的大小上。
		 * 您应该同时指定两者，也可以将该大小作为int传递。
		 */
		public static final String WIDTH = "SCAN_WIDTH";
		public static final String HEIGHT = "SCAN_HEIGHT";

		/**
		 * 所需的持续时间（毫秒），在成功扫描之后暂停，然后返回到呼叫意图。 指定为long，不是integer！ 例如：1000L，而不是1000。
		 */
		public static final String RESULT_DISPLAY_DURATION_MS = "RESULT_DISPLAY_DURATION_MS";

		/**
		 * 在意图扫描时提示在屏幕上显示。 指定为{@link String}
		 */
		public static final String PROMPT_MESSAGE = "PROMPT_MESSAGE";

		/**
		 * 如果找到条形码，条形码将{@link android.app.Activity#RESULT_OK} 返回给
		 * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
		 * ， 该应用程序通过
		 * {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}
		 * 可以使用{@link android.content.Intent#getStringExtra(String)}检索条形码内容。
		 * 如果用户按Back，结果代码将为{@link android.app.Activity#RESULT_CANCELED}
		 */
		public static final String RESULT = "SCAN_RESULT";

		/**
		 * 调用具有{@link #RESULT_FORMAT}的
		 * {@link android.content.Intent#getStringExtra(String)}来确定哪个条形码格式被找到。
		 * 有关可能的值，请参阅{@link com.google.zxing.BarcodeFormat}
		 */
		public static final String RESULT_FORMAT = "SCAN_RESULT_FORMAT";

		/**
		 * 调用具有{@link #RESULT_UPC_EAN_EXTENSION}的
		 * {@link android.content.Intent#getStringExtra(String)}
		 * 返回还找到的任何UPC扩展条形码的内容。 仅适用于{@link com.google.zxing.BarcodeFormat#UPC_A}
		 * 和{@link com.google.zxing.BarcodeFormat#EAN_13}格式。
		 */
		public static final String RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION";

		/**
		 * 调用具有{@link #RESULT_BYTES}的
		 * {@link android.content.Intent#getByteArrayExtra(String)}获取条形码中的原始字节的
		 * {@code byte[]}（如果可用）。
		 */
		public static final String RESULT_BYTES = "SCAN_RESULT_BYTES";

		/**
		 * {@link com.google.zxing.ResultMetadataType#ORIENTATION}值的关键字（如果有）。 使用
		 * {@link #RESULT_ORIENTATION}调用
		 * {@link android.content.Intent#getIntArrayExtra(String)}
		 */
		public static final String RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION";

		/**
		 * 键入{@link com.google.zxing.ResultMetadataType#ERROR_CORRECTION_LEVEL}
		 * 的值（如果可用）。 使用{@link #RESULT_ERROR_CORRECTION_LEVEL}调用
		 * {@link android.content.Intent#getStringExtra(String)}
		 */
		public static final String RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL";

		/**
		 * 映射到{@link com.google.zxing.ResultMetadataType#BYTE_SEGMENTS}
		 * 值的键的前缀（如果可用）。 实际值将被设置在通过向该前缀中添加0,1,2，...形成的一系列键。
		 * 因此，第一个字节段位于keys"SCAN_RESULT_BYTE_SEGMENTS_0"下。 用这些keys调用
		 * {@link android.content.Intent#getByteArrayExtra(String)}。
		 */
		public static final String RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_";

		/**
		 * 将此设置为false将不会将扫描的代码保存在历史记录中. Specified as a {@code boolean}.
		 */
		public static final String SAVE_HISTORY = "SAVE_HISTORY";

		private Scan() {
		}
	}

	/**
	 * 与扫描历史相关的常数和检索历史记录项
	 */
	public static final class History {

		/** 项目编号 */
		public static final String ITEM_NUMBER = "ITEM_NUMBER";

		private History() {
		}
	}

	/**
	 * 与{@link Encode#ACTION}意图相关的常量。
	 */
	public static final class Encode {
		/**
		 * 发送此意图将一条数据编码为QR码并将其全屏显示，以便另一个人可以从屏幕扫描条形码
		 */
		public static final String ACTION = "com.google.zxing.client.android.ENCODE";

		/**
		 * 要编码的数据。 使用{@link android.content.Intent#putExtra(String, String)}或
		 * {@link android.content.Intent#putExtra(String, android.os.Bundle)}，
		 * 具体取决于指定的类型和格式。 非QR码格式应该在这里使用一个String。 有关QR码，详见内容。
		 */
		public static final String DATA = "ENCODE_DATA";

		/**
		 * 如果格式为QR码，则提供的数据类型。 使用一个{@link Contents.Type}使用
		 * {@link android.content.Intent#putExtra(String, String)}
		 */
		public static final String TYPE = "ENCODE_TYPE";

		/**
		 * 要显示的条形码格式。 如果未指定或为空，则默认为QR码。 使用
		 * {@link android.content.Intent#putExtra(String, String)}， 其中format是
		 * {@link com.google.zxing.BarcodeFormat}之一。
		 */
		public static final String FORMAT = "ENCODE_FORMAT";

		/**
		 * 通常，条形码的内容将在TextView中显示给用户。 将此布尔值设置为false将隐藏该TextView，仅显示编码条形码。
		 */
		public static final String SHOW_CONTENTS = "ENCODE_SHOW_CONTENTS";

		private Encode() {
		}
	}

	/**
	 * 与{@link SearchBookContents#ACTION}意图相关的常量。
	 */
	public static final class SearchBookContents {
		/**
		 * 使用Google图书搜索来搜索提供的图书的内容。
		 */
		public static final String ACTION = "com.google.zxing.client.android.SEARCH_BOOK_CONTENTS";

		/**
		 * 本书搜索，由ISBN号码标识。
		 */
		public static final String ISBN = "ISBN";

		/**
		 * 可选字段是要搜索的文本。
		 */
		public static final String QUERY = "QUERY";

		private SearchBookContents() {
		}
	}

	/**
	 * 与{@link WifiConnect#ACTION}意图相关的常量。
	 */
	public static final class WifiConnect {
		/**
		 * 用于触发连接到WiFi网络的内部意图
		 */
		public static final String ACTION = "com.google.zxing.client.android.WIFI_CONNECT";

		/**
		 * 网络连接，这里提供的所有配置
		 */
		public static final String SSID = "SSID";

		/**
		 * 网络连接，这里提供的所有配置
		 */
		public static final String TYPE = "TYPE";

		/**
		 * 网络连接，这里提供的所有配置
		 */
		public static final String PASSWORD = "PASSWORD";

		private WifiConnect() {
		}
	}

	/**
	 * 与{@link Share#ACTION}意图相关的常量。
	 */
	public static final class Share {
		/**
		 * 给用户选择要编码为条形码的项目，然后将其作为QR码显示，并显示在屏幕上，供朋友用手机扫描
		 */
		public static final String ACTION = "com.google.zxing.client.android.SHARE";

		private Share() {
		}
	}
}
