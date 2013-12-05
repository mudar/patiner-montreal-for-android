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
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.googlemap.LocationAwarenessListener;
import ca.mudar.patinoires.ui.fragment.MapFragment;
import ca.mudar.patinoires.utils.ConnectionHelper;


public class MapActivity extends BaseActivity {
    private static final String TAG = "MapActivity";
    private MapFragment mMapFragment;
    /**
     * Location
     */
    private Location initLocation;
    private boolean isCenterOnMyLocation;
    /**
     * Google Playservices
     */
    private LocationAwarenessListener mLocationAwarenessListener;
    private boolean isPlayservicesOutdated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        isPlayservicesOutdated = (GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS);
        if (isPlayservicesOutdated) {

            setContentView(R.layout.activity_playservices_update);
            setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        } else {

            setContentView(R.layout.activity_map);
            setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
        }

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

        mLocationAwarenessListener = new LocationAwarenessListener(this);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStart() {
        super.onStart();

        mLocationAwarenessListener.startConnection();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check Playservices status
        if (isPlayservicesOutdated) {
            if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()) == ConnectionResult.SUCCESS) {
                // Playservice updated, display message and restart activity
                ((PatinoiresApp) getApplicationContext()).showToastText(R.string.toast_playservices_restart, Toast.LENGTH_LONG);
                final Intent intent = getIntent();
                this.finish();
                startActivity(intent);
            }
            return;
        }

        if (!ConnectionHelper.hasConnection(this)) {
            ConnectionHelper.showDialogNoConnection(this);
        }

        FragmentManager fm = getSupportFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.fragment_map);
        if (initLocation != null || isCenterOnMyLocation) {
            mMapFragment.setMapCenter(initLocation);
        }
    }

    @Override
    public void onStop() {
        initLocation = null;
        isCenterOnMyLocation = false;

        mLocationAwarenessListener.stopConnection();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().setBackgroundDrawable(null);
        System.gc();
    }

    /**
     * Handle hardware search/menu keys to toggle the SearchView collapse/expand
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (mMapFragment != null) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                mMapFragment.searchToggle(false);
            } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                mMapFragment.searchToggle(true);
            }
        }

        return super.onKeyUp(keyCode, event);
    }
}
