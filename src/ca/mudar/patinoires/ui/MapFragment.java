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
import ca.mudar.patinoires.providers.RinksContract.Parks;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.ui.widgets.MyItemizedOverlay;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Helper;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {
    protected static final String TAG = "MapFragment";

    protected static int INDEX_OVERLAY_MY_LOCATION = 0x0;
    protected static int INDEX_OVERLAY_PLACEMARKS = 0x1;

    protected static int ZOOM_DEFAULT = 14;
    protected static int ZOOM_NEAR = 17;

    protected PatinoiresApp mAppHelper;
    protected ActivityHelper mActivityHelper;

    protected MapView mMapView;
    protected MyLocationOverlay mLocationOverlay;
    protected MapController mMapController;
    protected LocationManager mLocationManager;
    protected OnMyLocationChangedListener mListener;

    protected GeoPoint mMapCenter = null;

    /**
     * Container Activity must implement this interface to receive the list item
     * clicks.
     */
    public interface OnMyLocationChangedListener {
        public void OnMyLocationChanged(GeoPoint geoPoint);
    }

    /**
     * Attach a listener.
     */
    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnMyLocationChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMyLocationChangedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setHasOptionsMenu(true);

        mActivityHelper = ActivityHelper.createInstance(getActivity());
        mAppHelper = ((PatinoiresApp) getSupportActivity().getApplicationContext());
    }

    /**
     * Create the map view and restore saved instance (if any). {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /**
         * Restore map center and zoom
         */
        int savedZoom = ZOOM_DEFAULT;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Const.KEY_INSTANCE_COORDS)) {
                int[] coords = savedInstanceState.getIntArray(Const.KEY_INSTANCE_COORDS);
                mMapCenter = new GeoPoint(coords[0], coords[1]);
            }
            if (savedInstanceState.containsKey(Const.KEY_INSTANCE_ZOOM)) {
                savedZoom = savedInstanceState.getInt(Const.KEY_INSTANCE_ZOOM);
            }
        }

        View root = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = (MapView) root.findViewById(R.id.map_view);

        mMapView.setBuiltInZoomControls(true);

        mMapController = mMapView.getController();
        mMapController.setZoom(savedZoom);

        mLocationManager = (LocationManager) getActivity().getApplicationContext()
                .getSystemService(MapActivity.LOCATION_SERVICE);

        initMap();

        return root;
    }

    /**
     * Enable user location (GPS) updates on map display. {@inheritDoc}
     */
    @Override
    public void onResume() {
        mLocationOverlay.enableMyLocation();

        super.onResume();
    }

    /**
     * Disable user location (GPS) updates on map hide. {@inheritDoc}
     */
    @Override
    public void onPause() {
        mLocationOverlay.disableMyLocation();
        super.onPause();
    }

    /**
     * Save map center and zoom. {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        GeoPoint center = mMapView.getMapCenter();
        int[] coords = {
                center.getLatitudeE6(), center.getLongitudeE6()
        };

        outState.putIntArray(Const.KEY_INSTANCE_COORDS, coords);
        outState.putInt(Const.KEY_INSTANCE_ZOOM, mMapView.getZoomLevel());

        super.onSaveInstanceState(outState);
    }

    /**
     * Disable/Enable user location (GPS) updates on map hide/display.
     * {@inheritDoc}
     */
    // @Override
    // public void onHiddenChanged(boolean hidden) {
    // if (hidden) {
    // mLocationOverlay.disableMyLocation();
    // }
    // else {
    // mLocationOverlay.enableMyLocation();
    // }
    // super.onHiddenChanged(hidden);
    // }

    // @Override
    // public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // }

    /**
     * Initialize Map: centre and load placemarks
     */
    protected void initMap() {
        mLocationOverlay = new MyLocationOverlay(getActivity().getApplicationContext(), mMapView);
        mLocationOverlay.enableCompass();
        mLocationOverlay.enableMyLocation();
        mMapView.getOverlays().add(INDEX_OVERLAY_MY_LOCATION, mLocationOverlay);

        ArrayList<MapMarker> arMapMarker = fetchMapMarkers();

        // Drawable drawable = getActivity().getResources().getDrawable(
        // mActivityHelper.getMapPlacemarkIcon(indexSection));
        Drawable drawable = this.getResources().getDrawable(R.drawable.ic_map_default_marker);
        MyItemizedOverlay mItemizedOverlay = new MyItemizedOverlay(drawable,
                mMapView);

        if (arMapMarker.size() > 0) {
            for (MapMarker marker : arMapMarker) {
                OverlayItem overlayitem = new OverlayItem(marker.geoPoint, marker.name,
                        marker.address);
                mItemizedOverlay.addOverlay(overlayitem);
            }
            mMapView.getOverlays().add(INDEX_OVERLAY_PLACEMARKS, mItemizedOverlay);
        }

        initialAnimateToPoint();
    }

    /**
     * Set new map center.
     * 
     * @param mapCenter
     */
    protected void animateToPoint(GeoPoint mapCenter) {
        if (mapCenter != null) {
            mMapCenter = mapCenter;
            mMapController.animateTo(mapCenter);
        }
    }

    /**
     * Initial map center animation on detected user location. If user is more
     * than minimum-distance from the city, center the map on Downtown. Also
     * defines the zoom.
     */
    protected void initialAnimateToPoint() {
        List<String> enabledProviders = mLocationManager.getProviders(true);

        double coordinates[] = Const.MAPS_DEFAULT_COORDINATES;
        final double lat = coordinates[0];
        final double lng = coordinates[1];

        Location userLocation = mAppHelper.getLocation();
        if (userLocation != null) {
            /**
             * Center on app's user location.
             */
            GeoPoint appGeoPoint = Helper.locationToGeoPoint(userLocation);
            mMapController.setCenter(appGeoPoint);
        }
        else {
            /**
             * Center on Downtown.
             */
            GeoPoint cityCenter = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
            mMapController.setCenter(cityCenter);
        }

        if ((mMapCenter == null) && enabledProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            /**
             * Get user current location then display on map.
             */
            mLocationOverlay.runOnFirstFix(new Runnable() {
                public void run() {
                    GeoPoint userGeoPoint = mLocationOverlay.getMyLocation();

                    if (mListener != null) {
                        mListener.OnMyLocationChanged(userGeoPoint);
                    }

                    /**
                     * If user is very far from Montreal (> 25km) we center the
                     * map on Downtown.
                     */
                    float[] resultDistance = new float[1];
                    android.location.Location.distanceBetween(lat, lng,
                            (userGeoPoint.getLatitudeE6() / 1E6),
                            (userGeoPoint.getLongitudeE6() / 1E6), resultDistance);

                    if (resultDistance[0] > Const.MAPS_MIN_DISTANCE) {
                        userGeoPoint = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
                    }

                    mMapCenter = userGeoPoint;
                    mMapController.animateTo(userGeoPoint);
                }
            });
        }
        else if (mMapCenter != null) {
            /**
             * The AppHelper knows the user location from a previous query, so
             * use the saved value.
             */
            mMapController.setCenter(mMapCenter);
        }
    }

    /**
     * The query selected columns
     */
    static final String[] MAP_MARKER_PROJECTION = new String[] {
            BaseColumns._ID,
            ParksColumns.PARK_NAME,
            ParksColumns.PARK_ADDRESS,
            ParksColumns.PARK_GEO_LAT,
            ParksColumns.PARK_GEO_LNG
    };

    /**
     * Get the list of Placemarks from the database and return them as array to
     * be added as OverlayItems in the map.
     * 
     * @return ArrayList of MarpMarkers
     */
    protected ArrayList<MapMarker> fetchMapMarkers() {
        MapMarker mMapMarker;
        ArrayList<MapMarker> alLocations = new ArrayList<MapMarker>();

        Cursor cur = getActivity().getApplicationContext().getContentResolver()
                .query(Parks.CONTENT_URI, MAP_MARKER_PROJECTION, null, null, null);
        if (cur.moveToFirst()) {
            final int columnId = cur.getColumnIndexOrThrow(BaseColumns._ID);
            final int columnName = cur.getColumnIndexOrThrow(ParksColumns.PARK_NAME);
            final int columnAddress = cur.getColumnIndexOrThrow(ParksColumns.PARK_ADDRESS);
            final int columnGeoLat = cur.getColumnIndexOrThrow(ParksColumns.PARK_GEO_LAT);
            final int columnGeoLng = cur.getColumnIndexOrThrow(ParksColumns.PARK_GEO_LNG);

            do {
                mMapMarker = new MapMarker(cur.getInt(columnId), cur.getString(columnName),
                        cur.getString(columnAddress), cur.getDouble(columnGeoLat),
                        cur.getDouble(columnGeoLng));
                alLocations.add(mMapMarker);

            } while (cur.moveToNext());
        }
        /**
         * Note: using startManagingCursor() crashed the application when
         * running on Honeycomb! So we don't manage the cursor and close it
         * manually here.
         */
        cur.close();

        return alLocations;
    }

    /**
     * Data structure of a Placemark/MapMarker/OverlayItem
     */
    protected static class MapMarker {
        public final int id;
        public final String name;
        public final String address;
        public final GeoPoint geoPoint;

        public MapMarker(int id, String name, String address, double geoLat, double geoLng) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.geoPoint = new GeoPoint((int) (geoLat * 1E6), (int) (geoLng * 1E6));
        }
    }

    /**
     * Setter for the MapCenter GeoPoint. Centers map on the new location and
     * displays the ViewBallooon.
     * 
     * @param mapCenter The new location
     */
    public void setMapCenter(GeoPoint mapCenter) {
        animateToPoint(mapCenter);

        Overlay overlayPlacemarks = mMapView.getOverlays().get(INDEX_OVERLAY_PLACEMARKS);
        overlayPlacemarks.onTap(mapCenter, mMapView);
    }

    /**
     * Used for menu's "My Location" and for Postal Code search. Sets the map
     * center on the location with a near zoom.
     */
    public void setMapCenterOnLocation(Location mapCenter) {
        GeoPoint geoPoint = new GeoPoint((int) (mapCenter.getLatitude() * 1E6),
                (int) (mapCenter.getLongitude() * 1E6));

        mMapController.setZoom(ZOOM_NEAR);
        setMapCenter(geoPoint);
    }

}
