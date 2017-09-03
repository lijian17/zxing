package com.google.zxing.client.android.share;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;

/**
 * 选择已安装应用程序以通过Intent共享的活动
 * 
 * @author lijian
 * @date 2017-9-3 下午7:34:07
 */
public final class AppPickerActivity extends ListActivity {

	private AsyncTask<Object, Object, List<AppInfo>> backgroundTask;

	@Override
	protected void onResume() {
		super.onResume();
		backgroundTask = new LoadPackagesAsyncTask(this);
		backgroundTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	protected void onPause() {
		AsyncTask<?, ?, ?> task = backgroundTask;
		if (task != null) {
			task.cancel(true);
			backgroundTask = null;
		}
		super.onPause();
	}

	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {
		Adapter adapter = getListAdapter();
		if (position >= 0 && position < adapter.getCount()) {
			String packageName = ((AppInfo) adapter.getItem(position))
					.getPackageName();
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("url", "market://details?id=" + packageName); // Browser.BookmarkColumns.URL
			setResult(RESULT_OK, intent);
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

}
