package com.google.zxing.client.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * 支持条码扫描偏好
 * 
 * @see PreferencesActivity
 * 
 * @author lijian
 * @date 2017-9-3 下午9:12:13
 */
public final class PreferencesFragment extends PreferenceFragment implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private CheckBoxPreference[] checkBoxPrefs;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.preferences);

		PreferenceScreen preferences = getPreferenceScreen();
		preferences.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		checkBoxPrefs = findDecodePrefs(preferences,
				PreferencesActivity.KEY_DECODE_1D_PRODUCT,
				PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL,
				PreferencesActivity.KEY_DECODE_QR,
				PreferencesActivity.KEY_DECODE_DATA_MATRIX,
				PreferencesActivity.KEY_DECODE_AZTEC,
				PreferencesActivity.KEY_DECODE_PDF417);
		disableLastCheckedPref();

		EditTextPreference customProductSearch = (EditTextPreference) preferences
				.findPreference(PreferencesActivity.KEY_CUSTOM_PRODUCT_SEARCH);
		customProductSearch
				.setOnPreferenceChangeListener(new CustomSearchURLValidator());
	}

	private static CheckBoxPreference[] findDecodePrefs(
			PreferenceScreen preferences, String... keys) {
		CheckBoxPreference[] prefs = new CheckBoxPreference[keys.length];
		for (int i = 0; i < keys.length; i++) {
			prefs[i] = (CheckBoxPreference) preferences.findPreference(keys[i]);
		}
		return prefs;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		disableLastCheckedPref();
	}

	/**
	 * 检查选项是否开启
	 */
	private void disableLastCheckedPref() {
		Collection<CheckBoxPreference> checked = new ArrayList<CheckBoxPreference>(
				checkBoxPrefs.length);
		for (CheckBoxPreference pref : checkBoxPrefs) {
			if (pref.isChecked()) {
				checked.add(pref);
			}
		}
		boolean disable = checked.size() <= 1;
		for (CheckBoxPreference pref : checkBoxPrefs) {
			pref.setEnabled(!(disable && checked.contains(pref)));
		}
	}

	/**
	 * 自定义搜索URL验证器
	 * 
	 * @author lijian
	 * @date 2017-9-3 下午9:16:27
	 */
	private class CustomSearchURLValidator implements
			Preference.OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (!isValid(newValue)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						PreferencesFragment.this.getActivity());
				builder.setTitle(R.string.msg_error);// 错误
				builder.setMessage(R.string.msg_invalid_value);// 值无效
				builder.setCancelable(true);
				builder.show();
				return false;
			}
			return true;
		}

		/**
		 * 验证是否
		 * 
		 * @param newValue
		 * @return
		 */
		private boolean isValid(Object newValue) {
			// 允许empty/null
			if (newValue == null) {
				return true;
			}
			String valueString = newValue.toString();
			if (valueString.isEmpty()) {
				return true;
			}
			// 在验证之前，请删除自定义占位符，这些占位符在某些位置不会被视为URL的有效部分：空白、%t、%s
			valueString = valueString.replaceAll("%[st]", "");
			// 空白%f，但如果后跟数字或a-f，则可能是十六进制序列
			valueString = valueString.replaceAll("%f(?![0-9a-f])", "");
			// 需要一个方案否则:
			try {
				URI uri = new URI(valueString);
				return uri.getScheme() != null;
			} catch (URISyntaxException use) {
				return false;
			}
		}
	}

}
