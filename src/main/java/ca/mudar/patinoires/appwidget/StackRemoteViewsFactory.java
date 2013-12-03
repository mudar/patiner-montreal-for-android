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
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StackRemoteViewsFactory implements
        RemoteViewsService.RemoteViewsFactory,
        Loader.OnLoadCompleteListener<Cursor> {
    private static final String TAG = "StackRemoteViewsFactory";
    private Context mContext;
    private int mAppWidgetId;
    private int indexRinkDescColumn;
    private Cursor mCursor;
    private CursorLoader mLoader;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        indexRinkDescColumn = FavoriteRinksQuery.RINK_DESC_FR;
        mLoader = new CursorLoader(
                mContext,
                RinksContract.Rinks.CONTENT_FAVORITES_URI,
                FavoriteRinksQuery.PROJECTION,
                null,
                null,
                RinksContract.Rinks.DEFAULT_SORT);
        // mLoader.setUpdateThrottle(500);
        mLoader.registerListener(mAppWidgetId, this);
        mLoader.startLoading();
    }

    @Override
    public void onDestroy() {
        if (mLoader != null) {
            mLoader.reset();
        }
    }

    @Override
    public int getCount() {
        if (mCursor == null) {
            return 0;
        } else {
            return mCursor.getCount();
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.appwidget_list_item);

        if (mCursor.moveToPosition(position)) {
            WidgetItem rink = new WidgetItem(
                    mCursor.getString(FavoriteRinksQuery.RINK_NAME),
                    mCursor.getString(indexRinkDescColumn),
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

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            return;
        }
        mCursor = cursor;

        final PatinoiresApp mAppHelper = ((PatinoiresApp) mContext.getApplicationContext());
        indexRinkDescColumn = (mAppHelper.getLanguage().equals(Const.PrefsValues.LANG_FR)
                ? FavoriteRinksQuery.RINK_DESC_FR :
                FavoriteRinksQuery.RINK_DESC_EN );

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(mContext);
        if (mAppWidgetId == -1) {
            int[] ids = widgetManager.getAppWidgetIds(FavoritesWidgetProvider
                    .getComponentName(mContext));

            widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.stack_view);
        } else {
            widgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.stack_view);
        }
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
                RinksContract.RinksColumns.RINK_IS_FAVORITE
        };
        // final int _ID = 0;
        final int RINK_ID = 1;
        final int RINK_KIND_ID = 2;
        final int RINK_NAME = 3;
        final int RINK_DESC_FR = 4;
        final int RINK_DESC_EN = 5;
        final int RINK_CONDITION = 6;
        // final int RINK_IS_FAVORITE = 7;
    }
}
