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

package ca.mudar.patinoires.ui;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.BoroughsColumns;
import ca.mudar.patinoires.providers.RinksContract.Favorites;
import ca.mudar.patinoires.providers.RinksContract.FavoritesColumns;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Const.DbValues;
import ca.mudar.patinoires.utils.Const.PrefsValues;
import ca.mudar.patinoires.utils.Helper;
import ca.mudar.patinoires.utils.NotifyingAsyncQueryHandler;

public class RinkDetailsFragment extends Fragment
        implements NotifyingAsyncQueryHandler.AsyncQueryListener {
    private static final String TAG = "RinkDetailsFragment";
    protected static int mRinkId = -1;
    protected static Uri mRinkUri = null;
    protected ActivityHelper mActivityHelper;
    protected PatinoiresApp mAppHelper;
    protected View mRootView;
    protected int mIsFavorite = 0;
    protected double mGeoLat = 0;
    protected double mGeoLng = 0;
    protected String mRinkName = "";
    protected Resources mResources;
    protected NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mActivityHelper = ActivityHelper.createInstance(getActivity());

        mAppHelper = ((PatinoiresApp) getActivity().getApplicationContext());
        mResources = getResources();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_rinks_details, container, false);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);

        Intent intent = getActivity().getIntent();

        // TODO Optimize this using savedInstanceState to avoid reload of
        // identical data onResume
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            mRinkId = getIdFromUri(intent.getData());
        }
        // else if ((savedInstanceState != null)
        // && savedInstanceState.containsKey(Const.KEY_INSTANCE_RINK_ID)) {
        // mRinkId = savedInstanceState.getInt(Const.KEY_INSTANCE_RINK_ID);
        // }
        else {
            mRinkId = intent.getIntExtra(Const.INTENT_EXTRA_ID_RINK, -1);
        }

        mRinkUri = RinksContract.Rinks.buildRinkUri(Integer.toString(mRinkId));

        Cursor cur = getActivity().getApplicationContext().getContentResolver()
                .query(mRinkUri, RinksQuery.PROJECTION, null, null, null);

        try {
            if (!cur.moveToFirst()) {
                return;
            }
            updateRinkInfo(cur);
            updateConditionsInfo(cur);
            updateTimeInfo(cur);
            getActivity().supportInvalidateOptionsMenu();

        } finally {
            cur.close();
        }

        // mHandler = new
        // NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        // mHandler.startQuery(RinksQuery._TOKEN, mRinkUri,
        // RinksQuery.PROJECTION);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_rink_details_fragment, menu);

        int resIcon = (mIsFavorite == 1 ? R.drawable.ic_action_star_on
                : R.drawable.ic_action_star_off);

        menu.findItem(R.id.menu_favorites_toggle).setIcon(resIcon);

        if ((mGeoLat == 0) || (mGeoLng == 0)) {
            menu.findItem(R.id.menu_gmaps_directions).setVisible(false);
            menu.findItem(R.id.map_view_rink).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_favorites_toggle) {
            onCheckedChanged(mIsFavorite == 1 ? true : false);

            mIsFavorite = (mIsFavorite == 0 ? 1 : 0); // Toggle value
            getActivity().supportInvalidateOptionsMenu();
            mActivityHelper.notifyAllTabs(getActivity().getContentResolver());
            return true;
            // return false;
        } else if (item.getItemId() == R.id.menu_gmaps_directions) {

            if ((mGeoLat != 0) && (mGeoLng != 0)) {
                /**
                 * Get directions using Intents.
                 */
                Location userLocation = mAppHelper.getLocation();
                String sAddr = Double.toString(userLocation.getLatitude()) + ","
                        + Double.toString(userLocation.getLongitude());
                String urlGmaps = String.format(Const.URL_GMAPS_DIRECTIONS, sAddr,
                        mGeoLat + "," + mGeoLng);

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(urlGmaps));
                startActivity(Intent.createChooser(intent,
                        mResources.getString(R.string.dialog_title_maps_chooser)));
            }
        } else if (item.getItemId() == R.id.map_view_rink) {

            mActivityHelper.goMap(mGeoLat, mGeoLng);
            return true;
        }

        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        Long millis = System.currentTimeMillis();

        String userUpdatedAt = (String) DateUtils.getRelativeTimeSpanString(millis - 1000,
                millis, 0, DateUtils.FORMAT_ABBREV_RELATIVE);
        ((TextView) mRootView.findViewById(R.id.l_user_updated_at)).setText(String.format(
                mResources.getString(R.string.rink_details_user_updated_at),
                userUpdatedAt
        ));
    }

    /**
     * Update conditions and timestamps. Triggered by RinkDetailsActivity after
     * conditions refresh.
     */
    public void onConditionsRefresh() {
        Cursor cur = getActivity().getApplicationContext().getContentResolver()
                .query(mRinkUri, RinksQuery.PROJECTION, null, null, null);

        try {
            if (!cur.moveToFirst()) {
                return;
            }
            updateConditionsInfo(cur);
            updateTimeInfo(cur);
        } finally {
            cur.close();
        }
    }

    private void updateRinkInfo(Cursor cursor) {
        String lang = mAppHelper.getLanguage();
        int visibility;

        mRinkName = cursor.getString(RinksQuery.RINK_NAME);
        String desc = cursor.getString(lang.equals(PrefsValues.LANG_FR) ? RinksQuery.RINK_DESC_FR
                : RinksQuery.RINK_DESC_EN);

        mIsFavorite = cursor.getInt(RinksQuery.RINK_IS_FAVORITE);
        mGeoLat = cursor.getDouble(RinksQuery.PARK_GEO_LAT);
        mGeoLng = cursor.getDouble(RinksQuery.PARK_GEO_LNG);

        ((TextView) mRootView.findViewById(R.id.l_rink_name)).setText(mRinkName);
        ((TextView) mRootView.findViewById(R.id.l_rink_desc)).setText(desc);

        String boroughName = cursor.getString(RinksQuery.BOROUGH_NAME);
        ((TextView) mRootView.findViewById(R.id.l_borough_name)).setText(boroughName);

        String prefixParcName = mResources.getString(R.string.rink_details_park_name);
        String name = String.format(cursor.getString(RinksQuery.PARK_NAME), prefixParcName);
        String address = cursor.getString(RinksQuery.PARK_ADDRESS);
        int distance = cursor.getInt(RinksQuery.PARK_GEO_DISTANCE);
        String sDistance = (distance > 0 ? Helper.getDistanceDisplay(getActivity()
                .getApplicationContext(), distance) : null);

        final String phone = cursor.getString(RinksQuery.PARK_PHONE);

        ((TextView) mRootView.findViewById(R.id.l_park_name)).setText(name);
        if (address.equals(name)) {
            /**
             * Avoid displaying useless duplicate information for the park
             * address.
             */
            visibility = View.GONE;
        } else {
            visibility = View.VISIBLE;
            ((TextView) mRootView.findViewById(R.id.l_park_address)).setText(address);
        }
        mRootView.findViewById(R.id.l_park_address).setVisibility(visibility);

        if (sDistance == null) {
            visibility = View.GONE;
        } else {
            visibility = View.VISIBLE;
            ((TextView) mRootView.findViewById(R.id.l_park_distance)).setText(
                    String.format(mResources.getString(R.string.rink_details_park_distance),
                            sDistance)
            );
        }
        mRootView.findViewById(R.id.l_park_distance).setVisibility(visibility);

        if (phone == null) {
            /**
             * Hide phone info.
             */
            visibility = View.GONE;
        } else {
            visibility = View.VISIBLE;
            ((TextView) mRootView.findViewById(R.id.l_park_phone)).setText(String.format(
                    mResources.getString(R.string.rink_details_park_phone),
                    phone
            ));

            ImageButton dialerButton = (ImageButton) mRootView.findViewById(R.id.l_rink_call);
            dialerButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            });
        }
        mRootView.findViewById(R.id.l_park_phone).setVisibility(visibility);
        mRootView.findViewById(R.id.l_rink_call).setVisibility(visibility);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateConditionsInfo(Cursor cursor) {
        int kindId = cursor.getInt(RinksQuery.RINK_KIND_ID);

        final int isCleared = cursor.getInt(RinksQuery.RINK_IS_CLEARED);
        final int isFlooded = cursor.getInt(RinksQuery.RINK_IS_FLOODED);
        final int isResurfaced = cursor.getInt(RinksQuery.RINK_IS_RESURFACED);
        int condition = cursor.getInt(RinksQuery.RINK_CONDITION);

        ((ImageView) mRootView.findViewById(R.id.l_rink_kind_id))
                .setImageResource(Helper.getRinkImage(kindId, condition));

        /**
         * Display condition on a colored background.
         */
        TextView vCondition = (TextView) mRootView.findViewById(R.id.l_rink_condition);
        vCondition.setText(String.format(
                mResources.getString(R.string.rink_details_conditions),
                getConditionText(condition)
        ));

        if (Const.SUPPORTS_JELLYBEAN) {
            vCondition.setBackground(mResources.getDrawable(Helper.getConditionBackground(condition)));
        }
        else {
            vCondition.setBackgroundResource(Helper.getConditionBackground(condition));
        }

        vCondition.setTextColor(mResources.getColor(Helper.getConditionTextColor(condition)));

        int visibility = View.GONE;
        int visibilitySurface = View.GONE;
        if ((condition != DbValues.CONDITION_CLOSED) && (condition != DbValues.CONDITION_UNKNOWN)) {
            visibility = View.VISIBLE;

            String surface = getSurfaceText(isCleared, isFlooded, isResurfaced);
            ((TextView) mRootView.findViewById(R.id.l_rink_surface)).setText(String.format(
                    mResources.getString(R.string.rink_details_surface),
                    surface
            ));
            visibilitySurface = (surface.length() > 0 ? View.VISIBLE : View.INVISIBLE);

            ImageView viewIsCleared = (ImageView) mRootView.findViewById(R.id.l_rink_is_cleared);
            ImageView viewIsFlooded = (ImageView) mRootView.findViewById(R.id.l_rink_is_flooded);
            ImageView viewIsResurfaced = (ImageView) mRootView
                    .findViewById(R.id.l_rink_is_resurfaced);

            viewIsCleared.setImageResource(isCleared == 1 ? R.drawable.ic_surface_cleared_on
                    : R.drawable.ic_surface_cleared_off);
            viewIsFlooded.setImageResource(isFlooded == 1 ? R.drawable.ic_surface_flooded_on
                    : R.drawable.ic_surface_flooded_off);
            viewIsResurfaced
                    .setImageResource(isResurfaced == 1 ? R.drawable.ic_surface_resurfaced_on
                            : R.drawable.ic_surface_resurfaced_off);
            final PatinoiresApp appHelper = ((PatinoiresApp) getActivity().getApplicationContext());
            viewIsCleared.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    int message = (isCleared == 1 ? R.string.rink_details_is_cleared
                            : R.string.rink_details_is_cleared_false);
                    appHelper.showToastText(message, Toast.LENGTH_SHORT);
                }
            });
            viewIsFlooded.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    int message = isFlooded == 1 ? R.string.rink_details_is_flooded
                            : R.string.rink_details_is_flooded_false;
                    appHelper.showToastText(message, Toast.LENGTH_SHORT);
                }
            });
            viewIsResurfaced.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    int message = isResurfaced == 1 ? R.string.rink_details_is_resurfaced
                            : R.string.rink_details_is_resurfaced_false;
                    appHelper.showToastText(message, Toast.LENGTH_SHORT);
                }
            });
        }
        mRootView.findViewById(R.id.l_rink_is_cleared).setVisibility(visibility);
        mRootView.findViewById(R.id.l_rink_is_flooded).setVisibility(visibility);
        mRootView.findViewById(R.id.l_rink_is_resurfaced).setVisibility(visibility);
        mRootView.findViewById(R.id.l_rink_surface).setVisibility(visibilitySurface);

    }

    private void updateTimeInfo(Cursor cursor) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
        int visibilty;
        try {
            visibilty = View.VISIBLE;

            String updatedAt = cursor.getString(RinksQuery.BOROUGH_UPDATED_AT);
            Long millis = dateFormat.parse(updatedAt).getTime();

            updatedAt = (String) DateUtils.getRelativeTimeSpanString(millis,
                    System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE);
            ((TextView) mRootView.findViewById(R.id.l_borough_updated_at))
                    .setText(String.format(
                            mResources.getString(R.string.rink_details_borough_updated_at),
                            updatedAt
                    ));
        } catch (ParseException e) {
            Log.v(TAG, e.toString());
            visibilty = View.GONE;
        }
        mRootView.findViewById(R.id.l_borough_updated_at).setVisibility(visibilty);

        Long userMillis = mAppHelper.getLastUpdateConditions();
        if (userMillis != null) {
            visibilty = View.VISIBLE;

            String userUpdatedAt = (String) DateUtils.getRelativeTimeSpanString(userMillis,
                    System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE);
            ((TextView) mRootView.findViewById(R.id.l_user_updated_at)).setText(String.format(
                    mResources.getString(R.string.rink_details_user_updated_at),
                    userUpdatedAt
            ));
        } else {
            visibilty = View.GONE;
        }
        mRootView.findViewById(R.id.l_user_updated_at).setVisibility(visibilty);
    }

    /**
     * Handle toggling of starred checkbox.
     */
    public void onCheckedChanged(boolean wasFavorite) {
        int message = R.string.toast_favorites_added;

        if (wasFavorite) {
            /**
             * Remove from favorites.
             */
            String[] args = new String[]{
                    Integer.toString(mRinkId)
            };
            mHandler.startDelete(0x10, null, Favorites.CONTENT_URI,
                    FavoritesColumns.FAVORITE_RINK_ID + "=?", args);
            message = R.string.toast_favorites_removed;
        } else {
            /**
             * Add to favorites
             */
            final ContentValues values = new ContentValues();
            values.put(RinksContract.Favorites.FAVORITE_RINK_ID, mRinkId);
            mHandler.startInsert(Favorites.CONTENT_URI, values);
        }

        mAppHelper.showToastText(String.format(mResources.getString(message),
                mRinkName),
                Toast.LENGTH_LONG);
    }

    private int getIdFromUri(Uri uri) {
        int rinkId = -1;

        List<String> pathSegments = uri.getPathSegments();
        if ((pathSegments.size() == 2)
                && (pathSegments.get(0).equals(Const.INTENT_EXTRA_URL_PATH_FR)
                || pathSegments.get(0).equals(Const.INTENT_EXTRA_URL_PATH_EN))) {
            Pattern p = Pattern.compile("^([0-9]+)-");
            Matcher m = p.matcher(pathSegments.get(1));

            if (m.find()) {
                try {
                    rinkId = Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    Log.v(TAG, e.toString());
                }
            }
        }

        return rinkId;
    }

    private String getConditionText(int condition) {
        int conditionIndex;

        switch (condition) {
            case DbValues.CONDITION_EXCELLENT:
                conditionIndex = R.string.prefs_condition_excellent;
                break;
            case DbValues.CONDITION_GOOD:
                conditionIndex = R.string.prefs_condition_good;
                break;
            case DbValues.CONDITION_BAD:
                conditionIndex = R.string.prefs_condition_bad;
                break;
            case DbValues.CONDITION_CLOSED:
                conditionIndex = R.string.prefs_condition_closed;
                break;
            default:
                conditionIndex = R.string.prefs_condition_unknown;
                break;
        }

        return (String) mResources.getText(conditionIndex);
    }

    private String getSurfaceText(int isCleared, int isFlooded, int isResurfaced) {
        int surfaceIndex;

        if (isResurfaced == 1) {
            surfaceIndex = R.string.rink_details_is_resurfaced;
        } else if (isFlooded == 1) {
            surfaceIndex = R.string.rink_details_is_flooded;
        } else if (isCleared == 1) {
            surfaceIndex = R.string.rink_details_is_cleared;
        } else {
            return "";
        }

        return (String) mResources.getText(surfaceIndex);
    }

    private static interface RinksQuery {
        // int _TOKEN = 0x10;

        final String[] PROJECTION = new String[]{
                BaseColumns._ID,
                RinksColumns.RINK_ID,
                RinksColumns.RINK_KIND_ID,
                RinksColumns.RINK_NAME,
                RinksColumns.RINK_DESC_FR,
                RinksColumns.RINK_DESC_EN,
                RinksColumns.RINK_IS_CLEARED,
                RinksColumns.RINK_IS_FLOODED,
                RinksColumns.RINK_IS_RESURFACED,
                RinksColumns.RINK_CONDITION,
                RinksColumns.RINK_IS_FAVORITE,

                ParksColumns.PARK_NAME,
                ParksColumns.PARK_GEO_LAT,
                ParksColumns.PARK_GEO_LNG,
                ParksColumns.PARK_GEO_DISTANCE,
                ParksColumns.PARK_ADDRESS,
                ParksColumns.PARK_PHONE,

                BoroughsColumns.BOROUGH_NAME,
                BoroughsColumns.BOROUGH_UPDATED_AT
        };
        // final int _ID = 0;
        // final int RINK_ID = 1;
        final int RINK_KIND_ID = 2;
        final int RINK_NAME = 3;
        final int RINK_DESC_FR = 4;
        final int RINK_DESC_EN = 5;
        final int RINK_IS_CLEARED = 6;
        final int RINK_IS_FLOODED = 7;
        final int RINK_IS_RESURFACED = 8;
        final int RINK_CONDITION = 9;
        final int RINK_IS_FAVORITE = 10;
        final int PARK_NAME = 11;
        final int PARK_GEO_LAT = 12;
        final int PARK_GEO_LNG = 13;
        final int PARK_GEO_DISTANCE = 14;
        final int PARK_ADDRESS = 15;
        final int PARK_PHONE = 16;
        final int BOROUGH_NAME = 17;
        final int BOROUGH_UPDATED_AT = 18;
    }

}
