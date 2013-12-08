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

package ca.mudar.patinoires.dashclock;

import android.content.Intent;
import android.database.Cursor;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.appwidget.StackRemoteViewsFactory;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.ui.activity.RinkDetailsActivity;

import static ca.mudar.patinoires.appwidget.StackRemoteViewsFactory.FavoriteRinksQuery;

public class FavoritesDashClockExtension extends DashClockExtension {
    private static final String TAG = "ExampleExtension";

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        addWatchContentUris(new String[]{
                RinksContract.Rinks.CONTENT_NEAREST_FAVORITE_URI.toString()
        });
    }

    @Override
    protected void onUpdateData(int reason) {
        final DashClockItem rink = getNearestFavoriteRink();

        if (rink == null) {
            clearUpdateExtensionData();
        } else {
            publishUpdateExtensionData(rink);
        }
    }

    /**
     * publishUpdate. Display information of nearest favorite rink.
     */
    private void publishUpdateExtensionData(DashClockItem rink) {

        if (rink != null) {
            // Publish the extension data update.
            final String condition = rink.getConditionText(getApplicationContext().getResources());
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_widget_small_default)
                    .status(condition)
                    .expandedTitle(rink.name)
                    .expandedBody(rink.desc
                            + Const.LINE_SEPARATOR
                            + String.format(
                            getApplicationContext().getResources().getString(R.string.rink_details_conditions),
                            condition)
                    )
                    .clickIntent(getOnClickIntent(rink.rinkId)));
        }
    }

    /**
     * Clear DashClock if user has no Favorites
     */
    private void clearUpdateExtensionData() {
        publishUpdate(null);
    }

    /**
     * Get the RinkDetailsActivity intent
     *
     * @param rinkId
     * @return Intent
     */
    private Intent getOnClickIntent(int rinkId) {
        final Intent onClickIntent = new Intent(getApplicationContext(), RinkDetailsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        onClickIntent.setData(RinksContract.Rinks.buildRinkUri(String.valueOf(rinkId)));

        return onClickIntent;
    }

    /**
     * Load favorite rinks data from Cursor, sorted by distance. We return the first (nearest) item.
     *
     * @return DashClockItem
     */
    private DashClockItem getNearestFavoriteRink() {
        Cursor c = getApplicationContext().getContentResolver().query(
                RinksContract.Rinks.CONTENT_NEAREST_FAVORITE_URI,
                StackRemoteViewsFactory.FavoriteRinksQuery.PROJECTION,
                null,
                null,
                RinksContract.Parks.PARK_GEO_DISTANCE + " ASC "
        );

        if (c == null) {
            return null;
        }

        DashClockItem rink = null;
        if (c.moveToFirst()) {
            final int indexRinkDescColumn = (((PatinoiresApp) getApplicationContext()).getLanguage().equals(Const.PrefsValues.LANG_FR)
                    ? FavoriteRinksQuery.RINK_DESC_FR :
                    FavoriteRinksQuery.RINK_DESC_EN);

            rink = new DashClockItem(
                    c.getString(FavoriteRinksQuery.RINK_NAME),
                    c.getString(indexRinkDescColumn),
                    c.getInt(FavoriteRinksQuery.RINK_ID),
                    c.getInt(FavoriteRinksQuery.RINK_KIND_ID),
                    c.getInt(FavoriteRinksQuery.RINK_CONDITION));
        }
        c.close();

        return rink;
    }
}
