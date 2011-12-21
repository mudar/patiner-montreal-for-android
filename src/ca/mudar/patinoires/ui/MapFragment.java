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
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.widgets.MyItemizedOverlay;
import ca.mudar.patinoires.ui.widgets.MyOverlayItem;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Helper;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.util.Log;
import android.view.LayoutInflater;
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

    protected GeoPoint initGeoPoint = null;

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

        Integer latitude = getSupportActivity().getIntent().getIntExtra(Const.INTENT_EXTRA_GEO_LAT,
                Integer.MIN_VALUE);
        Integer longitude = getSupportActivity().getIntent().getIntExtra(
                Const.INTENT_EXTRA_GEO_LNG, Integer.MIN_VALUE);

        if (!latitude.equals(Integer.MIN_VALUE) && !longitude.equals(Integer.MIN_VALUE)) {
            Log.v(TAG, "coords = " + latitude + "," + longitude);
            initGeoPoint = new GeoPoint(latitude, longitude);
        }

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
     * Initialize Map: centre and load rinks
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

                MyOverlayItem overlayitem = new MyOverlayItem(marker.geoPoint, marker.name,
                        marker.address, marker.id, marker.extra);
                mItemizedOverlay.addOverlay(overlayitem);
            }
            mMapView.getOverlays().add(INDEX_OVERLAY_PLACEMARKS, mItemizedOverlay);
        }

        if (initGeoPoint == null) {
            initialAnimateToPoint();
        }
        else {
            mMapController.setZoom(ZOOM_NEAR);
            setMapCenter(initGeoPoint);
        }
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
                    if (userGeoPoint != null) {
                        mMapController.animateTo(userGeoPoint);
                    }

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
            ParksColumns.PARK_ID,
            ParksColumns.PARK_NAME,
            ParksColumns.PARK_ADDRESS,
            ParksColumns.PARK_GEO_LAT,
            ParksColumns.PARK_GEO_LNG,
            ParksColumns.PARK_TOTAL_RINKS,
            RinksColumns.RINK_DESC_FR,
            RinksColumns.RINK_DESC_EN,
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

        /**
         * Filter rinks by conditions.
         */
        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        Cursor cur = getActivity().getApplicationContext().getContentResolver()
                .query(Parks.CONTENT_URI, MAP_MARKER_PROJECTION, filter, null, null);
        // .query(Parks.CONTENT_URI, MAP_MARKER_PROJECTION, filter, null, null,
        // null, null, null);

        // public Cursor query(Uri uri, String[] projection, String selection,
        // String[] selectionArgs,
        // String groupBy, String having, String sortOrder, String limit) {

        if (cur.moveToFirst()) {

            // TODO: put this in a query Interface
            final int columnId = 0x0;
            final int columnParkId = 0x1;
            final int columnName = 0x2;
            final int columnAddress = 0x3;
            final int columnGeoLat = 0x4;
            final int columnGeoLng = 0x5;
            final int columnRinksTotal = 0x6;
            final int columnDescFr = 0x7;
            final int columnDescEn = 0x8;

            String prefixParcName = getResources().getString(R.string.rink_details_park_name);

            do {
                String extra = "";
                /**
                 * Display the name of the rink or the total number of rinks.
                 */
                int nbRinks = cur.getInt(columnRinksTotal);
                if (nbRinks > 1) {
                    extra = String.format(
                            getResources().getString(R.string.park_total_rinks_plural),
                            nbRinks);
                }
                else {
                    extra = cur.getString(mAppHelper.getLanguage().equals("fr") ?
                            columnDescFr : columnDescEn);
                }

                mMapMarker = new MapMarker(cur.getInt(columnParkId), String.format(
                        cur.getString(columnName), prefixParcName),
                        cur.getString(columnAddress), cur.getDouble(columnGeoLat),
                        cur.getDouble(columnGeoLng), extra);
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
        public final String extra;

        public MapMarker(int id, String name, String address, double geoLat, double geoLng,
                String extra) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.geoPoint = new GeoPoint((int) (geoLat * 1E6), (int) (geoLng * 1E6));
            this.extra = extra;
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
