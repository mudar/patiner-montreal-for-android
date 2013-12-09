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

package ca.mudar.patinoires.googlemap;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.services.DistanceUpdateService;

public class LocationAwarenessListener implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    private final static String TAG = "LocationAwarenessListener";
    private final Context mContext;
    private final PatinoiresApp mAppHelper;
    private LocationRequest mLocationRequest;
    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    private boolean mHasPassiveLocationUpdates;

    public LocationAwarenessListener(Context context, boolean hasPassiveLocationUpdates) {
        mContext = context;
        mAppHelper = (PatinoiresApp) context.getApplicationContext();
        mHasPassiveLocationUpdates = hasPassiveLocationUpdates;

        // We always initialize the Active locationClient.
        // When stopped, we check if the passive locationClient needs to be started.
        initializeActiveLocationClient();
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onLocationChanged(Location location) {
        // Update the app's location
        mAppHelper.setLocation(location);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mAppHelper.setLocation(mLocationClient.getLastLocation());

        startPeriodicUpdates();
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Nothing here. Play Services are not not strictly required
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        (Activity) mContext,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Thrown if Google Play services canceled the original PendingIntent.
                e.printStackTrace();
            }
        }
        // else { // Nothing here. Play Services are not not strictly required }
    }

    private void initializeActiveLocationClient() {
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        // Set the update interval to five seconds
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_ACTIVE_IN_MILLIS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to two seconds
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_ACTIVE_IN_MILLIS);

        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(mContext, this, this);
    }

    private void initializePassiveLocationClient() {
        mLocationRequest = LocationRequest.create();

        // Set the passive update interval to one hour
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_PASSIVE_IN_MILLIS);

        // Use balanced-power accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);

        // Set the passive interval ceiling to fifteen minutes
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_PASSIVE_IN_MILLIS);

        Intent updateIntent = new Intent(mContext, DistanceUpdateService.class);
        updateIntent.setAction(Const.INTENT_ACTION_PASSIVE_LOCATION);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create a new location client, using the pending intent service to handle callbacks.
        mLocationClient.requestLocationUpdates(mLocationRequest, pendingIntent);
    }

    public void startConnection() {
        mLocationClient.connect();
    }

    public void stopConnection() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        mLocationClient.disconnect();
    }

    public void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    public void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);

        if (mHasPassiveLocationUpdates) {
            // We start the passive location updates when activity leaves foreground
            initializePassiveLocationClient();
        }
    }
}
