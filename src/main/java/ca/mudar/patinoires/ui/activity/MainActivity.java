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

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksDatabase;
import ca.mudar.patinoires.receivers.DetachableResultReceiver;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ConnectionHelper;
import ca.mudar.patinoires.utils.EulaHelper;
import ca.mudar.patinoires.utils.LocationUtils;

public class MainActivity extends BaseActivity implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    protected static final String TAG = "MainActivity";
    private static boolean hasLoadedData;
    protected PatinoiresApp mAppHelper;
    protected SharedPreferences prefs;
    boolean mUpdatesRequested = false;
    private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
    private String lang;
    private LocationRequest mLocationRequest;
    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    public static void finalizeLoadingData(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(Const.APP_PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        PatinoiresApp appHelper = (PatinoiresApp) context;

        prefsEditor.putBoolean(Const.PrefsNames.HAS_LOADED_DATA, true);
        prefsEditor.putInt(Const.PrefsNames.VERSION_DATABASE,
                RinksDatabase.getDatabaseVersion());
        prefsEditor.commit();
        hasLoadedData = true;

        /**
         * Make sure the distance is updated on first load!
         */
        Location l = appHelper.getLocation();
        appHelper.initializeLocation();
        appHelper.setLocation(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        /**
         * SharedPreferences are used to verify determine if syncService is
         * required for initial launch or on database upgrade.
         */
        prefs = getSharedPreferences(Const.APP_PREFS_NAME, Context.MODE_PRIVATE);
        hasLoadedData = prefs.getBoolean(Const.PrefsNames.HAS_LOADED_DATA, false);
        int dbVersionPrefs = prefs.getInt(Const.PrefsNames.VERSION_DATABASE, -1);

        if (!hasLoadedData || RinksDatabase.getDatabaseVersion() > dbVersionPrefs) {
            hasLoadedData = false;
        }
        createServiceFragment();

        mAppHelper = (PatinoiresApp) getApplicationContext();
        lang = mAppHelper.getLanguage();


        setContentView(R.layout.activity_main);
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        initializePlayServices();
    }

    @Override
    public void onStart() {
        super.onStart();

        mLocationClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!lang.equals(mAppHelper.getLanguage())) {
            lang = mAppHelper.getLanguage();
            this.onConfigurationChanged();
        }

        startRinkConditionsService();

        mUpdatesRequested = true;
    }

    @Override
    public void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        mLocationClient.disconnect();

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_dashboard, menu);

        return true;
    }

    /*
  * Called by Location Services when the request to connect the
  * client finishes successfully. At this point, you can
  * request the current location or start periodic updates
  */
    @Override
    public void onConnected(Bundle bundle) {
        mAppHelper.setLocation(mLocationClient.getLastLocation());

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
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
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        }
        // else { // Nothing here. Play Services are not not strictly required }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Update the app's location
        mAppHelper.setLocation(location);
    }

    /**
     * Update the interface language, independently from the phone's UI
     * language. This does not override the parent function because the Manifest
     * does not include configChanges.
     */
    private void onConfigurationChanged() {
        View root = findViewById(android.R.id.content).getRootView();

        ((Button) root.findViewById(R.id.home_btn_skating))
                .setText(R.string.btn_skating);
        ((Button) root.findViewById(R.id.home_btn_hockey))
                .setText(R.string.btn_hockey);
        ((Button) root.findViewById(R.id.home_btn_map))
                .setText(R.string.btn_map);
        ((Button) root.findViewById(R.id.home_btn_favorites))
                .setText(R.string.btn_favorites);

        supportInvalidateOptionsMenu();
    }

    private void createServiceFragment() {
        FragmentManager fm = getSupportFragmentManager();

        mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm
                .findFragmentByTag(SyncStatusUpdaterFragment.TAG);
        if (mSyncStatusUpdaterFragment == null) {
            mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
            fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG)
                    .commit();
        }
    }

    /**
     * Starting the sync service is done onResume() for
     * SyncStatusUpdaterFragment to be ready. Otherwise, we send an empty
     * receiver to the service.
     */
    private void startRinkConditionsService() {

        // TODO Move this to a sync service listener.
        if (!hasLoadedData
                && (mSyncStatusUpdaterFragment != null)
                && ConnectionHelper.hasConnection(this)) {

            Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(),
                    SyncService.class);
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mSyncStatusUpdaterFragment.mReceiver);
            startService(intent);
        } else {
            triggerRefresh(mSyncStatusUpdaterFragment.mReceiver, false);
        }
    }

    private void initializePlayServices() {
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        mUpdatesRequested = false;

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
    }

    private void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
    }

    public static class SyncStatusUpdaterFragment extends Fragment implements
            DetachableResultReceiver.Receiver {
        public static final String TAG = SyncStatusUpdaterFragment.class.getName();
        private DetachableResultReceiver mReceiver;
        private boolean hasSyncError = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mReceiver = new DetachableResultReceiver(new Handler());
            mReceiver.setReceiver(this);
        }

        /**
         * {@inheritDoc}
         */
        public void onReceiveResult(int resultCode, Bundle resultData) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) {
                return;
            }
            activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

            final PatinoiresApp appHelper = (PatinoiresApp) getActivity().getApplicationContext();

            switch (resultCode) {
                case SyncService.STATUS_RUNNING: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

                    break;
                }
                case SyncService.STATUS_FINISHED: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

                    // TODO put this in an activity listener
                    if (EulaHelper.hasAcceptedEula(getActivity().getApplicationContext()) && !hasSyncError) {
                        appHelper.showToastText(R.string.toast_sync_finished, Toast.LENGTH_SHORT);
                    }
                    finalizeLoadingData(getActivity().getApplicationContext());

                    break;
                }
                case SyncService.STATUS_IGNORED: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

                    break;
                }
                case SyncService.STATUS_ERROR: {
                    hasSyncError = true;
                    /**
                     * Error happened down in SyncService: hide progressbars and
                     * show Toast error message.
                     */
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

//                    final String errorText = getString(R.string.toast_sync_error_debug,
//                            resultData.getString(Intent.EXTRA_TEXT));
//                    appHelper.showToastText(errorText , Toast.LENGTH_LONG);
                    appHelper.showToastText(R.string.toast_sync_error, Toast.LENGTH_LONG);
                    break;
                }
            }
        }
    }
}
