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

package ca.mudar.patinoires.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.R;

public class SettingsHelper {


    /**
     * Error proof verification: if the user unchecks all 4 conditions filters,
     * re-enable the last unchecked condition and display Toast message.
     */
    public static boolean verifyConditionsError(SharedPreferences prefs) {
        return prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_EXCELLENT, false)
                || prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_GOOD, false)
                || prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_BAD, false)
                || prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_CLOSED, false)
                || prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_UNKNOWN, false);
    }

    /**
     * Get display name of selected preference value. Example: "English" for
     * "en", "Metric" for "iso", etc.
     *
     * @param index Preference key
     * @return Display name of the value
     */
    public static String getSummaryByValue(Resources resources, String index) {
        if (index == null) {
            return "";
        } else if (index.equals(Const.PrefsValues.UNITS_ISO)) {
            return resources.getString(R.string.prefs_units_iso);
        } else if (index.equals(Const.PrefsValues.UNITS_IMP)) {
            return resources.getString(R.string.prefs_units_imperial);
        } else if (index.equals(Const.PrefsValues.LIST_SORT_NAME)) {
            return resources.getString(R.string.prefs_list_sort_name);
        } else if (index.equals(Const.PrefsValues.LIST_SORT_DISTANCE)) {
            return resources.getString(R.string.prefs_list_sort_distance);
        } else if (index.equals(Const.PrefsValues.LANG_FR)) {
            return resources.getString(R.string.prefs_language_french);
        } else if (index.equals(Const.PrefsValues.LANG_EN)) {
            return resources.getString(R.string.prefs_language_english);
        } else {
            return "";
        }
    }
}
