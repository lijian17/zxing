package com.google.zxing.client.android;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.history.HistoryActivity;
import com.google.zxing.client.android.history.HistoryItem;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.android.result.ResultButtonListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.android.result.supplement.SupplementalInfoRetriever;
import com.google.zxing.client.android.share.ShareActivity;

/**
 * 这个activity打开相机，并在后台线程上进行实际扫描。 它绘制取景器以帮助用户正确放置条形码，在图像处理正在发生时显示反馈，然后在扫描成功时覆盖结果。
 * 
 * @author lijian
 * @date 2017-8-16 下午3:34:28
 */
public final class CaptureActivity extends Activity implements
		SurfaceHolder.Callback {
	private static final String TAG = CaptureActivity.class.getSimpleName();

	/** 默认意图结果持续时间 */
	private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
	/** bulk模扫描延迟时间 */
	private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

	private static final String[] ZXING_URLS = {
			"http://zxing.appspot.com/scan", "zxing://scan/" };

	private static final int HISTORY_REQUEST_CODE = 0x0000bacc;

	/** EnumSet.of创建指定类型指定初始数据的集合 */
	private static final Collection<ResultMetadataType> DISPLAYABLE_METADATA_TYPES = EnumSet
			.of(ResultMetadataType.ISSUE_NUMBER,
					ResultMetadataType.SUGGESTED_PRICE,
					ResultMetadataType.ERROR_CORRECTION_LEVEL,
					ResultMetadataType.POSSIBLE_COUNTRY);

	/** 相机管理器 */
	private CameraManager cameraManager;
	/** CaptureActivity的处理器 */
	private CaptureActivityHandler handler;
	/** 保存扫描结果并显示 */
	private Result savedResultToShow;
	/** 取景器视图 */
	private ViewfinderView viewfinderView;
	/** 状态view(请将条码置于取景框内) */
	private TextView statusView;
	/** 扫描结果view容器 */
	private View resultView;
	private Result lastResult;
	/** 标记是否有Surface */
	private boolean hasSurface;
	/** 标记是否拷贝到剪贴板 */
	private boolean copyToClipboard;
	/** 意图源 */
	private IntentSource source;
	/** 源链接 */
	private String sourceUrl;
	/** 从网页管理器扫描 */
	private ScanFromWebPageManager scanFromWebPageManager;
	/** 解码格式集 */
	private Collection<BarcodeFormat> decodeFormats;
	/** 解码Hint集 */
	private Map<DecodeHintType, ?> decodeHints;
	/** 字符集 */
	private String characterSet;
	/** 管理与扫描历史相关的功能 */
	private HistoryManager historyManager;
	/** 闲置计时器 */
	private InactivityTimer inactivityTimer;
	/** 管理声音和震动 */
	private BeepManager beepManager;
	/** 检测环境光 */
	private AmbientLightManager ambientLightManager;

	/**
	 * 获得取景器
	 * 
	 * @return
	 */
	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	/**
	 * 获得Handler，即CaptureActivityHandler
	 * 
	 * @return
	 */
	public Handler getHandler() {
		return handler;
	}

	CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// 设置保持屏幕常量
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.capture);

		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);
		ambientLightManager = new AmbientLightManager(this);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// 必须在此处初始化historyManager才能更新历史记录preference
		historyManager = new HistoryManager(this);
		historyManager.trimHistory();

		// CameraManager必须在这里初始化，而不是在onCreate()中。
		// 这是必要的，因为如果我们要在第一次启动时显示帮助，我们不想打开相机驱动程序并测量屏幕尺寸。 这导致扫描矩形的大小错误，部分关闭屏幕的错误。
		cameraManager = new CameraManager(getApplication());

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);

		resultView = findViewById(R.id.result_view);
		statusView = (TextView) findViewById(R.id.status_view);

		handler = null;
		lastResult = null;

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION,
				true)) {
			setRequestedOrientation(getCurrentOrientation());
		} else {
			// 传感器横屏模式
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}

		resetStatusView();

		beepManager.updatePrefs();
		ambientLightManager.start(cameraManager);

		inactivityTimer.onResume();

		Intent intent = getIntent();

		copyToClipboard = prefs.getBoolean(
				PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true)
				&& (intent == null || intent.getBooleanExtra(
						Intents.Scan.SAVE_HISTORY, true));

		source = IntentSource.NONE;
		sourceUrl = null;
		scanFromWebPageManager = null;
		decodeFormats = null;
		characterSet = null;

		if (intent != null) {

			String action = intent.getAction();
			String dataString = intent.getDataString();

			if (Intents.Scan.ACTION.equals(action)) {

				// 扫描意图请求的格式，并将结果返回到calling activity
				source = IntentSource.NATIVE_APP_INTENT;
				decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
				decodeHints = DecodeHintManager.parseDecodeHints(intent);

				if (intent.hasExtra(Intents.Scan.WIDTH)
						&& intent.hasExtra(Intents.Scan.HEIGHT)) {
					int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
					int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
					if (width > 0 && height > 0) {
						cameraManager.setManualFramingRect(width, height);
					}
				}

				if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
					int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID,
							-1);
					if (cameraId >= 0) {
						cameraManager.setManualCameraId(cameraId);
					}
				}

				String customPromptMessage = intent
						.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
				if (customPromptMessage != null) {
					statusView.setText(customPromptMessage);
				}

			} else if (dataString != null
					&& dataString.contains("http://www.google")
					&& dataString.contains("/m/products/scan")) {

				// 只扫描产品并将结果发送到手机产品搜索
				source = IntentSource.PRODUCT_SEARCH_LINK;
				sourceUrl = dataString;
				decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

			} else if (isZXingURL(dataString)) {

				// 查询字符串中请求扫描格式（如果没有指定，则为所有格式）
				// 如果指定了返回URL，请在那里发送结果。 否则，自己处理
				source = IntentSource.ZXING_LINK;
				sourceUrl = dataString;
				Uri inputUri = Uri.parse(dataString);
				scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
				decodeFormats = DecodeFormatManager
						.parseDecodeFormats(inputUri);
				// 允许呼叫者指定提示的子集
				decodeHints = DecodeHintManager.parseDecodeHints(inputUri);
			}

			characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

		}

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// 活动已暂停但未停止，因此表面仍然存在。 因此，surfaceCreated()不会被调用，所以在这里初始化摄像头。
			initCamera(surfaceHolder);
		} else {
			// 安装回调并等待surfaceCreated()初始化相机
			surfaceHolder.addCallback(this);
		}
	}

	/**
	 * 获得当前方向
	 * 
	 * @return
	 */
	private int getCurrentOrientation() {
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			switch (rotation) {
			case Surface.ROTATION_0:
			case Surface.ROTATION_90:
				return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			default:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			}
		} else {
			switch (rotation) {
			case Surface.ROTATION_0:
			case Surface.ROTATION_270:
				return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			default:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			}
		}
	}

	/**
	 * 判断是否为ZXing的URL
	 * 
	 * @param dataString
	 * @return
	 */
	private static boolean isZXingURL(String dataString) {
		if (dataString == null) {
			return false;
		}
		for (String url : ZXING_URLS) {
			if (dataString.startsWith(url)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		ambientLightManager.stop();
		beepManager.close();
		cameraManager.closeDriver();
		// historyManager = null; // 保持onActivityResult
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (source == IntentSource.NATIVE_APP_INTENT) {
				setResult(RESULT_CANCELED);
				finish();
				return true;
			}
			if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK)
					&& lastResult != null) {
				restartPreviewAfterDelay(0L);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_FOCUS:
		case KeyEvent.KEYCODE_CAMERA:
			// 处理这些事件，以免他们启动Camera应用程序
			return true;
			// 使用音量上/下打开闪光灯
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			cameraManager.setTorch(false);
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			cameraManager.setTorch(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.capture, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		switch (item.getItemId()) {
		case R.id.menu_share:// 分享
			intent.setClassName(this, ShareActivity.class.getName());
			startActivity(intent);
			break;
		case R.id.menu_history:// 查看历史
			intent.setClassName(this, HistoryActivity.class.getName());
			startActivityForResult(intent, HISTORY_REQUEST_CODE);
			break;
		case R.id.menu_settings:// 设置
			intent.setClassName(this, PreferencesActivity.class.getName());
			startActivity(intent);
			break;
		case R.id.menu_help:// 帮助
			intent.setClassName(this, HelpActivity.class.getName());
			startActivity(intent);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK && requestCode == HISTORY_REQUEST_CODE
				&& historyManager != null) {
			int itemNumber = intent
					.getIntExtra(Intents.History.ITEM_NUMBER, -1);
			if (itemNumber >= 0) {
				HistoryItem historyItem = historyManager
						.buildHistoryItem(itemNumber);
				decodeOrStoreSavedBitmap(null, historyItem.getResult());
			}
		}
	}

	/**
	 * 解码或存储保存的位图
	 * 
	 * @param bitmap
	 * @param result
	 */
	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		// 位图未使用 - 将很快使用
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG, "*** WARNING *** surfaceCreated() 给了我们一个null的surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * 已找到有效的条形码，因此可以显示成功并显示结果。
	 * 
	 * @param rawResult
	 *            条形码的内容。
	 * @param barcode
	 *            被解码的相机数据的灰度位图。
	 * @param scaleFactor
	 *            缩放缩略图的缩放比例
	 */
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		inactivityTimer.onActivity();
		lastResult = rawResult;
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				this, rawResult);

		boolean fromLiveScan = barcode != null;
		if (fromLiveScan) {
			historyManager.addHistoryItem(rawResult, resultHandler);
			// 那么不是从历史，所以beep/vibrate，我们有一个image draw
			beepManager.playBeepSoundAndVibrate();
			drawResultPoints(barcode, scaleFactor, rawResult);
		}

		switch (source) {
		case NATIVE_APP_INTENT:
		case PRODUCT_SEARCH_LINK:
			handleDecodeExternally(rawResult, resultHandler, barcode);
			break;
		case ZXING_LINK:
			if (scanFromWebPageManager == null
					|| !scanFromWebPageManager.isScanFromWebPage()) {
				handleDecodeInternally(rawResult, resultHandler, barcode);
			} else {
				handleDecodeExternally(rawResult, resultHandler, barcode);
			}
			break;
		case NONE:
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			if (fromLiveScan
					&& prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE,
							false)) {
				// 批量扫描模式：条码已扫描并保存
				Toast.makeText(
						getApplicationContext(),
						getResources()
								.getString(R.string.msg_bulk_mode_scanned)
								+ " (" + rawResult.getText() + ')',
						Toast.LENGTH_SHORT).show();
				// 等待片刻，否则会持续扫描相同的条形码3次
				restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
			} else {
				handleDecodeInternally(rawResult, resultHandler, barcode);
			}
			break;
		}
	}

	/**
	 * 叠加1D或2D的线以突出显示条形码的主要特征
	 * 
	 * @param barcode
	 *            捕获图像的位图
	 * @param scaleFactor
	 *            缩小缩放比例的数量
	 * @param rawResult
	 *            解码结果包含要绘制的点
	 */
	private void drawResultPoints(Bitmap barcode, float scaleFactor,
			Result rawResult) {
		ResultPoint[] points = rawResult.getResultPoints();
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_points));
			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
							.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
				// Hacky特殊情况 - 绘制两条线条，用于条形码和元数据
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
				drawLine(canvas, paint, points[2], points[3], scaleFactor);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					if (point != null) {
						canvas.drawPoint(scaleFactor * point.getX(),
								scaleFactor * point.getY(), paint);
					}
				}
			}
		}
	}

	/**
	 * 画线
	 * 
	 * @param canvas
	 * @param paint
	 * @param a
	 * @param b
	 * @param scaleFactor
	 */
	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b, float scaleFactor) {
		if (a != null && b != null) {
			canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(),
					scaleFactor * b.getX(), scaleFactor * b.getY(), paint);
		}
	}

	/**
	 * 放置我们自己的UI来处理解码的内容
	 * 
	 * @param rawResult
	 * @param resultHandler
	 * @param barcode
	 */
	private void handleDecodeInternally(Result rawResult,
			ResultHandler resultHandler, Bitmap barcode) {

		CharSequence displayContents = resultHandler.getDisplayContents();

		if (copyToClipboard && !resultHandler.areContentsSecure()) {
			ClipboardInterface.setText(displayContents, this);
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (resultHandler.getDefaultButtonID() != null
				&& prefs.getBoolean(PreferencesActivity.KEY_AUTO_OPEN_WEB,
						false)) {
			resultHandler.handleButtonPress(resultHandler.getDefaultButtonID());
			return;
		}

		statusView.setVisibility(View.GONE);
		viewfinderView.setVisibility(View.GONE);
		resultView.setVisibility(View.VISIBLE);

		ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
		if (barcode == null) {
			barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(
					getResources(), R.drawable.launcher_icon));
		} else {
			barcodeImageView.setImageBitmap(barcode);
		}

		TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
		formatTextView.setText(rawResult.getBarcodeFormat().toString());

		TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
		typeTextView.setText(resultHandler.getType().toString());

		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT);
		TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
		timeTextView
				.setText(formatter.format(new Date(rawResult.getTimestamp())));

		TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
		View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
		metaTextView.setVisibility(View.GONE);
		metaTextViewLabel.setVisibility(View.GONE);
		Map<ResultMetadataType, Object> metadata = rawResult
				.getResultMetadata();
		if (metadata != null) {
			StringBuilder metadataText = new StringBuilder(20);
			for (Map.Entry<ResultMetadataType, Object> entry : metadata
					.entrySet()) {
				if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
					metadataText.append(entry.getValue()).append('\n');
				}
			}
			if (metadataText.length() > 0) {
				metadataText.setLength(metadataText.length() - 1);
				metaTextView.setText(metadataText);
				metaTextView.setVisibility(View.VISIBLE);
				metaTextViewLabel.setVisibility(View.VISIBLE);
			}
		}

		TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
		contentsTextView.setText(displayContents);
		int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
		contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

		TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
		supplementTextView.setText("");
		supplementTextView.setOnClickListener(null);
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				PreferencesActivity.KEY_SUPPLEMENTAL, true)) {
			SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView,
					resultHandler.getResult(), historyManager, this);
		}

		int buttonCount = resultHandler.getButtonCount();
		ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
		buttonView.requestFocus();
		for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
			TextView button = (TextView) buttonView.getChildAt(x);
			if (x < buttonCount) {
				button.setVisibility(View.VISIBLE);
				button.setText(resultHandler.getButtonText(x));
				button.setOnClickListener(new ResultButtonListener(
						resultHandler, x));
			} else {
				button.setVisibility(View.GONE);
			}
		}

	}

	/**
	 * 简要显示条形码的内容，然后处理条码扫描器外的结果
	 * 
	 * @param rawResult
	 * @param resultHandler
	 * @param barcode
	 */
	private void handleDecodeExternally(Result rawResult,
			ResultHandler resultHandler, Bitmap barcode) {

		if (barcode != null) {
			viewfinderView.drawResultBitmap(barcode);
		}

		long resultDurationMS;
		if (getIntent() == null) {
			resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
		} else {
			resultDurationMS = getIntent().getLongExtra(
					Intents.Scan.RESULT_DISPLAY_DURATION_MS,
					DEFAULT_INTENT_RESULT_DURATION_MS);
		}

		if (resultDurationMS > 0) {
			String rawResultString = String.valueOf(rawResult);
			if (rawResultString.length() > 32) {
				rawResultString = rawResultString.substring(0, 32) + " ...";
			}
			statusView.setText(getString(resultHandler.getDisplayTitle())
					+ " : " + rawResultString);
		}

		if (copyToClipboard && !resultHandler.areContentsSecure()) {
			CharSequence text = resultHandler.getDisplayContents();
			ClipboardInterface.setText(text, this);
		}

		if (source == IntentSource.NATIVE_APP_INTENT) {

			// 回传他们所要求的任何动作 - 当不推荐的意图退休时，可以将其更改为Intents.Scan.ACTION
			Intent intent = new Intent(getIntent().getAction());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
			intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult
					.getBarcodeFormat().toString());
			byte[] rawBytes = rawResult.getRawBytes();
			if (rawBytes != null && rawBytes.length > 0) {
				intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
			}
			Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
			if (metadata != null) {
				if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
					intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
							metadata.get(ResultMetadataType.UPC_EAN_EXTENSION)
									.toString());
				}
				Number orientation = (Number) metadata
						.get(ResultMetadataType.ORIENTATION);
				if (orientation != null) {
					intent.putExtra(Intents.Scan.RESULT_ORIENTATION,
							orientation.intValue());
				}
				String ecLevel = (String) metadata
						.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
				if (ecLevel != null) {
					intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL,
							ecLevel);
				}
				@SuppressWarnings("unchecked")
				Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata
						.get(ResultMetadataType.BYTE_SEGMENTS);
				if (byteSegments != null) {
					int i = 0;
					for (byte[] byteSegment : byteSegments) {
						intent.putExtra(
								Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i,
								byteSegment);
						i++;
					}
				}
			}
			sendReplyMessage(R.id.return_scan_result, intent, resultDurationMS);

		} else if (source == IntentSource.PRODUCT_SEARCH_LINK) {

			// 重新形成触发我们进入查询的URL，以便请求与扫描URL一起进入相同的TLD
			int end = sourceUrl.lastIndexOf("/scan");
			String replyURL = sourceUrl.substring(0, end) + "?q="
					+ resultHandler.getDisplayContents() + "&source=zxing";
			sendReplyMessage(R.id.launch_product_query, replyURL,
					resultDurationMS);

		} else if (source == IntentSource.ZXING_LINK) {

			if (scanFromWebPageManager != null
					&& scanFromWebPageManager.isScanFromWebPage()) {
				String replyURL = scanFromWebPageManager.buildReplyURL(
						rawResult, resultHandler);
				scanFromWebPageManager = null;
				sendReplyMessage(R.id.launch_product_query, replyURL,
						resultDurationMS);
			}

		}
	}

	/**
	 * 发送回复消息
	 * 
	 * @param id
	 * @param arg
	 * @param delayMS
	 */
	private void sendReplyMessage(int id, Object arg, long delayMS) {
		if (handler != null) {
			Message message = Message.obtain(handler, id, arg);
			if (delayMS > 0L) {
				handler.sendMessageDelayed(message, delayMS);
			} else {
				handler.sendMessage(message);
			}
		}
	}

	/**
	 * 初始化相机
	 * 
	 * @param surfaceHolder
	 */
	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("没有提供SurfaceHolder");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG, "initCamera() 而已经打开 -- 迟到的SurfaceView回调?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// 创建处理程序启动预览，这也可以引发RuntimeException
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats,
						decodeHints, characterSet, cameraManager);
			}
			decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// 条形码扫描仪已经看到了这个品种的疯狂的崩溃:
			// java.?lang.?RuntimeException: 连接到摄像机服务失败
			Log.w(TAG, "初始化相机出现意外错误", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	/**
	 * 显示框架Bug消息并退出
	 */
	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));// 条码扫描器
		// 很遗憾，Android 相机出现问题。你可能需要重启设备。
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		// 确定
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	/**
	 * 重新启动延迟后预览
	 * 
	 * @param delayMS
	 *            延迟执行的毫秒数
	 */
	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
		}
		resetStatusView();
	}

	/**
	 * 重置view状态
	 */
	private void resetStatusView() {
		resultView.setVisibility(View.GONE);
		statusView.setText(R.string.msg_default_status);
		statusView.setVisibility(View.VISIBLE);
		viewfinderView.setVisibility(View.VISIBLE);
		lastResult = null;
	}

	/**
	 * 绘制取景器
	 */
	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}
}
