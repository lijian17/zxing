package com.google.zxing.client.android.share;

import java.util.List;

import com.google.zxing.client.android.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 一种定制的适配器，用于从光标中获取书签。在蜂窝我们使用SimpleCursorAdapter，但它假定一个_id柱的存在，和书签模式被改写为HC不一。这导致应用程序崩溃，因此这个新类是向前和向后兼容的。
 * 
 * @author lijian
 * @date 2017-9-3 下午7:26:22
 */
final class BookmarkAdapter extends BaseAdapter {

  private final Context context;
  /** 标题链接集合 */
  private final List<String[]> titleURLs;

  BookmarkAdapter(Context context, List<String[]> titleURLs) {
    this.context = context;
    this.titleURLs = titleURLs;
  }

  @Override
  public int getCount() {
    return titleURLs.size();
  }

  @Override
  public Object getItem(int index) {
    return titleURLs.get(index);
  }

  @Override
  public long getItemId(int index) {
    return index;
  }

  @Override
  public View getView(int index, View view, ViewGroup viewGroup) {
    View layout;
    if (view instanceof LinearLayout) {
      layout = view;
    } else {
      LayoutInflater factory = LayoutInflater.from(context);
      layout = factory.inflate(R.layout.bookmark_picker_list_item, viewGroup, false);
    }
    String[] titleURL = titleURLs.get(index);
    // 标题
    ((TextView) layout.findViewById(R.id.bookmark_title)).setText(titleURL[0]);
    // url地址
    ((TextView) layout.findViewById(R.id.bookmark_url)).setText(titleURL[1]);
    return layout;
  }
}
