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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.widgets.MyInfoWindowAdapter;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Helper;

public class MapFragment extends SupportMapFragment implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    protected static final String TAG = "MapFragment";
    protected static final int INDEX_OVERLAY_MY_LOCATION = 0x0;
    protected static final int INDEX_OVERLAY_PLACEMARKS = 0x1;
    // protected static final float ZOOM_DEFAULT = 12f;
    private static final float ZOOM_NEAR = 17f;
    private static final float ZOOM_MIN = 16f;
    private static final float HUE_MARKER = 228f;
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
    private DbAsyncTask dbAsyncTask = null;
    private Map<String, String> mMarkersMap;

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

        queryOverlays();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
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
        mMap.setLocationSource(null);
        mMap.setPadding(0, getPaddingTop(), 0, 0);

        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter(getActivity()));

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
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
        if (mapCenter != null)
            Log.v(TAG, "lat = " + mapCenter.getLatitude() + " lng = " + mapCenter.getLongitude());
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
        final String parkId = mMarkersMap.get(marker.getId());
        Log.v(TAG, "Park id = " + parkId);


        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        String uri = RinksContract.Parks.buildRinksUri(parkId).toString();

        Cursor cur = getActivity().getContentResolver()
                .query(RinksContract.Parks.buildRinksUri(parkId), ParkRinksQuery.PROJECTION,
                        filter,
                        null, RinksContract.Rinks.DEFAULT_SORT);

        ArrayList<DialogListItemRink> rinksArrayList = new ArrayList<DialogListItemRink>();
        if (cur.moveToFirst()) {
            DialogListItemRink rink;
            do {
                String description = cur.getString(mAppHelper.getLanguage().equals(
                        Const.PrefsValues.LANG_FR) ?
                        ParkRinksQuery.DESC_FR : ParkRinksQuery.DESC_EN);
                int image = Helper.getRinkImage(cur.getInt(ParkRinksQuery.KIND_ID),
                        cur.getInt(ParkRinksQuery.CONDITION));

                rink = new DialogListItemRink(cur.getInt(ParkRinksQuery.RINK_ID),
                        description,
                        image);
                rinksArrayList.add(rink);

            } while (cur.moveToNext());
        }
        cur.close();

        displayDialog(marker, rinksArrayList);


    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void queryOverlays() {
        if (dbAsyncTask != null) {
            dbAsyncTask.cancel(true);
        }

        final String queryFilter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        dbAsyncTask = new DbAsyncTask();
        dbAsyncTask.execute(queryFilter);
    }

    private int getPaddingTop() {

        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            return actionBarHeight;
        } else {
            Resources res = getResources();
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, res.getDisplayMetrics());
            return px;
        }

    }

    /**
     * Container Activity must implement this interface to receive the list item
     * clicks.
     */
    public interface OnMyLocationChangedListener {
        public void OnMyLocationChanged(Location location);
    }

    private static interface RinksQuery {
        // int _TOKEN = 0x10;

        final String[] MAP_MARKER_PROJECTION = new String[]{
                BaseColumns._ID,
                ParksColumns.PARK_ID,
                ParksColumns.PARK_NAME,
                ParksColumns.PARK_ADDRESS,
                ParksColumns.PARK_GEO_LAT,
                ParksColumns.PARK_GEO_LNG,
                ParksColumns.PARK_TOTAL_RINKS,
                RinksColumns.RINK_DESC_FR,
                RinksColumns.RINK_DESC_EN,
                RinksColumns.RINK_IS_FAVORITE
        };
        // final int columnId = 0x0;
        final int columnParkId = 0x1;
        final int columnName = 0x2;
        final int columnAddress = 0x3;
        final int columnGeoLat = 0x4;
        final int columnGeoLng = 0x5;
        final int columnRinksTotal = 0x6;
        final int columnDescFr = 0x7;
        final int columnDescEn = 0x8;
        // final int columnRinkIsFavorite = 0x9;
    }

    private class DbAsyncTask extends AsyncTask<Object, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            mMarkersMap = new HashMap<String, String>();
        }

        @Override
        protected Cursor doInBackground(Object... params) {
            final String queryFilter = (String) params[0];

            try {
                Cursor cur = getActivity()
                        .getApplicationContext()
                        .getContentResolver()
                        .query(RinksContract.Parks.CONTENT_URI,
                                RinksQuery.MAP_MARKER_PROJECTION,
                                queryFilter,
                                null,
                                null);
                return cur;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor == null || !isAdded()) {
                return;
            }

            final int totalMarkers = cursor.getCount();
            if (totalMarkers == 0) {
                cursor.close();
                return;
            }

            if (isCancelled()) {
                cursor.close();
                return;
            }

            mMap.clear();

            // TODO: use same following code between DB and JSON
            if (searchedMarker != null) {
                searchedMarker = mMap.addMarker(new MarkerOptions()
                        .position(searchedMarker.getPosition())
                        .title(searchedMarker.getTitle())
                        .snippet(null)
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).visible(true));
                searchedMarker.showInfoWindow();
                hasHintMarker = false;
            }

            if (screenCenter == null || clickedMarker != null) {
                hasHintMarker = false;
            }
            Location locationCenter = new Location(Const.LOCATION_PROVIDER_DEFAULT);
            if (hasHintMarker) {
                locationCenter.setLatitude(screenCenter.latitude);
                locationCenter.setLongitude(screenCenter.longitude);
            }

            final String prefixParcName = getResources().getString(R.string.rink_details_park_name);

            cursor.moveToFirst();
            do {
                if (isCancelled()) {
                    cursor.close();
                    return;
                }

                final String parkId = cursor.getString(RinksQuery.columnParkId);
                final String parkName = String.format(cursor.getString(RinksQuery.columnName), prefixParcName);
                final double lat = cursor.getDouble(RinksQuery.columnGeoLat);
                final double lng = cursor.getDouble(RinksQuery.columnGeoLng);
                final String address = cursor.getString(RinksQuery.columnAddress);
                String desc = (address != null && address.length() > 0 ? address + Const.LINE_SEPARATOR : "");

                /**
                 * Display the name of the rink or the total number of rinks.
                 */
                int nbRinks = cursor.getInt(RinksQuery.columnRinksTotal);
                if (nbRinks > 1) {
                    desc += String.format(
                            getResources().getString(R.string.park_total_rinks_plural),
                            nbRinks);
                } else {
                    desc += cursor.getString(mAppHelper.getLanguage().equals(Const.PrefsValues.LANG_FR) ?
                            RinksQuery.columnDescFr : RinksQuery.columnDescEn);
                }


                final Marker marker = mMap.addMarker(new MarkerOptions()
                        .title(parkName)
                        .position(new LatLng(lat, lng))
                        .snippet(desc)
                        .icon(BitmapDescriptorFactory.defaultMarker(HUE_MARKER))
                        .visible(true));

                mMarkersMap.put(marker.getId(), parkId);

                if (clickedMarker != null) {
                    if (clickedMarker.getPosition().equals(marker.getPosition())) {
                        marker.showInfoWindow();
                    }
                } else if (hasHintMarker) {
                    Location locationMarker = new Location(Const.LOCATION_PROVIDER_DEFAULT);
                    locationMarker.setLatitude(marker.getPosition().latitude);
                    locationMarker.setLongitude(marker.getPosition().longitude);

//                    if (locationCenter.distanceTo(locationMarker) < DISTANCE_MARKER_HINT) {
//                        marker.showInfoWindow();
//                        hasHintMarker = false;
//                        clickedMarker = marker;
//                    }
                }

            } while (cursor.moveToNext());
            cursor.close();

        }

    }



    private static interface ParkRinksQuery {
        final String[] PROJECTION = new String[] {
                BaseColumns._ID,
                RinksColumns.RINK_ID,
                RinksColumns.RINK_KIND_ID,
                RinksColumns.RINK_DESC_FR,
                RinksColumns.RINK_DESC_EN,
                RinksColumns.RINK_CONDITION,
                // RinksColumns.RINK_IS_FAVORITE
        };

        // final int _ID = 0x0;
        final int RINK_ID = 0x1;
        final int KIND_ID = 0x2;
        final int DESC_FR = 0x3;
        final int DESC_EN = 0x4;
        final int CONDITION = 0x5;
        // final int IS_FAVORITE = 0x6;
    }

    private static class DialogListItemRink {
        public final int rinkId;
        public final String description;
        public final int image;

        public DialogListItemRink(int rinkId, String description, int resourceImage) {
            this.rinkId = rinkId;
            this.description = description;
            this.image = resourceImage;
        }
    }


    private class RinksDialogAdapter extends ArrayAdapter<DialogListItemRink> {
        protected static final String TAG = "RinksDialogAdapter";

        private Context context;
        private int textViewResourceId;

        public RinksDialogAdapter(Context context, int textViewResourceId,
                                  List<DialogListItemRink> objects) {
            super(context, textViewResourceId, objects);

            this.context = context;
            this.textViewResourceId = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(textViewResourceId, null);
            }

            DialogListItemRink item = getItem(position);

            if (item != null) {
                TextView rinkDesc = (TextView) convertView.findViewById(R.id.l_rink_desc);
                rinkDesc.setText(item.description);
                rinkDesc.setCompoundDrawablesWithIntrinsicBounds(item.image, 0, 0, 0);
            }

            return convertView;
        }
    }


    private void displayDialog(Marker marker,
                               final ArrayList<DialogListItemRink> rinksArrayList) {
        RinksDialogAdapter parkRinksAdapter = new RinksDialogAdapter(getActivity(),
                R.layout.maps_parks_rink_item, rinksArrayList);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(marker.getTitle())
                .setAdapter(
                        parkRinksAdapter,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                DialogListItemRink rink = rinksArrayList.get(item);

                                Intent intent = new Intent(getActivity(), RinkDetailsActivity.class);
                                intent.putExtra(Const.INTENT_EXTRA_ID_RINK, rink.rinkId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                getActivity().startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alert = builder.create();
        alert.show();
    }

}
