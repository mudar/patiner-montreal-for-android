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

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.googlemap.LocationAwarenessListener;
import ca.mudar.patinoires.providers.RinksDatabase;
import ca.mudar.patinoires.receivers.DetachableResultReceiver;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ConnectionHelper;
import ca.mudar.patinoires.utils.EulaHelper;

public class MainActivity extends BaseActivity {
    protected static final String TAG = "MainActivity";
    private static boolean hasLoadedData;
    private static boolean hasLoadedDataLocally = false;
    protected PatinoiresApp mAppHelper;
    private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
    private String lang;
    private LocationAwarenessListener mLocationAwarenessListener;

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
        final Location loc = appHelper.getLocation();
        appHelper.initializeLocation();
        appHelper.setLocation(loc);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        /**
         * SharedPreferences are used to verify determine if syncService is
         * required for initial launch or on database upgrade.
         */
        final SharedPreferences prefs = getSharedPreferences(Const.APP_PREFS_NAME,
                Context.MODE_PRIVATE);
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

        mLocationAwarenessListener = new LocationAwarenessListener(this, true);
    }

    @Override
    public void onStart() {
        super.onStart();

        mLocationAwarenessListener.startConnection();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!lang.equals(mAppHelper.getLanguage())) {
            // Update UI if user has changed language in Settings
            lang = mAppHelper.getLanguage();
            this.onConfigurationChanged();
        }

        startRinkConditionsService();
    }

    @Override
    public void onStop() {
        mLocationAwarenessListener.stopConnection();

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_dashboard, menu);

        return true;
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
        if (!hasLoadedData && (mSyncStatusUpdaterFragment != null)) {
            Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(),
                    SyncService.class);
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mSyncStatusUpdaterFragment.mReceiver);
            if (!ConnectionHelper.hasConnection(this) && !hasLoadedDataLocally) {
                intent.putExtra(Const.INTENT_EXTRA_LOCAL_SYNC, true);
                hasLoadedDataLocally = true;
            }
            startService(intent);
        } else {
            triggerRefresh(mSyncStatusUpdaterFragment.mReceiver, false);
        }
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
                        if (appHelper.isSeasonOver()) {
                            appHelper.showToastText(R.string.toast_season_over, Toast.LENGTH_LONG);
                        } else {
                            appHelper.showToastText(R.string.toast_sync_finished, Toast.LENGTH_SHORT);
                        }
                    }
                    if (!hasLoadedDataLocally) {
                        finalizeLoadingData(getActivity().getApplicationContext());
                    }

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
