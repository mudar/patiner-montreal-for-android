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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ConnectionHelper;
import ca.mudar.patinoires.utils.EulaHelper;
import ca.mudar.patinoires.utils.LocationUtils;

public class BaseActivity extends ActionBarActivity {
    protected static final String TAG = "BaseActivity";
    private static final String SEND_INTENT_TYPE = "text/plain";
    private static boolean hasLaunchedEula = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar ab = getSupportActionBar();
        if ((Activity) this instanceof MainActivity) {
            ab.setHomeButtonEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        } else {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        ((PatinoiresApp) getApplicationContext()).updateUiLanguage();

        /**
         * Display the GPLv3 licence
         */
        if (!EulaHelper.hasAcceptedEula(this) && !hasLaunchedEula) {
            hasLaunchedEula = true;
            EulaHelper.showEula(false, this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Const.INTENT_REQ_CODE_EULA) {
            hasLaunchedEula = false;
            boolean hasAcceptedEula = EulaHelper.acceptEula(resultCode, this);
            if (!hasAcceptedEula) {
                this.finish();
            }
        } else if (requestCode == LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.v(TAG, "PlayServices has resolved connection issue");
            }
        }
    }

    /**
     * Display the Map, centered on the given coordinates.
     */
    public final void goMap(double lat, double lng) {
        final Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        intent.putExtra(Const.INTENT_EXTRA_GEO_LAT, lat);
        intent.putExtra(Const.INTENT_EXTRA_GEO_LNG, lng);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /**
     * Display the Map. Center will be user location or city center.
     */
//    public final void goMap() {
//        final Intent intent = new Intent(getApplicationContext(), MapActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        startActivity(intent);
//    }

    /**
     * Display the Map
     */
    public final void goRinkDetails(int id) {

        final Intent intent = new Intent(getApplicationContext(), RinkDetailsActivity.class);
        intent.putExtra(Const.INTENT_EXTRA_ID_RINK, id);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /**
     * Start refresh.
     */
    public void triggerRefresh(Parcelable receiver, boolean forceUpdate) {

        if (!ConnectionHelper.hasConnection(getApplicationContext())) {
            ConnectionHelper.showDialogNoConnection(this);
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_SYNC, null,
                getApplicationContext(),
                SyncService.class);
        intent.putExtra(Const.INTENT_EXTRA_FORCE_UPDATE, forceUpdate);
        if (receiver != null) {
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
        }
        startService(intent);
    }

    /**
     * @param item The selected menu item
     * @return boolean
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        if (item.getItemId() == android.R.id.home) {
            // Respond to the action bar's Up/Home button
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.menu_preferences) {
            if (Const.SUPPORTS_HONEYCOMB) {
                intent = new Intent(this, SettingsActivityHC.class);
            } else {
                intent = new Intent(this, SettingsActivity.class);
            }
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_refresh) {
            triggerRefresh(null, true);
            return true;
        } else if (item.getItemId() == R.id.menu_search) {
            // TODO Handle search
            ((PatinoiresApp) getApplicationContext()).showToastText("Coming soon!", Toast.LENGTH_SHORT);
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_share_map) {
            // Native sharing
            final String shareSubject = getResources().getString(R.string.share_subject_map);
            final String shareText = getResources().getString(R.string.share_text_map);

            final Bundle extras = new Bundle();
            extras.putString(Intent.EXTRA_SUBJECT, shareSubject);
            extras.putString(Intent.EXTRA_TEXT, shareText);

            final Intent sendIntent = new Intent();
            sendIntent.putExtras(extras);
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType(this.SEND_INTENT_TYPE);
            startActivity(sendIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_eula) {
            intent = new Intent(this, EulaActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_rating) {
            // Launch Playstore to rate app
            final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(Uri.parse(Const.URL_PLAYSTORE));
            startActivity(viewIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_share_app) {
            // Share the app
            final String shareSubject = getResources().getString(R.string.share_subject_app);
            final String shareText = String.format(getResources().getString(R.string.share_text_app), Const.URL_PLAYSTORE);

            final Bundle extras = new Bundle();
            extras.putString(Intent.EXTRA_SUBJECT, shareSubject);
            extras.putString(Intent.EXTRA_TEXT, shareText);

            final Intent sendIntent = new Intent();
            sendIntent.putExtras(extras);
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType(this.SEND_INTENT_TYPE);
            startActivity(sendIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Notify the ContentResolvers used in the 4 tabs. This will update the
     * contents of the Favorites ListView. It also updates the context menu of
     * the added/removed favorite rink to toggle the displayed favorites action.
     *
     * @param resolver
     */
    public void notifyAllTabs(ContentResolver resolver) {
        /**
         * We start by the favorites!
         */
        resolver.notifyChange(RinksContract.Rinks.CONTENT_FAVORITES_URI, null);

        resolver.notifyChange(RinksContract.Rinks.CONTENT_SKATING_URI, null);
        resolver.notifyChange(RinksContract.Rinks.CONTENT_HOCKEY_URI, null);
        resolver.notifyChange(RinksContract.Rinks.CONTENT_ALL_URI, null);
    }
}
