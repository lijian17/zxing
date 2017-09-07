package com.google.zxing.client.android.result;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.CalendarParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * 处理以QR码编码的日历条目
 * 
 * @author lijian
 * @date 2017-9-7 下午10:45:21
 */
public final class CalendarResultHandler extends ResultHandler {
	private static final String TAG = CalendarResultHandler.class
			.getSimpleName();

	private static final int[] buttons = {//
	R.string.button_add_calendar // 添加至日程表
	};

	/**
	 * 处理以QR码编码的日历条目
	 * 
	 * @param activity
	 * @param result
	 */
	public CalendarResultHandler(Activity activity, ParsedResult result) {
		super(activity, result);
	}

	@Override
	public int getButtonCount() {
		return buttons.length;
	}

	@Override
	public int getButtonText(int index) {
		return buttons[index];
	}

	@Override
	public void handleButtonPress(int index) {
		if (index == 0) {
			CalendarParsedResult calendarResult = (CalendarParsedResult) getResult();

			String description = calendarResult.getDescription();
			String organizer = calendarResult.getOrganizer();
			if (organizer != null) { // 没有单独的意图键，放在描述中
				if (description == null) {
					description = organizer;
				} else {
					description = description + '\n' + organizer;
				}
			}

			addCalendarEvent(calendarResult.getSummary(),
					calendarResult.getStart(), calendarResult.isStartAllDay(),
					calendarResult.getEnd(), calendarResult.getLocation(),
					description, calendarResult.getAttendees());
		}
	}

	/**
	 * 通过预先添加添加事件UI来发送创建新的日历事件的意图。 系统的旧版本有一个错误，事件标题将不会被填写
	 * 
	 * @param summary
	 *            事件的描述
	 * @param start
	 *            开始时间
	 * @param allDay
	 *            如果为真，事件被认为是从开始时起始的一整天
	 * @param end
	 *            结束时间（可选）
	 * @param location
	 *            事件位置的文本描述
	 * @param description
	 *            事件本身的文本描述
	 * @param attendees
	 *            与会者邀请
	 */
	private void addCalendarEvent(String summary, Date start, boolean allDay,
			Date end, String location, String description, String[] attendees) {
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.setType("vnd.android.cursor.item/event");
		long startMilliseconds = start.getTime();
		intent.putExtra("beginTime", startMilliseconds);
		if (allDay) {
			intent.putExtra("allDay", true);
		}
		long endMilliseconds;
		if (end == null) {
			if (allDay) {
				// + 1 day
				endMilliseconds = startMilliseconds + 24 * 60 * 60 * 1000;
			} else {
				endMilliseconds = startMilliseconds;
			}
		} else {
			endMilliseconds = end.getTime();
		}
		intent.putExtra("endTime", endMilliseconds);
		intent.putExtra("title", summary);
		intent.putExtra("eventLocation", location);
		intent.putExtra("description", description);
		if (attendees != null) {
			intent.putExtra(Intent.EXTRA_EMAIL, attendees);
			// 文档说这是一个String[]或逗号分隔的String，这是正确的?
		}

		try {
			// 首先手动进行
			rawLaunchIntent(intent);
		} catch (ActivityNotFoundException anfe) {
			Log.w(TAG, "没有可以响应的日历应用程序 " + Intent.ACTION_INSERT);
			// 对于不喜欢“INSERT”的日历应用程序：
			intent.setAction(Intent.ACTION_EDIT);
			launchIntent(intent); // 如果没有任何东西可以处理它，这里是真的
		}
	}

	@Override
	public CharSequence getDisplayContents() {

		CalendarParsedResult calResult = (CalendarParsedResult) getResult();
		StringBuilder result = new StringBuilder(100);

		ParsedResult.maybeAppend(calResult.getSummary(), result);

		Date start = calResult.getStart();
		ParsedResult.maybeAppend(format(calResult.isStartAllDay(), start),
				result);

		Date end = calResult.getEnd();
		if (end != null) {
			if (calResult.isEndAllDay() && !start.equals(end)) {
				// 仅显示年/月/日
				// 如果是全天，这是结束日期，它是排他的，所以显示用户
				// 它在前一天结束，使更直观的感觉。
				// 但是如果事件已经（不正确地）指定了相同的开始/结束，那么不要这样做
				end = new Date(end.getTime() - 24 * 60 * 60 * 1000);
			}
			ParsedResult.maybeAppend(format(calResult.isEndAllDay(), end),
					result);
		}

		ParsedResult.maybeAppend(calResult.getLocation(), result);
		ParsedResult.maybeAppend(calResult.getOrganizer(), result);
		ParsedResult.maybeAppend(calResult.getAttendees(), result);
		ParsedResult.maybeAppend(calResult.getDescription(), result);
		return result.toString();
	}

	private static String format(boolean allDay, Date date) {
		if (date == null) {
			return null;
		}
		DateFormat format = allDay ? DateFormat
				.getDateInstance(DateFormat.MEDIUM) : DateFormat
				.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		return format.format(date);
	}

	@Override
	public int getDisplayTitle() {
		return R.string.result_calendar;// 找到日程
	}
}
