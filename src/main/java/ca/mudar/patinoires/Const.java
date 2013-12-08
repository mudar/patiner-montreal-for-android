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

import android.app.AlarmManager;
import android.os.Build;

public class Const {

    public static final String APP_PREFS_NAME = "PatinoiresPrefsFile";
    public static final String URL_JSON_SEASON_STATUS = "http://patinoires.mudar.ca/api/season.json";
    public static final String URL_JSON_INITIAL_IMPORT = "http://www.patinermontreal.ca/data.json";
    public static final String URL_JSON_CONDITIONS_UPDATES = "http://www.patinermontreal.ca/conditions.json";
    public static final String URL_GMAPS_DIRECTIONS = "http://maps.google.com/maps?saddr=%s&daddr=%s&dirflg=r";
    public static final String URL_PLAYSTORE = "http://play.google.com/store/apps/details?id=ca.mudar.patinoires";
    public static final int TABS_INDEX_SKATING = 0x0;
    public static final int TABS_INDEX_HOCKEY = 0x1;
    public static final int TABS_INDEX_ALL = 0x2;
    public static final int TABS_INDEX_FAVORITES = 0x3;
    public static final String TABS_TAG_FAVORITES = "tab_favorites";
    public static final String TABS_TAG_SKATING = "tab_skating";
    public static final String TABS_TAG_HOCKEY = "tab_hockey";
    public static final String TABS_TAG_ALL = "tab_all";
    public static final int INDEX_PREFS_EXCELLENT = 0x0;
    public static final int INDEX_PREFS_GOOD = 0x1;
    public static final int INDEX_PREFS_BAD = 0x2;
    public static final int INDEX_PREFS_CLOSED = 0x3;
    public static final int INDEX_PREFS_UNKNOWN = 0x4;
    public static final String LOCATION_PROVIDER_DEFAULT = "DefaultLocationProvider";
    public static final String LOCATION_PROVIDER_SEARCH = "SearchLocationProvider";
    public static final String LOCATION_PROVIDER_INTENT = "IntentLocationProvider";
    public static final String LOCATION_PROVIDER_PREFS = "PrefsLocationProvider";
    public static final String LOCATION_PROVIDER_SERVICE = "ServiceLocationProvider";
    public static final double MAPS_DEFAULT_COORDINATES[] = {
            45.5d, -73.666667d
    };
    public static final double MAPS_GEOCODER_LIMITS[] = {
            45.380127d, // lowerLeftLat
            -73.982620d, // lowerLeftLng
            45.720444d, // upperRightLat
            -73.466087d
    };


    public static final String INTENT_EXTRA_GEO_LAT = "geo_lat";
    public static final String INTENT_EXTRA_GEO_LNG = "geo_lng";
    public static final String INTENT_EXTRA_FORCE_UPDATE = "force_update";
    public static final String INTENT_EXTRA_TABS_CURRENT = "tabs_current";
    public static final String INTENT_EXTRA_ID_RINK = "id_rink";
    public static final String INTENT_EXTRA_URL_PATH_FR = "patinoires";
    public static final String INTENT_EXTRA_URL_PATH_EN = "rinks";
    public static final String INTENT_EXTRA_LOCAL_SYNC = "sync_assets";
    public static final int INTENT_REQ_CODE_EULA = 0x10;
    public static final String INTENT_ACTION_WIDGET_RINK = "ca.mudar.patinoires.widget_rink";
    public static final String INTENT_ACTION_PASSIVE_LOCATION = "ca.mudar.patinoires.passive_location";
    public static final String TAG_FRAGMENT_SEARCH = "tag_fragment_search";
    public static final String KEY_BUNDLE_SEARCH_ADDRESS = "bundle_search_address";
    public static final int BUNDLE_SEARCH_ADDRESS_SUCCESS = 0x1;
    public static final int BUNDLE_SEARCH_ADDRESS_ERROR = 0x0;
    public static final String KEY_BUNDLE_ADDRESS_LAT = "bundle_address_lat";
    public static final String KEY_BUNDLE_ADDRESS_LNG = "bundle_address_lng";
    public static final String KEY_BUNDLE_ADDRESS_DESC = "bundle_address_desc";
    public static final String LOCATION_PROVIDER = "my_default_provider";
    public static final long MILLISECONDS_FOUR_HOURS = 14400000; // 1000*60*60*4
    public static final long MILLISECONDS_FIVE_DAYS = 432000000; // 1000*60*60*24*5
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static boolean SUPPORTS_JELLY_BEAN = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    public static boolean SUPPORTS_ICE_CREAM_SANDWICH = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    public static boolean SUPPORTS_HONEYCOMB = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    /**
     * Location
     */
    // The default search radius when searching for places nearby.
    public static int DEFAULT_RADIUS = 150;
    // The maximum distance the user should travel between location updates.
    public static int MAX_DISTANCE = DEFAULT_RADIUS / 2;
    // The maximum time that should pass before the user gets a location update.
    public static long MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static int DB_MAX_DISTANCE = MAX_DISTANCE / 2;

