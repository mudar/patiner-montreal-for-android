/*
    Patiner Montréal for Android.
    Information about outdoor rinks in the city of Montréal: conditions,
    services, contact, map, etc.

    Copyright (C) 2010 Mudar Noufal <mn@mudar.ca>

    This file is part of Patiner Montréal for Android.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.mudar.patinoires.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import java.util.Locale;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.Const.PrefsNames;
import ca.mudar.patinoires.Const.PrefsValues;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.utils.SettingsHelper;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    protected static final String TAG = "SettingsActivity";
    protected SharedPreferences mSharedPrefs;
    ListPreference tUnits;
    ListPreference tListSort;
    ListPreference tLanguage;
    private PatinoiresApp mAppHelper;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Const.APP_PREFS_NAME);

        mAppHelper = (PatinoiresApp) getApplicationContext();
        mAppHelper.updateUiLanguage();

        addPreferencesFromResource(R.xml.preferences);

        mSharedPrefs = getSharedPreferences(Const.APP_PREFS_NAME, MODE_PRIVATE);

        tUnits = (ListPreference) findPreference(PrefsNames.UNITS_SYSTEM);
        tListSort = (ListPreference) findPreference(PrefsNames.LIST_SORT);
        tLanguage = (ListPreference) findPreference(PrefsNames.LANGUAGE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * Default units system is ISO
         */
        tUnits.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(PrefsNames.UNITS_SYSTEM,
                PrefsValues.UNITS_ISO)));

        /**
         * Default sort list order is by name
         */
        tListSort.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(PrefsNames.LIST_SORT,
                PrefsValues.LIST_SORT_DISTANCE)));

        /**
         * The app's Default language is the phone's language. If not supported,
         * we default to English.
         */
        String lg = mSharedPrefs.getString(PrefsNames.LANGUAGE, Locale.getDefault().getLanguage());
        if (!lg.equals(PrefsValues.LANG_EN) && !lg.equals(PrefsValues.LANG_FR)) {
            lg = PrefsValues.LANG_EN;
        }
        tLanguage.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(PrefsNames.LANGUAGE, lg)));
        /**
         * This is required because language initially defaults to phone
         * language.
         */
        tLanguage.setValue(lg);

        /**
         * Set up a listener whenever a key changes
         */
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /**
         * Remove the listener onPause
         */
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * ChangeListener
     */
    @SuppressWarnings("deprecation")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        boolean needsErrorCheck = false;
        /**
         * onChanged, new preferences values are sent to the AppHelper.
         */
        if (key.equals(PrefsNames.UNITS_SYSTEM)) {
            String units = prefs.getString(key, PrefsValues.UNITS_ISO);
            tUnits.setSummary(SettingsHelper.getSummaryByValue(getResources(), units));
            mAppHelper.setUnits(units);
        } else if (key.equals(PrefsNames.LIST_SORT)) {
            String sort = prefs.getString(key, PrefsValues.LIST_SORT_DISTANCE);
            tListSort.setSummary(SettingsHelper.getSummaryByValue(getResources(), sort));
            mAppHelper.setListSort(sort);
        } else if (key.equals(PrefsNames.LANGUAGE)) {
            String lg = prefs.getString(key, Locale.getDefault().getLanguage());
            tLanguage.setSummary(SettingsHelper.getSummaryByValue(getResources(), lg));
            onConfigurationChanged(lg);
        } else if (key.equals(PrefsNames.CONDITIONS_SHOW_EXCELLENT)) {
            mAppHelper.setConditionsFilter(
                    prefs.getBoolean(PrefsNames.CONDITIONS_SHOW_EXCELLENT, true),
                    Const.INDEX_PREFS_EXCELLENT);
            needsErrorCheck = true;
        } else if (key.equals(PrefsNames.CONDITIONS_SHOW_GOOD)) {
            mAppHelper.setConditionsFilter(prefs.getBoolean(PrefsNames.CONDITIONS_SHOW_GOOD, true),
                    Const.INDEX_PREFS_GOOD);
            needsErrorCheck = true;
        } else if (key.equals(PrefsNames.CONDITIONS_SHOW_BAD)) {
            mAppHelper.setConditionsFilter(prefs.getBoolean(PrefsNames.CONDITIONS_SHOW_BAD, true),
                    Const.INDEX_PREFS_BAD);
            needsErrorCheck = true;
        } else if (key.equals(PrefsNames.CONDITIONS_SHOW_CLOSED)) {
            mAppHelper.setConditionsFilter(
                    prefs.getBoolean(PrefsNames.CONDITIONS_SHOW_CLOSED, true),
                    Const.INDEX_PREFS_CLOSED);
            needsErrorCheck = true;
        } else if (key.equals(PrefsNames.CONDITIONS_SHOW_UNKNOWN)) {
            mAppHelper.setConditionsFilter(
                    prefs.getBoolean(PrefsNames.CONDITIONS_SHOW_UNKNOWN, true),
                    Const.INDEX_PREFS_UNKNOWN);
            needsErrorCheck = true;
        }

        if (needsErrorCheck) {
            if (!SettingsHelper.verifyConditionsError(prefs)) {
                ((CheckBoxPreference) findPreference(key)).setChecked(true);
                mAppHelper.showToastText(R.string.toast_prefs_conditions_error, Toast.LENGTH_LONG);
            }
        }
    }

    /**
     * Update the interface language, independently from the phone's UI
     * language. This does not override the parent function because the Manifest
     * does not include configChanges.
     */
    private void onConfigurationChanged(String lg) {
        mAppHelper.setLanguage(lg);
        mAppHelper.updateUiLanguage();

        finish();
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }

}
