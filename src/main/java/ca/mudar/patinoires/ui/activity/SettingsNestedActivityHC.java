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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.utils.SettingsHelper;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsNestedActivityHC extends PreferenceActivity {
    protected static final String TAG = "SettingsNestedActivityHC";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

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
     * This fragment shows the preferences for the first header.
     */
    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {
        private SharedPreferences mSharedPrefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            ((PatinoiresApp) getActivity().getApplicationContext()).updateUiLanguage();

            PreferenceManager pm = this.getPreferenceManager();
            pm.setSharedPreferencesName(Const.APP_PREFS_NAME);
            pm.setSharedPreferencesMode(Context.MODE_PRIVATE);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_nested_hc);

            mSharedPrefs = pm.getSharedPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

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

            final PatinoiresApp appHelper = (PatinoiresApp) getActivity().getApplicationContext();

            /**
             * onChanged, new preferences values are sent to the AppHelper.
             */
            if (key.equals(Const.PrefsNames.CONDITIONS_SHOW_EXCELLENT)) {
                appHelper.setConditionsFilter(
                        prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_EXCELLENT, true),
                        Const.INDEX_PREFS_EXCELLENT);
            } else if (key.equals(Const.PrefsNames.CONDITIONS_SHOW_GOOD)) {
                appHelper.setConditionsFilter(
                        prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_GOOD, true),
                        Const.INDEX_PREFS_GOOD);
            } else if (key.equals(Const.PrefsNames.CONDITIONS_SHOW_BAD)) {
                appHelper.setConditionsFilter(
                        prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_BAD, true),
                        Const.INDEX_PREFS_BAD);
            } else if (key.equals(Const.PrefsNames.CONDITIONS_SHOW_CLOSED)) {
                appHelper.setConditionsFilter(
                        prefs.getBoolean(
                                Const.PrefsNames.CONDITIONS_SHOW_CLOSED, true),
                        Const.INDEX_PREFS_CLOSED);
            } else if (key.equals(Const.PrefsNames.CONDITIONS_SHOW_UNKNOWN)) {
                appHelper.setConditionsFilter(
                        prefs.getBoolean(
                                Const.PrefsNames.CONDITIONS_SHOW_UNKNOWN, true),
                        Const.INDEX_PREFS_UNKNOWN);
            }

            if (!SettingsHelper.verifyConditionsError(prefs)) {
                ((CheckBoxPreference) findPreference(key)).setChecked(true);
                appHelper.showToastText(R.string.toast_prefs_conditions_error, Toast.LENGTH_LONG);
            }
        }
    }
}
