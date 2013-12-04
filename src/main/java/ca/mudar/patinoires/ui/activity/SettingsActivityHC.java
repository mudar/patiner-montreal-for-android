/*
    Park Catcher Montréal
    Find a free parking in the nearest residential street when driving in
    Montréal. A Montréal Open Data project.

    Copyright (C) 2012 Mudar Noufal <mn@mudar.ca>

    This file is part of Park Catcher Montréal.

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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import java.util.List;
import java.util.Locale;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.utils.SettingsHelper;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsActivityHC extends PreferenceActivity {
    private static final String TAG = "MyPreferenceActivityHC";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar ab = getActionBar();

        ab.setDisplayHomeAsUpEnabled(true);

        showBreadCrumbs(getResources().getString(R.string.prefs_breadcrumb), null);
    }

    @Override
    public Intent getIntent() {
        // Override the original intent to remove headers and directly show
        // SettingsFragment
        final Intent intent = new Intent(super.getIntent());
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                SettingsFragment.class.getName());
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        return intent;
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Respond to the action bar's Up/Home button
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the interface language, independently from the phone's UI
     * language. This does not override the parent function because the Manifest
     * does not include configChanges.
     */
    private void onConfigurationChanged(String lg) {

        ((PatinoiresApp) getApplicationContext()).setLanguage(lg);

        // Notify favorites URI, for the AppWidget
        getApplicationContext().getContentResolver().notifyChange(RinksContract.Rinks.CONTENT_FAVORITES_URI, null);

        // Restart Settings with a fade in/out
        this.finish();
        Intent intent = new Intent(getApplicationContext(), SettingsActivityHC.class);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        ListPreference tUnits;
        ListPreference tListSort;
        ListPreference tLanguage;
        private SharedPreferences mSharedPrefs;
        private PatinoiresApp mAppHelper;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mAppHelper = (PatinoiresApp) getActivity().getApplicationContext();
            mAppHelper.updateUiLanguage();

            PreferenceManager pm = this.getPreferenceManager();
            pm.setSharedPreferencesName(Const.APP_PREFS_NAME);
            pm.setSharedPreferencesMode(Context.MODE_PRIVATE);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_hc);

            mSharedPrefs = pm.getSharedPreferences();

            tUnits = (ListPreference) findPreference(Const.PrefsNames.UNITS_SYSTEM);
            tListSort = (ListPreference) findPreference(Const.PrefsNames.LIST_SORT);
            tLanguage = (ListPreference) findPreference(Const.PrefsNames.LANGUAGE);

            /**
             * Handle the widget setting
             */
            final PreferenceScreen widgetPrefScreen = (PreferenceScreen) findPreference(Const.PrefsNames.SYSTEM_WIDGET_SETTINGS);
            widgetPrefScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    launchWidgetSettings();

                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            /**
             * Default units system is ISO
             */
            tUnits.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(Const.PrefsNames.UNITS_SYSTEM,
                    Const.PrefsValues.UNITS_ISO)));

            /**
             * Default sort list order is by name
             */
            tListSort.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(Const.PrefsNames.LIST_SORT,
                    Const.PrefsValues.LIST_SORT_DISTANCE)));

            /**
             * The app's Default language is the phone's language. If not supported,
             * we default to English.
             */
            String lg = mSharedPrefs.getString(Const.PrefsNames.LANGUAGE, Locale.getDefault().getLanguage());
            if (!lg.equals(Const.PrefsValues.LANG_EN) && !lg.equals(Const.PrefsValues.LANG_FR)) {
                lg = Const.PrefsValues.LANG_EN;
            }
            tLanguage.setSummary(SettingsHelper.getSummaryByValue(getResources(), mSharedPrefs.getString(Const.PrefsNames.LANGUAGE, lg)));
            /**
             * This is required because language initially defaults to phone
             * language.
             */
            tLanguage.setValue(lg);

            /**
             * Set up a listener whenever a key changes
             */
            if (mSharedPrefs != null) {
                mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onPause() {
            /**
             * Remove the listener onPause
             */
            if (mSharedPrefs != null) {
                mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
            }

            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            /**
             * onChanged, new preferences values are sent to the AppHelper.
             */
            if (key.equals(Const.PrefsNames.UNITS_SYSTEM)) {
                String units = prefs.getString(key, Const.PrefsValues.UNITS_ISO);
                tUnits.setSummary(SettingsHelper.getSummaryByValue(getResources(), units));
                mAppHelper.setUnits(units);
            } else if (key.equals(Const.PrefsNames.LIST_SORT)) {
                String sort = prefs.getString(key, Const.PrefsValues.LIST_SORT_DISTANCE);
                tListSort.setSummary(SettingsHelper.getSummaryByValue(getResources(), sort));
                mAppHelper.setListSort(sort);
            } else if (key.equals(Const.PrefsNames.LANGUAGE)) {
                String lg = prefs.getString(key, Locale.getDefault().getLanguage());
                tLanguage.setSummary(SettingsHelper.getSummaryByValue(getResources(), lg));
                ((SettingsActivityHC) getActivity()).onConfigurationChanged(lg);
            }
        }

        private void launchWidgetSettings() {
            try {
                startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
