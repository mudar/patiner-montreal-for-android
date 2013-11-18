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

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.ui.fragment.MapFragment;
import ca.mudar.patinoires.utils.ConnectionHelper;


public class MapActivity extends BaseActivity implements MapFragment.OnMyLocationChangedListener {
    protected static final String TAG = "MapActivity";
    private static final String SEND_INTENT_TYPE = "text/plain";

    private Location initLocation;
    private boolean isCenterOnMyLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        initLocation = null;

        double latitude = getIntent().getDoubleExtra(Const.INTENT_EXTRA_GEO_LAT, Double.MIN_VALUE);
        double longitude = getIntent().getDoubleExtra(Const.INTENT_EXTRA_GEO_LNG, Double.MIN_VALUE);

        if (Double.compare(latitude, Double.MIN_VALUE) != 0
                && Double.compare(latitude, Double.MIN_VALUE) != 0) {
            initLocation = new Location(Const.LOCATION_PROVIDER_INTENT);

            initLocation.setLatitude(latitude);
            initLocation.setLongitude(longitude);

            isCenterOnMyLocation = false;
        } else {
            isCenterOnMyLocation = true;
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
        initLocation = null;

        double latitude = intent.getDoubleExtra(Const.INTENT_EXTRA_GEO_LAT, Double.MIN_VALUE);
        double longitude = intent.getDoubleExtra(Const.INTENT_EXTRA_GEO_LNG, Double.MIN_VALUE);

        if (Double.compare(latitude, Double.MIN_VALUE) != 0
                && Double.compare(latitude, Double.MIN_VALUE) != 0) {
            initLocation = new Location(Const.LOCATION_PROVIDER_INTENT);

            initLocation.setLatitude(latitude);
            initLocation.setLongitude(longitude);

            isCenterOnMyLocation = false;
        } else {
            isCenterOnMyLocation = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!ConnectionHelper.hasConnection(this)) {
            ConnectionHelper.showDialogNoConnection(this);
        }

        if (initLocation != null || isCenterOnMyLocation) {
            FragmentManager fm = getSupportFragmentManager();
            MapFragment fragmentMap = (MapFragment) fm.findFragmentById(R.id.fragment_map);
            fragmentMap.setMapCenter(initLocation);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        initLocation = null;
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

        return true;
    }

    /**
     * Implement the fragmentMap listener interface, to get the user's location,
     * send it to the AppHelper and enable the My Location item in the menu.
     */
    @Override
    public void OnMyLocationChanged(final Location location) {
        /**
         * Following code allows the background listener to modify the UI's
         * menu.
         */
        runOnUiThread(new Runnable() {
            public void run() {
                // TODO: verify that new location is sent to service & favorites
                // fragment
                ((PatinoiresApp) getApplicationContext())
                        .setLocation(location);
                // invalidateOptionsMenu();
            }
        });
    }

}
