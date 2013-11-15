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

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;

public class MapFragment extends SupportMapFragment implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    protected static final String TAG = "MapFragment";
    protected static final int INDEX_OVERLAY_MY_LOCATION = 0x0;
    protected static final int INDEX_OVERLAY_PLACEMARKS = 0x1;
    // protected static final float ZOOM_DEFAULT = 12f;
    private static final float ZOOM_NEAR = 17f;
    private static final float ZOOM_MIN = 16f;
    private static final float HUE_MARKER = 94f;
    private static final float HUE_MARKER_STARRED = BitmapDescriptorFactory.HUE_YELLOW;
    private static final float DISTANCE_MARKER_HINT = 50f;
    protected OnMyLocationChangedListener mListener;
    protected LocationManager mLocationManager;
    ActivityHelper activityHelper;
    private GoogleMap mMap;
        private LocationSource.OnLocationChangedListener onLocationChangedListener;
    private Location initLocation = null;
    private Location mMapCenter = null;
    private LatLng screenCenter = null;
    private Marker clickedMarker = null;
    private Marker searchedMarker = null;
    private boolean hasHintMarker = true;
    private PatinoiresApp mAppHelper;
    private MenuItem searchItem;
    private String postalCode;

    /**
     * Attach a listener.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnMyLocationChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMyLocationChangedListener");
        }
    }

    /**
     * Create map and initialize
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activityHelper = ActivityHelper.createInstance(getActivity());
        mAppHelper = (PatinoiresApp) getActivity().getApplicationContext();

        setUpMapIfNeeded();

        mLocationManager = (LocationManager) getActivity().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);

//        initMap();
    }

    /**
     * Enable user location (GPS) updates on map display.
     */
    @Override
    public void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (!checkReady()) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = getMap();
            // Check if we were successful in obtaining the map.
            if (checkReady()) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {

        mMap.setMyLocationEnabled(true);
//        mMap.setMapType(MAP_TYPE_NORMAL);

        mMap.setLocationSource(null);

//        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter(getActivity()));

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        // Required for the windowActionBarOverlay / transparent actionbar
//        mMap.setPadding(0, getPaddingTop(), 0, 0);

//        UiSettings uiSettings = mMap.getUiSettings();

//        uiSettings.setZoomControlsEnabled(true);
//        uiSettings.setMyLocationButtonEnabled(true);
//        uiSettings.setAllGesturesEnabled(true);
//        uiSettings.setCompassEnabled(true);
    }

    private boolean checkReady() {
        if (mMap == null) {
            mAppHelper.showToastText(R.string.toast_map_not_ready, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    /**
     * Set new map center.
     *
     * @param mapCenter
     */
    protected void animateToPoint(Location mapCenter) {
        Log.v(TAG, "animateToPoint");
        if (mMap == null) {
            Log.v(TAG, "map is null!");
            return;
        }
        if (mapCenter != null) {
            Log.v(TAG, "ZOOM_NEAR");
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    mapCenter.getLatitude(), mapCenter.getLongitude()), ZOOM_NEAR));
        } else {
            Log.v(TAG, "ZOOM_DEFAULT + initialAnimateToPoint");
            mMap.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_NEAR));
            initialAnimateToPoint();
        }
    }

    /**
     * Initial map center animation on detected user location. If user is more
     * than minimum-distance from the city, center the map on Downtown. Also
     * defines the zoom.
     */
    protected void initialAnimateToPoint() {
        Log.v(TAG, "initialAnimateToPoint");
        final List<String> enabledProviders = mLocationManager.getProviders(true);

        double coordinates[] = Const.MAPS_DEFAULT_COORDINATES;
        final double lat = coordinates[0];
        final double lng = coordinates[1];

        final Location userLocation = mAppHelper.getLocation();
        if (userLocation != null) {
            /**
             * Center on app's user location.
             */
            Log.v(TAG, "initialAnimateToPoint lat = " +
            userLocation.getLatitude() + ". Lon = "
            + userLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(
                    userLocation.getLatitude(), userLocation.getLongitude())));
        } else {
            /**
             * Center on Downtown.
             */
            Log.v(TAG, "initialAnimateToPoint. Center on Downtown");
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        }

        if (mMapCenter != null) {
            /**
             * The AppHelper knows the user location from a previous query, so
             * use the saved value.
             */
            Log.v(TAG, "initialAnimateToPoint. mMapCenter != null");
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(
                    mMapCenter.getLatitude(), mMapCenter.getLongitude())));
        }
    }

    /**
     * Setter for the MapCenter Location. Centers map on the new location
     *
     * @param mapCenter The new location
     */
    public void setMapCenter(Location mapCenter) {
        Log.v(TAG, "setMapCenter");
//        Log.v(TAG, "lat = " + mapCenter.getLatitude() + " lng = " +mapCenter.getLongitude());
        initLocation = mapCenter;
        animateToPoint(mapCenter);
    }

    /**
     * Sets the map center on the user real location with a near zoom.
     */
    public void resetMapCenter() {
        searchedMarker = null;
        if (!mMap.isMyLocationEnabled()) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setLocationSource(null);
        setMapCenterZoomed(mAppHelper.getLocation());
    }

    /**
     * Sets the map center on the location with a near zoom. Used for Address
     * Search and Tab re-selection.
     */
    private void setMapCenterZoomed(Location mapCenter) {
        // mMapController.setZoom(ZOOM_NEAR);
        setMapCenter(mapCenter);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


    /**
     * Container Activity must implement this interface to receive the list item
     * clicks.
     */
    public interface OnMyLocationChangedListener {
        public void OnMyLocationChanged(Location location);
    }

}
