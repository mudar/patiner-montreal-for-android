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

package ca.mudar.patinoires.ui.view;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.googlemap.GeoHelper;
import ca.mudar.patinoires.utils.ConnectionHelper;
import ca.mudar.patinoires.utils.SearchMessageHandler;

public class SearchViewQueryListener implements
        SearchView.OnQueryTextListener,
        SearchMessageHandler.OnMessageHandledListener,
        Runnable {
    protected static final String TAG = "SearchViewQueryListener";
    private static final String EXCEPTION_MSG_SUFFIX = "(Android system error)";
    private static final int QUERY_MIN_LENGTH = 2;
    private final Activity mActivity;
    private final Handler handler;
    private final OnAddressFoundListener mListener;
    private MenuItem mSearchMenuItem;
    private String mSearchQuery;

    public SearchViewQueryListener(Activity activity, OnAddressFoundListener listener) {
        mActivity = activity;
        mListener = listener;

        handler = new SearchMessageHandler(this);
    }

    /**
     * Implementation of SearchView.OnQueryTextListener. Handle the Address
     * Search query
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;

        if ((mSearchQuery == null) || (mSearchQuery.length() < QUERY_MIN_LENGTH)) {
            return false;
        }

        searchToggle(false);
        if (ConnectionHelper.hasConnection(mActivity)) {
            showSearchProcessing();
        } else {
            ((PatinoiresApp) mActivity.getApplicationContext())
                    .showToastText(R.string.toast_search_network_connection_error, Toast.LENGTH_LONG);
        }

        return true;
    }

    /**
     * SearchView.OnQueryTextListener
     */
    @Override
    public boolean onQueryTextChange(String query) {
        return false;
    }

    /**
     * Implementation of SearchMessageHandler.OnMessageHandledListener. Handle
     * the runnable thread results. This hides the indeterminate progress bar
     * then centers map on found location or displays error message.
     */
    @Override
    public void OnMessageHandled(Message msg) {
        if (mActivity == null) {
            return;
        }

        ((ActionBarActivity) mActivity).setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        if (mListener == null) {
            return;
        }

        final Bundle b = msg.getData();
        if (b.getInt(Const.KEY_BUNDLE_SEARCH_ADDRESS) == Const.BUNDLE_SEARCH_ADDRESS_SUCCESS) {
            /**
             * Address is found, center map on location.
             */
            final Location location = new Location(Const.LOCATION_PROVIDER_SEARCH);
            location.setLatitude(b.getDouble(Const.KEY_BUNDLE_ADDRESS_LAT));
            location.setLongitude(b.getDouble(Const.KEY_BUNDLE_ADDRESS_LNG));
            final String desc = b.getString(Const.KEY_BUNDLE_ADDRESS_DESC);

            mListener.setMapCenter(location);

            /**
             * Add marker for found location
             */
// TODO: display infoWindow for map-center Park/Rink, and verify use of searchedMarker
            final Marker searchedMarker = mListener.addMapMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .title(desc)
                    .snippet(null)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).visible(true));
            searchedMarker.showInfoWindow();
        } else if ((b.getInt(Const.KEY_BUNDLE_SEARCH_ADDRESS) == Const.BUNDLE_SEARCH_SERVICE_ERROR)
                && (b.getString(Const.KEY_BUNDLE_SERVICE_ERROR_MSG) != null)) {
            ((PatinoiresApp) mActivity.getApplicationContext()).showToastText(
                    b.getString(Const.KEY_BUNDLE_SERVICE_ERROR_MSG) + Const.LINE_SEPARATOR + EXCEPTION_MSG_SUFFIX,
                    Toast.LENGTH_LONG);
            mSearchMenuItem.setVisible(false);
        } else {
            /**
             * Address not found! Display error message.
             */
            final String errorMsg = String.format(
                    mActivity.getResources().getString(R.string.toast_search_error),
                    mSearchQuery);
            ((PatinoiresApp) mActivity.getApplicationContext()).showToastText(
                    errorMsg, Toast.LENGTH_LONG);
        }
    }

    /**
     * Implementation of Runnable. This runnable thread gets the Geocode search
     * value in the background. Results are sent to the handler.
     */
    @Override
    public void run() {
        final Message msg = handler.obtainMessage();
        final Bundle b = new Bundle();

        try {
            /**
             * Geocode search. Takes time and not very reliable!
             */
            final Address address = GeoHelper.findAddressFromName(mActivity, mSearchQuery);

            if (address == null) {
                /**
                 * Send error message to handler.
                 */
                b.putInt(Const.KEY_BUNDLE_SEARCH_ADDRESS, Const.BUNDLE_SEARCH_ADDRESS_ERROR);
            } else {
                /**
                 * Send success message to handler with the found geocoordinates.
                 */
                b.putInt(Const.KEY_BUNDLE_SEARCH_ADDRESS, Const.BUNDLE_SEARCH_ADDRESS_SUCCESS);
                b.putDouble(Const.KEY_BUNDLE_ADDRESS_LAT, address.getLatitude());
                b.putDouble(Const.KEY_BUNDLE_ADDRESS_LNG, address.getLongitude());
                b.putString(Const.KEY_BUNDLE_ADDRESS_DESC, address.getAddressLine(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
            b.putInt(Const.KEY_BUNDLE_SEARCH_ADDRESS, Const.BUNDLE_SEARCH_SERVICE_ERROR);
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                b.putString(Const.KEY_BUNDLE_SERVICE_ERROR_MSG, e.getMessage());
            }
        } finally {
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }

    public void setSearchMenuItem(MenuItem item) {
        mSearchMenuItem = item;

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        searchView.setQueryHint(mActivity.getResources().getString(R.string.search_hint_actionbar));
        searchView.setOnQueryTextListener(this);
        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));

        // Collapse when focus lost
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                searchToggle(hasFocus);
            }
        });
    }

    /**
     * Show the Indeterminate ProgressBar and start the Geocode search thread.
     */
    private void showSearchProcessing() {
        ((ActionBarActivity) mActivity).setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);

        final Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Toggle collapse/expand the SearchView.
     *
     * @param isDisplayed
     */
    public void searchToggle(boolean isDisplayed) {
        if (mSearchMenuItem != null) {
            if (isDisplayed) {
                MenuItemCompat.expandActionView(mSearchMenuItem);
            } else {
                MenuItemCompat.collapseActionView(mSearchMenuItem);
            }
        }
    }

    /**
     * ListFragment must implement the following interface
     */
    public interface OnAddressFoundListener {
        public void setMapCenter(Location location);

        public Marker addMapMarker(MarkerOptions options);
    }

}
