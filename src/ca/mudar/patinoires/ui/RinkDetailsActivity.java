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

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.data.PatinoiresDbAdapter;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.BoroughsColumns;
import ca.mudar.patinoires.providers.RinksContract.Favorites;
import ca.mudar.patinoires.providers.RinksContract.FavoritesColumns;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Const.DbValues;
import ca.mudar.patinoires.utils.NotifyingAsyncQueryHandler;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class RinkDetailsActivity extends FragmentActivity {

    static int mRinkId;
    static Uri mRinkUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PatinoiresApp) getApplicationContext()).updateUiLanguage();

        mRinkId = getIntent().getIntExtra(Const.INTENT_EXTRA_ID_RINK, -1);

        mRinkUri = RinksContract.Rinks.buildRinkUri(Integer.toString(mRinkId));

        if (mRinkId == -1) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            RinkDetailsFragment about = new RinkDetailsFragment();
            fm.beginTransaction().add(android.R.id.content, about).commit();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rink_details_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * AboutFragment
     */
    public static class RinkDetailsFragment extends Fragment implements
            NotifyingAsyncQueryHandler.AsyncQueryListener {
        private static final String TAG = "RinkDetailsFragment";

        protected ActivityHelper mActivityHelper;
        protected PatinoiresApp mAppHelper;

        // protected static final int QUERY_TOKEN = 0x2;

        protected NotifyingAsyncQueryHandler mHandler;

        protected View mRootView;
        protected int mIsFavorite;
        protected String mRinkName;
        protected String sGeoCoordinates;

        public static RinkDetailsFragment newInstance() {
            RinkDetailsFragment rink = new RinkDetailsFragment();

            return rink;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            mRinkName = "";

            mActivityHelper = ActivityHelper.createInstance(getActivity());
            mAppHelper = ((PatinoiresApp) getActivity().getApplicationContext());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            if (container == null) {
                return null;
            }

            mIsFavorite = 0;

            mRootView = inflater.inflate(R.layout.fragment_rinks_details, container, false);

            mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
            mHandler.startQuery(RinksQuery._TOKEN, mRinkUri, RinksQuery.PROJECTION);

            return mRootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

            inflater.inflate(R.menu.menu_rink_details_fragment, menu);

            int resIcon = (mIsFavorite == 1 ? R.drawable.ic_actionbar_favorite_on
                    : R.drawable.ic_actionbar_favorite_off);

            menu.findItem(R.id.menu_favorites_toggle).setIcon(resIcon);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            if (item.getItemId() == R.id.menu_favorites_toggle) {
                onCheckedChanged(mIsFavorite == 1 ? true : false);

                mIsFavorite = (mIsFavorite == 0 ? 1 : 0); // Toggle value
                getSupportActivity().invalidateOptionsMenu();
                return true;
            }
            else if (item.getItemId() == R.id.menu_gmaps_directions) {
                if (sGeoCoordinates != null) {
                    /**
                     * Get directions using Intents.
                     */
                    Location userLocation = mAppHelper.getLocation();
                    String sAddr = Double.toString(userLocation.getLatitude()) + ","
                            + Double.toString(userLocation.getLongitude());
                    String urlGmaps = String.format(Const.URL_GMAPS_DIRECTIONS, sAddr,
                            sGeoCoordinates);

                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse(urlGmaps));
                    startActivity(Intent.createChooser(intent,
                            getResources().getString(R.string.dialog_title_maps_chooser)));
                }
            }

            return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
        }

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (getSupportActivity() == null) {
                return;
            }

            if (token == RinksQuery._TOKEN) {
                try {
                    if (!cursor.moveToFirst()) {
                        return;
                    }
                    updateRinkInfo(cursor);
                    updateParkInfo(cursor);
                    updateBoroughInfo(cursor);
                    getSupportActivity().invalidateOptionsMenu();

                } finally {
                    cursor.close();
                }
            }
            else {
                cursor.close();
            }
        }

        private void updateRinkInfo(Cursor cursor) {
            String lang = mAppHelper.getLanguage();

            mRinkName = cursor.getString(RinksQuery.RINK_NAME);
            String desc = cursor.getString(lang.equals("fr") ? RinksQuery.RINK_DESC_FR
                    : RinksQuery.RINK_DESC_EN);
            int kindId = cursor.getInt(RinksQuery.RINK_KIND_ID);
            final int isCleared = cursor.getInt(RinksQuery.RINK_IS_CLEARED);
            final int isFlooded = cursor.getInt(RinksQuery.RINK_IS_FLOODED);
            final int isResurfaced = cursor.getInt(RinksQuery.RINK_IS_RESURFACED);
            int condition = cursor.getInt(RinksQuery.RINK_CONDITION);
            mIsFavorite = cursor.getInt(RinksQuery.RINK_IS_FAVORITE);
            sGeoCoordinates = cursor.getString(RinksQuery.PARK_GEO_LAT) + ","
                    + cursor.getString(RinksQuery.PARK_GEO_LNG);

            ((TextView) mRootView.findViewById(R.id.l_rink_name)).setText(mRinkName);
            ((TextView) mRootView.findViewById(R.id.l_rink_desc)).setText(desc);
            ((ImageView) mRootView.findViewById(R.id.l_rink_kind_id))
                    .setImageResource(getImageResource(kindId, condition));

            TextView viewRinkCondition = ((TextView) mRootView.findViewById(R.id.l_rink_condition));
            viewRinkCondition.append(getConditionText(condition));

            String surface = getSurfaceText(isCleared, isFlooded, isResurfaced);
            ((TextView) mRootView.findViewById(R.id.l_rink_surface)).append(surface);

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

        private void updateParkInfo(Cursor cursor) {
            String prefixParcName = getResources().getString(R.string.rink_details_park_name);
            String name = String.format(cursor.getString(RinksQuery.PARK_NAME), prefixParcName);
            String address = cursor.getString(RinksQuery.PARK_ADDRESS);
            final String phone = cursor.getString(RinksQuery.PARK_PHONE);

            ((TextView) mRootView.findViewById(R.id.l_park_name)).setText(name);
            ((TextView) mRootView.findViewById(R.id.l_park_address)).setText(address);

            if (phone == null) {
                mRootView.findViewById(R.id.l_park_phone).setVisibility(View.INVISIBLE);
                mRootView.findViewById(R.id.l_rink_call).setVisibility(View.GONE);

                ((TextView) mRootView.findViewById(R.id.l_park_phone)).setHeight(0);
            }
            else {
                ((TextView) mRootView.findViewById(R.id.l_park_phone)).append(phone);

                ImageButton dialerButton = (ImageButton) mRootView.findViewById(R.id.l_rink_call);
                dialerButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        startActivity(intent);
                    }
                });
            }

        }

        private void updateBoroughInfo(Cursor cursor) {

            String name = cursor.getString(RinksQuery.BOROUGH_NAME);
            String remarks = cursor.getString(RinksQuery.BOROUGH_REMARKS);
            String updatedAt = cursor.getString(RinksQuery.BOROUGH_UPDATED_AT);

            ((TextView) mRootView.findViewById(R.id.l_borough_name)).setText(name);

            if ((remarks == null) || (remarks.trim().length() == 0)) {
                ((TextView) mRootView.findViewById(R.id.l_borough_remarks))
                        .setVisibility(View.INVISIBLE);
                ((TextView) mRootView.findViewById(R.id.l_borough_remarks)).setHeight(0);
            }
            else {
                ((TextView) mRootView.findViewById(R.id.l_borough_remarks)).setText(remarks);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
                Long millis = dateFormat.parse(updatedAt).getTime();
                updatedAt = (String) DateUtils.getRelativeTimeSpanString(millis,
                        System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE);
            } catch (java.text.ParseException e) {
            }
            ((TextView) mRootView.findViewById(R.id.l_borough_updated_at)).append(updatedAt);

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
                String[] args = new String[] {
                        Integer.toString(mRinkId)
                };
                mHandler.startDelete(0x10, null, Favorites.CONTENT_URI,
                        FavoritesColumns.FAVORITE_RINK_ID + "=?", args);
                message = R.string.toast_favorites_removed;
            }
            else {
                /**
                 * Add to favorites
                 */
                final ContentValues values = new ContentValues();
                values.put(RinksContract.Favorites.FAVORITE_RINK_ID, mRinkId);
                mHandler.startInsert(Favorites.CONTENT_URI, values);
            }
            
            mAppHelper.showToastText(String.format(getResources().getString(message), mRinkName),
                    Toast.LENGTH_LONG);
        }

        private int getImageResource(int kindId, int condition) {
            int imageResource = R.drawable.ic_rink_skating_0;

            if (kindId == DbValues.KIND_PSE) {
                switch (condition) {
                    case 0:
                        imageResource = R.drawable.ic_rink_hockey_0;
                        break;
                    case 1:
                        imageResource = R.drawable.ic_rink_hockey_1;
                        break;
                    case 2:
                        imageResource = R.drawable.ic_rink_hockey_2;
                        break;
                    default:
                        imageResource = R.drawable.ic_rink_hockey_3;
                        break;
                }
            }
            else {
                switch (condition) {
                    case 0:
                        imageResource = R.drawable.ic_rink_skating_0;
                        break;
                    case 1:
                        imageResource = R.drawable.ic_rink_skating_1;
                        break;
                    case 2:
                        imageResource = R.drawable.ic_rink_skating_2;
                        break;
                    default:
                        imageResource = R.drawable.ic_rink_skating_3;
                        break;
                }
            }

            return imageResource;
        }

        private String getConditionText(int condition) {
            int conditionIndex;

            switch (condition) {
                case PatinoiresDbAdapter.CONDITION_EXCELLENT_INDEX:
                    conditionIndex = R.string.prefs_condition_excellent;
                    break;
                case PatinoiresDbAdapter.CONDITION_GOOD_INDEX:
                    conditionIndex = R.string.prefs_condition_good;
                    break;
                case PatinoiresDbAdapter.CONDITION_BAD_INDEX:
                    conditionIndex = R.string.prefs_condition_bad;
                    break;
                default:
                    conditionIndex = R.string.prefs_condition_closed;
                    break;
            }

            return (String) getResources().getText(conditionIndex);
        }

        private String getSurfaceText(int isCleared, int isFlooded, int isResurfaced) {
            int surfaceIndex;

            if (isResurfaced == 1) {
                surfaceIndex = R.string.rink_details_is_resurfaced;
            }
            else if (isFlooded == 1) {
                surfaceIndex = R.string.rink_details_is_flooded;
            }
            else if (isCleared == 1) {
                surfaceIndex = R.string.rink_details_is_cleared;
            }
            else {
                return "";
            }

            return (String) getResources().getText(surfaceIndex);
        }

        private static interface RinksQuery {
            int _TOKEN = 0x10;

            final String[] PROJECTION = new String[] {
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
                    ParksColumns.PARK_IS_CHALET,
                    ParksColumns.PARK_IS_CARAVAN,

                    BoroughsColumns.BOROUGH_NAME,
                    BoroughsColumns.BOROUGH_REMARKS,
                    BoroughsColumns.BOROUGH_UPDATED_AT
            };
            final int _ID = 0;
            final int RINK_ID = 1;
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
            // final int PARK_IS_CHALET = 17;
            // final int PARK_IS_CARAVAN = 18;

            final int BOROUGH_NAME = 19;
            final int BOROUGH_REMARKS = 20;
            final int BOROUGH_UPDATED_AT = 21;
        }
    }
}
