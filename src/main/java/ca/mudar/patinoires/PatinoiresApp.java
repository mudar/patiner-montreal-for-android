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

package ca.mudar.patinoires;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.location.Location;
import android.widget.Toast;

import java.util.Locale;

import ca.mudar.patinoires.Const.PrefsNames;
import ca.mudar.patinoires.Const.PrefsValues;
import ca.mudar.patinoires.services.DistanceUpdateService;

public class PatinoiresApp extends Application {
    protected static final String TAG = "AppHelper";
    // TODO: verify possible memory leakage of the following code
    private static PatinoiresApp instance = null;
    // TODO Verify need for a global variable for mLocation since it's mainly in
    // the preferences.
    private Location mLocation;
    private long mLastUpdateLocations;
    private long mLastUpdateConditions;
    private String mUnits;
    private String mListSort;
    private String mLanguage;
    private Toast mToast;
    private boolean[] conditionsFilter = new boolean[5];
    private boolean mIsSeasonOver;
    private boolean mHasFavoriteRinks = false;
    private SharedPreferences prefs;
    private Editor prefsEditor;

    public static PatinoiresApp getInstance() {
        checkInstance();
        return instance;
    }

    private static void checkInstance() {
        if (instance == null)
            throw new IllegalStateException("Application not created yet!");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        System.setProperty("http.keepAlive", "false");

        prefs = getSharedPreferences(Const.APP_PREFS_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();

        /**
         * Initialize UI settings based on preferences.
         */
        mUnits = prefs.getString(PrefsNames.UNITS_SYSTEM, PrefsValues.UNITS_ISO);

        mListSort = prefs.getString(PrefsNames.LIST_SORT, PrefsValues.LIST_SORT_DISTANCE);

        mLanguage = prefs.getString(PrefsNames.LANGUAGE, Locale.getDefault().getLanguage());
        if (!mLanguage.equals(PrefsValues.LANG_EN) && !mLanguage.equals(PrefsValues.LANG_FR)) {
            mLanguage = PrefsValues.LANG_EN;
        }

        mIsSeasonOver = prefs.getBoolean(PrefsNames.IS_SEASON_OVER, false);

        /**
         * Display/hide rinks based on their condition
         */
        conditionsFilter[Const.INDEX_PREFS_EXCELLENT] = prefs.getBoolean(
                PrefsNames.CONDITIONS_SHOW_EXCELLENT, true);
        conditionsFilter[Const.INDEX_PREFS_GOOD] = prefs.getBoolean(
                PrefsNames.CONDITIONS_SHOW_GOOD, true);
        conditionsFilter[Const.INDEX_PREFS_BAD] = prefs.getBoolean(
                PrefsNames.CONDITIONS_SHOW_BAD, true);
        conditionsFilter[Const.INDEX_PREFS_CLOSED] = prefs.getBoolean(
                PrefsNames.CONDITIONS_SHOW_CLOSED, true);
        conditionsFilter[Const.INDEX_PREFS_UNKNOWN] = prefs.getBoolean(
                PrefsNames.CONDITIONS_SHOW_UNKNOWN, true);

        mLastUpdateLocations = prefs.getLong(PrefsNames.LAST_UPDATE_TIME_LOCATIONS,
                System.currentTimeMillis() - Const.MILLISECONDS_FIVE_DAYS);
        mLastUpdateConditions = prefs.getLong(PrefsNames.LAST_UPDATE_TIME_CONDITIONS,
                System.currentTimeMillis() - Const.MILLISECONDS_FOUR_HOURS);

        /**
         * Having a single Toast instance allows overriding (replacing) the
         * messages and avoiding Toast stack delays.
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mLocation = null;

        updateUiLanguage();
    }

    /**
     * Used to force distance calculations. Mainly on first launch where an
     * empty or partial DB cursor receives the location update, ends up doing
     * partial distance updates.
     */
    public void initializeLocation() {
        // TODO replace this by a listener or synch tasks
        mLocation = null;

        prefsEditor.putFloat(PrefsNames.LAST_UPDATE_LAT, Float.NaN);
        prefsEditor.putFloat(PrefsNames.LAST_UPDATE_LNG, Float.NaN);
        prefsEditor.putLong(PrefsNames.LAST_UPDATE_TIME_GEO, System.currentTimeMillis());
        prefsEditor.commit();
    }

    public Location getLocation() {
        /**
         * Background services save a passively set location in the Preferences.
         */
        Float lastLat = prefs.getFloat(PrefsNames.LAST_UPDATE_LAT, Float.NaN);
        Float lastLng = prefs.getFloat(PrefsNames.LAST_UPDATE_LNG, Float.NaN);

        if (lastLat.equals(Float.NaN) || lastLng.equals(Float.NaN)) {
            return mLocation;
        }

        mLocation = new Location(Const.LOCATION_PROVIDER);
        mLocation.setLatitude(lastLat.doubleValue());
        mLocation.setLongitude(lastLng.doubleValue());

        return mLocation;
    }

    public void setLocation(Location location) {
//        Log.v(TAG, "setLocation");
        if (location != null) {
//            Log.v(TAG, "new location = " + location.getLatitude() + "," + location.getLongitude());
            if ((mLocation == null) || (this.mLocation.distanceTo(location) > Const.MAX_DISTANCE)) {
                Intent intent = new Intent(this.getApplicationContext(),
                        DistanceUpdateService.class);
                intent.putExtra(Const.INTENT_EXTRA_GEO_LAT, location.getLatitude());
                intent.putExtra(Const.INTENT_EXTRA_GEO_LNG, location.getLongitude());
                startService(intent);
            }
            /**
             * No need to save location in Preferences because it's done in the
             * background services.
             */

            mLocation = location;
        }
    }

    public long getLastUpdateLocations() {
        return mLastUpdateLocations;
    }

    public void setLastUpdateLocations() {
        mLastUpdateLocations = System.currentTimeMillis();
        mLastUpdateConditions = System.currentTimeMillis();

        prefsEditor.putLong(PrefsNames.LAST_UPDATE_TIME_LOCATIONS, mLastUpdateLocations);
        prefsEditor.putLong(PrefsNames.LAST_UPDATE_TIME_CONDITIONS, mLastUpdateConditions);
        prefsEditor.commit();
    }

    public long getLastUpdateConditions() {
        return mLastUpdateConditions;
    }

    public void setLastUpdateConditions() {
        mLastUpdateConditions = System.currentTimeMillis();

        prefsEditor.putLong(PrefsNames.LAST_UPDATE_TIME_CONDITIONS, mLastUpdateConditions);
        prefsEditor.commit();
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String lang) {
        this.mLanguage = lang;
        updateUiLanguage();
    }

    /**
     * Force the configuration change to a locale different that the phone's.
     */
    public void updateUiLanguage() {
        Locale locale = new Locale(mLanguage);
        Configuration config = new Configuration();
        config.locale = locale;
        Locale.setDefault(locale);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    public void showToastText(int res, int duration) {
        mToast.setText(res);
        mToast.setDuration(duration);
        mToast.show();
    }

    public void showToastText(String msg, int duration) {
        mToast.setText(msg);
        mToast.setDuration(duration);
        mToast.show();
    }

    public String getListSort() {
        return mListSort;
    }

    public void setListSort(String sort) {
        this.mListSort = sort;
    }

    public String getUnits() {
        return mUnits;
    }

    public void setUnits(String units) {
        this.mUnits = units;
    }

    public boolean[] getConditionsFilter() {
        return conditionsFilter;
    }

    public void setConditionsFilter(boolean[] conditions) {
        this.conditionsFilter = conditions;
    }

    public boolean getConditionsFilter(int index) {
        return conditionsFilter[index];
    }

    public void setConditionsFilter(boolean condition, int index) {
        this.conditionsFilter[index] = condition;
    }

    public boolean isSeasonOver() {
        return mIsSeasonOver;
    }

    public void setSeasonOver(boolean isSeasonOver) {
        mIsSeasonOver = isSeasonOver;

        prefsEditor.putBoolean(PrefsNames.IS_SEASON_OVER, isSeasonOver);
        prefsEditor.commit();
    }

    public boolean canDisplayWidgetTip() {
        final boolean hasSeenWidgetTip = prefs.getBoolean(PrefsNames.HAS_SEEN_WIDGET_TIP, false);

        return (!hasSeenWidgetTip && mHasFavoriteRinks && Const.SUPPORTS_HONEYCOMB);
    }

    public void setHasFavoriteRinks(boolean hasFavoriteRinks) {
        mHasFavoriteRinks = hasFavoriteRinks;
    }

    public void setHasSeenWidgetTip(boolean hasSeenToast) {
        prefsEditor.putBoolean(PrefsNames.HAS_SEEN_WIDGET_TIP, hasSeenToast);
        prefsEditor.commit();
    }
}
