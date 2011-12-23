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
import ca.mudar.patinoires.ui.MapFragment.OnMyLocationChangedListener;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.ConnectionHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Helper;

import com.google.android.maps.GeoPoint;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentMapActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;

public class MapActivity extends FragmentMapActivity implements OnMyLocationChangedListener {
    protected static final String TAG = "MapActivity";

    // protected MenuItem btnActionbarToggleList;
    private String postalCode;
    private ProgressDialog pd;
    private PatinoiresApp mAppHelper;
    private GeoPoint initGeoPoint;
    private boolean isCenterOnMyLocation;

    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppHelper = (PatinoiresApp) getApplicationContext();
        mAppHelper.updateUiLanguage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(true);
        }

        setContentView(R.layout.activity_map);

        initGeoPoint = null;

        Integer latitude = getIntent().getIntExtra(Const.INTENT_EXTRA_GEO_LAT,
                Integer.MIN_VALUE);
        Integer longitude = getIntent().getIntExtra(
                Const.INTENT_EXTRA_GEO_LNG, Integer.MIN_VALUE);

        if (!latitude.equals(Integer.MIN_VALUE) && !longitude.equals(Integer.MIN_VALUE)) {
            initGeoPoint = new GeoPoint(latitude, longitude);
            isCenterOnMyLocation = false;
        }
        else {
            isCenterOnMyLocation = true;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        initGeoPoint = null;

        Integer latitude = intent.getIntExtra(Const.INTENT_EXTRA_GEO_LAT,
                Integer.MIN_VALUE);
        Integer longitude = intent.getIntExtra(
                Const.INTENT_EXTRA_GEO_LNG, Integer.MIN_VALUE);

        if (!latitude.equals(Integer.MIN_VALUE) && !longitude.equals(Integer.MIN_VALUE)) {
            initGeoPoint = new GeoPoint(latitude, longitude);
            isCenterOnMyLocation = false;
        }
        else {
            isCenterOnMyLocation = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!ConnectionHelper.hasConnection(this)) {
            ConnectionHelper.showDialogNoConnection(this);
        }

        View root = findViewById(R.id.map_root_landscape);
        boolean isTablet = (root != null);

        if (initGeoPoint != null || isCenterOnMyLocation) {
            FragmentManager fm = getSupportFragmentManager();
            MapFragment fragmentMap = (MapFragment)
                    fm.findFragmentById(R.id.fragment_map);
            fragmentMap.setMapCenter(initGeoPoint);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        initGeoPoint = null;
        isCenterOnMyLocation = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().setBackgroundDrawable(null);
        System.gc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);

        /**
         * Disable the My Location button if the user location is not known yet.
         */
        if (mAppHelper.getLocation() == null) {
            menu.findItem(R.id.menu_map_mylocation).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_map_mylocation) {
            Location location = mAppHelper.getLocation();
            if (location != null) {
                FragmentManager fm = getSupportFragmentManager();
                MapFragment fragmentMap = (MapFragment) fm.findFragmentById(R.id.fragment_map);
                GeoPoint geoPoint = Helper.locationToGeoPoint(location);
                fragmentMap.setMapCenterZoomed(geoPoint);
            }
            return true;
        }

        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Implement the fragmentMap listener interface, to get the user's location,
     * send it to the AppHelper and enable the My Location item in the menu.
     */
    @Override
    public void OnMyLocationChanged(final GeoPoint geoPoint) {
        /**
         * Following code allows the background listener to modify the UI's
         * menu.
         */
        runOnUiThread(new Runnable() {
            public void run() {
                ((PatinoiresApp) getApplicationContext())
                        .setLocation(Helper.geoPointToLocation(geoPoint));
                invalidateOptionsMenu();
            }
        });

    }
}
