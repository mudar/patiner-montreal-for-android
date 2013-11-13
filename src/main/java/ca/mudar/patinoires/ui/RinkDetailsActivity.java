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
import ca.mudar.patinoires.receivers.DetachableResultReceiver;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ActivityHelper;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.view.MenuInflater;
import android.widget.Toast;

public class RinkDetailsActivity extends FragmentActivity {
    protected static final String TAG = "RinkDetailsActivity";

    private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ((PatinoiresApp) getApplicationContext()).updateUiLanguage();

        setContentView(R.layout.activity_rink_details);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(true);
        }

        FragmentManager fm = getSupportFragmentManager();

        mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment)
                fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
        if (mSyncStatusUpdaterFragment == null) {
            mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
            fm.beginTransaction().add(mSyncStatusUpdaterFragment,
                    SyncStatusUpdaterFragment.TAG).commit();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rink_details_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);

        if (item.getItemId() == R.id.menu_refresh) {
            mActivityHelper.triggerRefresh(mSyncStatusUpdaterFragment.mReceiver, true);
            return true;
        }

        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().setBackgroundDrawable(null);
        System.gc();
    }

    // @Override
    // protected void onSaveInstanceState(Bundle outState) {
    // outSt8ate.putInt(Const.KEY_INSTANCE_RINK_ID, mRinkId);
    // super.onSaveInstanceState(outState);
    // }

    public static class SyncStatusUpdaterFragment extends Fragment implements
            DetachableResultReceiver.Receiver {
        public static final String TAG = SyncStatusUpdaterFragment.class.getName();

        // private boolean mSyncing = false;
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

            RinkDetailsActivity activity = (RinkDetailsActivity) getActivity();
            if (activity == null) {
                return;
            }
            activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

            switch (resultCode) {
                case SyncService.STATUS_RUNNING: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
                    // mSyncing = true;
                    break;
                }
                case SyncService.STATUS_FINISHED: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
                    // mSyncing = false;

                    FragmentManager fm = getSupportFragmentManager();
                    RinkDetailsFragment detailsFragment = (RinkDetailsFragment) fm
                            .findFragmentById(R.id.fragment_rink_details);
                    detailsFragment.onConditionsRefresh();

                    Toast.makeText(activity, R.string.toast_sync_finished, Toast.LENGTH_SHORT)
                            .show();
                    break;
                }
                case SyncService.STATUS_ERROR: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
                    // Error happened down in SyncService, show as toast.
                    // mSyncing = false;
                    final String errorText = getString(R.string.toast_sync_error,
                            resultData.getString(Intent.EXTRA_TEXT));
                    Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

}
