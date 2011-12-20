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

package ca.mudar.patinoires;

import ca.mudar.patinoires.providers.RinksDatabase;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.receivers.DetachableResultReceiver;
import ca.mudar.patinoires.utils.EulaHelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends LocationFragmentActivity {

    private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
    private static boolean hasLoadedData;
    private String lang;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        /**
         * SharedPreferences are used to verify determine if syncService is
         * required for initial launch or on database upgrade.
         */
        hasLoadedData = prefs.getBoolean(Const.PrefsNames.HAS_LOADED_DATA, false);
        int dbVersionPrefs = prefs.getInt(Const.PrefsNames.VERSION_DATABASE, -1);

        if (!hasLoadedData || RinksDatabase.getDatabaseVersion() > dbVersionPrefs) {
            hasLoadedData = false;
            createServiceFragment();
        }

        /**
         * Display the GPLv3 licence
         */
        if (!EulaHelper.hasAcceptedEula(this)) {
            EulaHelper.showEula(false, this);
        }

        mAppHelper = (PatinoiresApp) getApplicationContext();

        lang = mAppHelper.getLanguage();
        mAppHelper.updateUiLanguage();

        setContentView(R.layout.activity_main);

        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        setProgressBarVisibility(false);

        /**
         * Android ICS has support for setHomeButtonEnabled() to disable tap on
         * actionbar logo on dashboard.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!lang.equals(mAppHelper.getLanguage())) {
            lang = mAppHelper.getLanguage();
            this.onConfigurationChanged();
        }

        /**
         * Starting the sync service is done onResume() for
         * SyncStatusUpdaterFragment to be ready. Otherwise, we send an empty
         * receiver to the service.
         */
        // TODO Move this to a sync service listener.
        if (!hasLoadedData && (mSyncStatusUpdaterFragment != null)) {
            Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(),
                    SyncService.class);
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mSyncStatusUpdaterFragment.mReceiver);
            startService(intent);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_dashboard, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
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

        invalidateOptionsMenu();
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

    public static class SyncStatusUpdaterFragment extends Fragment implements
            DetachableResultReceiver.Receiver {
        public static final String TAG = SyncStatusUpdaterFragment.class.getName();

        private DetachableResultReceiver mReceiver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mReceiver = new DetachableResultReceiver(new Handler());
            mReceiver.setReceiver(this);
        }

        /** {@inheritDoc} */
        public void onReceiveResult(int resultCode, Bundle resultData) {
            MainActivity activity = (MainActivity) getSupportActivity();
            if (activity == null) {
                return;
            }
            activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
            activity.setProgressBarVisibility(true);

            final ProgressBar progressHorizontal = (ProgressBar) getSupportActivity().findViewById(
                    R.id.progress_horizontal);
            progressHorizontal.setVisibility(View.VISIBLE);

            switch (resultCode) {
                case SyncService.STATUS_RUNNING: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

                    progressHorizontal.incrementProgressBy(resultData.getInt(
                            Const.KEY_BUNDLE_PROGRESS_INCREMENT, 0));
                    /**
                     * Title progress is in range 0..100
                     */
                    getSupportActivity().setProgress(100 * progressHorizontal.getProgress());

                    break;
                }
                case SyncService.STATUS_FINISHED: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
                    activity.setProgressBarVisibility(false);
                    progressHorizontal.setVisibility(View.INVISIBLE);

                    // TODO put this in an activity listener
                    if (EulaHelper.hasAcceptedEula(getSupportActivity().getApplicationContext()))
                    {
                        Toast.makeText(activity, R.string.toast_sync_finished, Toast.LENGTH_SHORT)
                                .show();
                    }
                    finalizeLoadingData(getSupportActivity().getApplicationContext());

                    break;
                }
                case SyncService.STATUS_ERROR: {
                    /**
                     * Error happened down in SyncService: hide progressbars and
                     * show Toast error message.
                     */
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
                    activity.setProgressBarVisibility(false);
                    progressHorizontal.setVisibility(View.INVISIBLE);

                    final String errorText = getString(R.string.toast_sync_error,
                            resultData.getString(Intent.EXTRA_TEXT));
                    Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

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
}
