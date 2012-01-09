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

package ca.mudar.patinoires.utils;

import ca.mudar.patinoires.MainActivity;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.ui.AboutActivity;
import ca.mudar.patinoires.ui.MapActivity;
import ca.mudar.patinoires.ui.RinkDetailsActivity;
import ca.mudar.patinoires.ui.widgets.MyPreferenceActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Parcelable;
import android.view.MenuItem;
import android.widget.Toast;

public class ActivityHelper {
    private static final String TAG = "ActivityHelper";

    protected Activity mActivity;

    /**
     * Instance Creator.
     * 
     * @param activity
     * @return
     */
    public static ActivityHelper createInstance(Activity activity) {
        System.setProperty("http.keepAlive", "false");
        return new ActivityHelper(activity);
    }

    /**
     * The Constructor.
     * 
     * @param activity
     */
    protected ActivityHelper(Activity activity) {
        mActivity = activity;
    }

    /**
     * Go to Dashboard on ActionBar home tap, clearing activity stack
     */
    public final void goHome() {
        if (mActivity instanceof MainActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mActivity.startActivity(intent);
        mActivity.overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
    }

    /**
     * Display the Map, centered on the given coordinates.
     */
    public final void goMap(double lat, double lng) {
        if (mActivity instanceof MapActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity.getApplicationContext(), MapActivity.class);
        intent.putExtra(Const.INTENT_EXTRA_GEO_LAT, (int) (lat * 1E6));
        intent.putExtra(Const.INTENT_EXTRA_GEO_LNG, (int) (lng * 1E6));
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        mActivity.startActivity(intent);
    }

    /**
     * Display the Map. Center will be user location or city center.
     */
    public final void goMap() {
        if (mActivity instanceof MapActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity.getApplicationContext(), MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        mActivity.startActivity(intent);
    }

    /**
     * Display the Map
     */
    public final void goRinkDetails(int id) {
        if (mActivity instanceof RinkDetailsActivity) {
            return;
        }

        final Intent intent = new Intent(mActivity.getApplicationContext(),
                RinkDetailsActivity.class);
        intent.putExtra(Const.INTENT_EXTRA_ID_RINK, id);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        mActivity.startActivity(intent);
    }

    /**
     * Start refresh.
     */
    public void triggerRefresh(Parcelable receiver, boolean forceUpdate) {

        if (!ConnectionHelper.hasConnection(mActivity.getApplicationContext())) {
            ConnectionHelper.showDialogNoConnection(mActivity);
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_SYNC, null,
                mActivity.getApplicationContext(),
                SyncService.class);
        intent.putExtra(Const.INTENT_EXTRA_FORCE_UPDATE, forceUpdate);
        if (receiver != null) {
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
        }
        mActivity.startService(intent);
    }

    /**
     * @param item The selected menu item
     * @param indexSection The current section
     * @return boolean
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        switch (item.getItemId()) {
            case android.R.id.home:
                goHome();
                return true;
            case R.id.menu_preferences:
                intent = new Intent(mActivity, MyPreferenceActivity.class);
                mActivity.startActivity(intent);
                return true;
            case R.id.menu_map:
                goMap();
                return true;
            case R.id.menu_refresh:
                triggerRefresh(null, true);
                return true;
            case R.id.menu_search:
                // TODO Handle search
                Toast.makeText(mActivity.getApplicationContext(), "Coming soon!",
                        Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_about:
                intent = new Intent(mActivity, AboutActivity.class);
                mActivity.startActivity(intent);
                return true;

        }
        return false;
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
        resolver.notifyChange(Rinks.CONTENT_FAVORITES_URI, null);

        resolver.notifyChange(Rinks.CONTENT_SKATING_URI, null);
        resolver.notifyChange(Rinks.CONTENT_HOCKEY_URI, null);
        resolver.notifyChange(Rinks.CONTENT_ALL_URI, null);
    }
}
