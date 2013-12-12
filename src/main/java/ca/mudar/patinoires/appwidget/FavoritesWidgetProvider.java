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
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.ui.activity.MainActivity;
import ca.mudar.patinoires.ui.activity.RinkDetailsActivity;

public class FavoritesWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "FavoritesWidgetProvider";

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, FavoritesWidgetProvider.class);
    }

    public static Intent getAppHomeIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void setRemoteAdapterCompat(RemoteViews rv, int appWidgetId, int viewId, Intent intent) {
        if (Const.SUPPORTS_ICE_CREAM_SANDWICH) {
            rv.setRemoteAdapter(viewId, intent);
        } else {
            rv.setRemoteAdapter(appWidgetId, viewId, intent);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            /**
             * Specify the service to provide data for the collection widget.
             * We need to embed the appWidgetId via the data otherwise it will be ignored.
             */
            final Intent intent = new Intent(context, WidgetUpdateService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_list_favorites);
            setRemoteAdapterCompat(rv, appWidgetId, R.id.stack_view, intent);

            /**
             * Set the empty view to be displayed if the collection is empty.
             * It must be a sibling view of the collection view.
             */
            rv.setEmptyView(R.id.stack_view, R.id.widget_loading);


            /**
             * Here we setup the a pending intent templates. Individuals items of a collection
             * cannot setup their own pending intents, instead, the collection as a whole can
             * setup a pending intent template, and the individual items can set a fillInIntent
             * to create unique before on an item to item basis.
             */
            // RinkDetails intent, on list item click
            final Intent onClickIntent = new Intent(context, RinkDetailsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent onClickPendingIntent = PendingIntent
                    .getActivity(context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.stack_view,
                    onClickPendingIntent);

            // MainActivity intent, on logo click
            final PendingIntent openAppPendingIntent = PendingIntent
                    .getActivity(context, 0, getAppHomeIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_logo,
                    openAppPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}