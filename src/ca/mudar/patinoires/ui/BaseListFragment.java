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
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.Favorites;
import ca.mudar.patinoires.providers.RinksContract.FavoritesColumns;
import ca.mudar.patinoires.providers.RinksContract.Parks;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.widgets.RinksCursorAdapter;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Const.PrefsValues;
import ca.mudar.patinoires.utils.Helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuInflater;
import android.support.v4.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public abstract class BaseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    protected static final String TAG = "BaseListFragment";

    protected ActivityHelper mActivityHelper;
    protected PatinoiresApp mAppHelper;

    protected Uri mContentUri;

    protected RinksCursorAdapter mAdapter;

    protected Cursor cursor = null;
    protected View rootView;
    protected String mSort;

    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Criteria criteria;
    boolean hasFollowLocationChanges = false;

    public BaseListFragment(Uri contentUri) {
        mContentUri = contentUri;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityHelper = ActivityHelper.createInstance(getActivity());
        mAppHelper = ((PatinoiresApp) getActivity().getApplicationContext());

        locationManager = (LocationManager) getSupportActivity().getSystemService(
                Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        SharedPreferences prefs = getSupportActivity().getSharedPreferences(Const.APP_PREFS_NAME,
                Context.MODE_PRIVATE);
        hasFollowLocationChanges = prefs
                .getBoolean(Const.PrefsNames.FOLLOW_LOCATION_CHANGES, false);

        Location myLocation = mAppHelper.getLocation();

        /**
         * By default, sort by name with AlphabetIndexer.
         */
        mSort = Rinks.DEFAULT_SORT;
        boolean hasIndexer = true;

        if (mAppHelper.getListSort().equals(PrefsValues.LIST_SORT_DISTANCE) && (myLocation != null)) {
            mSort = Parks.PARK_GEO_DISTANCE + " ASC ";
            /**
             * If sorting bydistance, disable the AlphabetIndexer.
             */
            hasIndexer = false;
        }

        setListAdapter(null);

        final String RINK_DESC = (mAppHelper.getLanguage().equals("fr") ? RinksColumns.RINK_DESC_FR
                : RinksColumns.RINK_DESC_EN);

        mAdapter = new RinksCursorAdapter(getActivity(),
                R.layout.fragment_list_item_rinks,
                cursor,
                new String[] {
                        RinksColumns.RINK_NAME, RINK_DESC, ParksColumns.PARK_GEO_DISTANCE,
                        RinksColumns.RINK_ID
                },
                new int[] {
                        R.id.rink_name, R.id.rink_address, R.id.rink_distance
                },
                0, hasIndexer);

        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_list_rinks, null);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(this.getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Cursor c = mAdapter.getCursor();

        String name = c.getString(RinksQuery.RINK_NAME);
        String phone = c.getString(RinksQuery.PARK_PHONE);
        int isFavorite = c.getInt(RinksQuery.RINK_IS_FAVORITE);
        double geoLat = c.getDouble(RinksQuery.PARK_GEO_LAT);
        double geoLng = c.getDouble(RinksQuery.PARK_GEO_LAT);

        menu.setHeaderTitle(name);
        MenuInflater inflater = (MenuInflater) getSupportActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_rink, menu);

        menu.findItem(R.id.favorites_add).setVisible(isFavorite == 0);
        menu.findItem(R.id.favorites_remove).setVisible(isFavorite == 1);
        menu.findItem(R.id.map_view_rink).setVisible((geoLat != 0) && (geoLng != 0));
        menu.findItem(R.id.call_rink).setVisible(phone != null);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = mAdapter.getCursor();
        c.moveToPosition(position);

        int rinkId = c.getInt(RinksQuery.RINK_ID);
        mActivityHelper.goRinkDetails(rinkId);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor c = mAdapter.getCursor();
        ContentResolver contentResolver = getSupportActivity().getContentResolver();

        int rinkId = c.getInt(RinksQuery.RINK_ID);
        Intent intent;

        switch (item.getItemId()) {
            case R.id.map_view_rink:
                double lat = c.getDouble(RinksQuery.PARK_GEO_LAT);
                double lng = c.getDouble(RinksQuery.PARK_GEO_LNG);
                mActivityHelper.goMap(lat, lng);
                
                return true;
            case R.id.favorites_add:
                /**
                 * Add to favorites, and update all ContentResolvers to update
                 * the context menu for this same item.
                 */
                final ContentValues values = new ContentValues();
                values.put(RinksContract.Favorites.FAVORITE_RINK_ID, rinkId);
                contentResolver.insert(Favorites.CONTENT_URI, values);
                mActivityHelper.notifyAllTabs(contentResolver);
                
                return true;
            case R.id.favorites_remove:
                /**
                 * Remove from favorites, and update all ContentResolvers to
                 * update the context menu for this same item.
                 */
                String[] args = new String[] {
                        Integer.toString(rinkId)
                };
                contentResolver.delete(Favorites.CONTENT_URI,
                        FavoritesColumns.FAVORITE_RINK_ID + "=?", args);
                mActivityHelper.notifyAllTabs(contentResolver);
                
                return true;
            case R.id.call_rink:
                final String phone = c.getString(RinksQuery.PARK_PHONE);
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(intent);
                
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        /**
         * Favorite rinks are not filtered by Conditions.
         */
        if (mContentUri == Rinks.CONTENT_FAVORITES_URI) {
            filter = null;
        }

        return new CursorLoader(getSupportActivity().getApplicationContext(), mContentUri,
                RinksQuery.RINKS_SUMMARY_PROJECTION, filter, null, mSort);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        if (!data.moveToFirst()) {
            /**
             * In the XML layout, `android.R.id.empty` is the ProgressBar. If
             * the list is empty (no rinks), hide the ProgressBar and display
             * `rinks_empty_list` TextView instead.
             */
            rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
            getListView().setEmptyView(rootView.findViewById(R.id.rinks_empty_list));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * The location listener. Doesn't do anything but listening, DB updates are
     * handled by the app's passive listener.
     */
    protected class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    public static interface RinksQuery {
        // int _TOKEN = 0x10;

        final String[] RINKS_SUMMARY_PROJECTION = new String[] {
                BaseColumns._ID,
                RinksColumns.RINK_ID,
                RinksColumns.RINK_KIND_ID,
                RinksColumns.RINK_NAME,
                RinksColumns.RINK_DESC_FR,
                RinksColumns.RINK_DESC_EN,
                RinksColumns.RINK_CONDITION,
                RinksColumns.RINK_IS_FAVORITE,
                ParksColumns.PARK_GEO_LAT,
                ParksColumns.PARK_GEO_LNG,
                ParksColumns.PARK_GEO_DISTANCE,
                ParksColumns.PARK_PHONE
        };
        final int _ID = 0;
        final int RINK_ID = 1;
        final int RINK_KIND_ID = 2;
        final int RINK_NAME = 3;
        final int RINK_DESC_FR = 4;
        final int RINK_DESC_EN = 5;
        final int RINK_CONDITION = 6;
        final int RINK_IS_FAVORITE = 7;
        final int PARK_GEO_LAT = 8;
        final int PARK_GEO_LNG = 9;
        final int PARK_GEO_DISTANCE = 10;
        final int PARK_PHONE = 11;
    }

}
