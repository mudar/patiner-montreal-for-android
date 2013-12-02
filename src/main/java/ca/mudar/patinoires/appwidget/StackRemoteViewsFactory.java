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

package ca.mudar.patinoires.appwidget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final int mCount = 10;
    private List<WidgetItem> mWidgetItems = new ArrayList<WidgetItem>();
    private Context mContext;
    private int mAppWidgetId;
    private PatinoiresApp mAppHelper;
    private Cursor mCursor;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {

        mAppHelper = ((PatinoiresApp) mContext.getApplicationContext());
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

        // We sleep for 3 seconds here to show how the empty view appears in the interim.
        // The empty view is set in the StackWidgetProvider and should be a sibling of the
        // collection view.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        if (mCursor == null) {
            return 0;
        } else {
            return mCursor.getCount();
        }
    }

    public RemoteViews getViewAt(int position) {

        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.appwidget_list_item);

        if (mCursor.moveToPosition(position)) {
            WidgetItem rink = new WidgetItem(
                    mCursor.getString(FavoriteRinksQuery.RINK_NAME),
                    mCursor.getString(FavoriteRinksQuery.RINK_DESC_EN),
                    mCursor.getInt(FavoriteRinksQuery.RINK_ID),
                    mCursor.getInt(FavoriteRinksQuery.RINK_KIND_ID),
                    mCursor.getInt(FavoriteRinksQuery.RINK_CONDITION)
            );

            rv.setTextViewText(R.id.rink_name, rink.name);
            rv.setTextViewText(R.id.rink_desc, rink.desc);
            rv.setImageViewResource(R.id.l_rink_kind_id, rink.getIcon());

            // Next, we set a fill-intent which will be used to fill-in the pending intent template
            // which is set on the collection view in StackWidgetProvider.
            Bundle extras = new Bundle();
            extras.putInt(Const.INTENT_EXTRA_ID_RINK, rink.rinkId);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.rink_list_item, fillInIntent);
        }

        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(
                RinksContract.Rinks.CONTENT_FAVORITES_URI,
                FavoriteRinksQuery.PROJECTION, null, null, null);
    }

    public static interface FavoriteRinksQuery {
        // int _TOKEN = 0x10;

        final String[] PROJECTION = new String[]{
                BaseColumns._ID,
                RinksContract.RinksColumns.RINK_ID,
                RinksContract.RinksColumns.RINK_KIND_ID,
                RinksContract.RinksColumns.RINK_NAME,
                RinksContract.RinksColumns.RINK_DESC_FR,
                RinksContract.RinksColumns.RINK_DESC_EN,
                RinksContract.RinksColumns.RINK_CONDITION,
                RinksContract.RinksColumns.RINK_IS_FAVORITE,
                RinksContract.ParksColumns.PARK_GEO_LAT,
                RinksContract.ParksColumns.PARK_GEO_LNG,
                RinksContract.ParksColumns.PARK_GEO_DISTANCE,
                RinksContract.ParksColumns.PARK_PHONE
        };
        final int _ID = 0;
        final int RINK_ID = 1;
        final int RINK_KIND_ID = 2;
        final int RINK_NAME = 3;
        final int RINK_DESC_FR = 4;
        final int RINK_DESC_EN = 5;
        final int RINK_CONDITION = 6;
        final int RINK_IS_FAVORITE = 7;
        final int PARK_GEO_LAT = 8;
        final int PARK_GEO_LNG = 9;
        final int PARK_GEO_DISTANCE = 10;
        final int PARK_PHONE = 11;
    }
}