    // Assets
    public static interface LocalAssets {
        final String LICENSE = "gpl-3.0-standalone.html";
        final String RINKS_DATA = "rinks.json";
    }

    // Search
    public static interface Search {
        final int MIN_LENGTH_SEARCH = 2;
        final String SCHEME = "content";
    }

    public static interface DbValues {
        final int SORT_DISTANCE = 0x0;
        final int SORT_RINK = 0x1;
        final int SORT_PARK = 0x2;
        final int SORT_BOROUGH = 0x3;
        final int KIND_PP = 0x4; // paysagée
        final int KIND_PPL = 0x5; // patin libre
        final int KIND_PSE = 0x6; // sport d'équipe
        final int KIND_C = 0xff; // citoyens
        final int CONDITION_EXCELLENT = 0x0;
        final int CONDITION_GOOD = 0x1;
        final int CONDITION_BAD = 0x2;
        final int CONDITION_CLOSED = 0x3;
        final int CONDITION_UNKNOWN = 0x4;
        final String DATE_FORMAT = "yyyy-MM-dd";
    }

    public static interface PrefsNames {
        final String HAS_LOADED_DATA = "prefs_has_loaded_data";
        final String VERSION_DATABASE = "prefs_version_database";
        final String LANGUAGE = "prefs_language";
        final String UNITS_SYSTEM = "prefs_units_system";
        final String LIST_SORT = "prefs_list_sort_by";
        final String FOLLOW_LOCATION_CHANGES = "prefs_follow_location_changes";
        final String LAST_UPDATE_TIME_LOCATIONS = "prefs_last_update_time_locations";
        final String LAST_UPDATE_TIME_CONDITIONS = "prefs_last_update_time_conditions";
        final String LAST_UPDATE_TIME_GEO = "prefs_last_update_time_geo";
        final String LAST_UPDATE_LAT = "prefs_last_update_lat";
        final String LAST_UPDATE_LNG = "prefs_last_update_lng";
        final String CONDITIONS_SHOW_EXCELLENT = "prefs_show_excellent";
        final String CONDITIONS_SHOW_GOOD = "prefs_show_good";
        final String CONDITIONS_SHOW_BAD = "prefs_show_bad";
        final String CONDITIONS_SHOW_CLOSED = "prefs_show_closed";
        final String CONDITIONS_SHOW_UNKNOWN = "prefs_show_unknown";
        final String IS_SEASON_OVER = "prefs_season_over";
        final String HAS_ACCEPTED_EULA = "accepted_eula"; // maintain old prefs name
        final String HAS_SEEN_WIDGET_TIP = "prefs_has_seen_widget_tip";
    }

    public static interface PrefsValues {
        final String LANG_FR = "fr";
        final String LANG_EN = "en";
        final String UNITS_ISO = "iso";
        final String UNITS_IMP = "imp";
        final String LIST_SORT_NAME = "name";
        final String LIST_SORT_DISTANCE = "distance";
    }

    public static interface UnitsDisplay {
        final float FEET_PER_MILE = 5280f;
        final float METER_PER_MILE = 1609.344f;
        final int ACCURACY_FEET_FAR = 100;
        final int ACCURACY_FEET_NEAR = 10;
        final int MIN_FEET = 200;
        final int MIN_METERS = 100;
    }
}
