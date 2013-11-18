package ca.mudar.patinoires.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.R;

/**
 * Created by mudar on 13/11/13.
 */
public class SettingsHelper {


    /**
     * Error proof verification: if the user unchecks all 4 conditions filters,
     * re-enable the last unchecked condition and display Toast message.
     */
    public static boolean verifyConditionsError(SharedPreferences prefs) {
        boolean hasEnabledCondition = false;

        hasEnabledCondition = prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_EXCELLENT, false)
                || hasEnabledCondition;
        hasEnabledCondition = prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_GOOD, false)
                || hasEnabledCondition;
        hasEnabledCondition = prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_BAD, false)
                || hasEnabledCondition;
        hasEnabledCondition = prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_CLOSED, false)
                || hasEnabledCondition;
        hasEnabledCondition = prefs.getBoolean(Const.PrefsNames.CONDITIONS_SHOW_UNKNOWN, false)
                || hasEnabledCondition;

        return hasEnabledCondition;
    }

    /**
     * Get display name of selected preference value. Example: "English" for
     * "en", "Metric" for "iso", etc.
     *
     * @param index Preference key
     * @return Display name of the value
     */
    public static String getSummaryByValue(Resources resources , String index) {
        if (index == null) {
            return "";
        } else if (index.equals(Const.PrefsValues.UNITS_ISO)) {
            return resources .getString(R.string.prefs_units_iso);
        } else if (index.equals(Const.PrefsValues.UNITS_IMP)) {
            return resources .getString(R.string.prefs_units_imperial);
        } else if (index.equals(Const.PrefsValues.LIST_SORT_NAME)) {
            return resources .getString(R.string.prefs_list_sort_name);
        } else if (index.equals(Const.PrefsValues.LIST_SORT_DISTANCE)) {
            return resources .getString(R.string.prefs_list_sort_distance);
        } else if (index.equals(Const.PrefsValues.LANG_FR)) {
            return resources .getString(R.string.prefs_language_french);
        } else if (index.equals(Const.PrefsValues.LANG_EN)) {
            return resources .getString(R.string.prefs_language_english);
        } else {
            return "";
        }
    }


}
