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

package ca.mudar.patinoires.googlemap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.ui.activity.RinkDetailsActivity;
import ca.mudar.patinoires.utils.Helper;


public class MapClickListener implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "MapClickListener";
    final private Context mContext;
    private Map<String, String> mMarkersMap = new HashMap<String, String>();

    public MapClickListener(Context context) {
        mContext = context;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final PatinoiresApp appHelper = (PatinoiresApp) mContext.getApplicationContext();

        final String parkId = mMarkersMap.get(marker.getId());

        if (parkId == null) {
            // Not a park, no dialog to show
            return;
        }
        final String filter = Helper.getSqliteConditionsFilter(appHelper.getConditionsFilter());

        Cursor cur = mContext.getContentResolver()
                .query(RinksContract.Parks.buildRinksUri(parkId), ParkRinksQuery.PROJECTION,
                        filter,
                        null, RinksContract.Rinks.DEFAULT_SORT);

        ArrayList<DialogListItemRink> rinksArrayList = new ArrayList<DialogListItemRink>();
        if (cur.moveToFirst()) {
            DialogListItemRink rink;
            do {
                String description = cur.getString(appHelper.getLanguage().equals(
                        Const.PrefsValues.LANG_FR) ?
                        ParkRinksQuery.DESC_FR : ParkRinksQuery.DESC_EN);
                int image = Helper.getRinkImage(cur.getInt(ParkRinksQuery.KIND_ID),
                        cur.getInt(ParkRinksQuery.CONDITION));

                rink = new DialogListItemRink(cur.getInt(ParkRinksQuery.RINK_ID),
                        description,
                        image);
                rinksArrayList.add(rink);

            } while (cur.moveToNext());
        }
        cur.close();

        displayDialog(marker, rinksArrayList);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void displayDialog(Marker marker,
                               final ArrayList<DialogListItemRink> rinksArrayList) {
        if (rinksArrayList.size() == 0) {
            return;
        }

        RinksDialogAdapter parkRinksAdapter = new RinksDialogAdapter(mContext,
                R.layout.maps_parks_rink_item, rinksArrayList);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(marker.getTitle())
                .setAdapter(
                        parkRinksAdapter,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                DialogListItemRink rink = rinksArrayList.get(item);

                                Intent intent = new Intent(mContext, RinkDetailsActivity.class);
                                intent.putExtra(Const.INTENT_EXTRA_ID_RINK, rink.rinkId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                mContext.startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void setMarkers(HashMap<String, String> markersMap) {
        mMarkersMap = markersMap;
    }

    private static interface ParkRinksQuery {
        final String[] PROJECTION = new String[]{
                BaseColumns._ID,
                RinksContract.RinksColumns.RINK_ID,
                RinksContract.RinksColumns.RINK_KIND_ID,
                RinksContract.RinksColumns.RINK_DESC_FR,
                RinksContract.RinksColumns.RINK_DESC_EN,
                RinksContract.RinksColumns.RINK_CONDITION,
                RinksContract.RinksColumns.RINK_IS_FAVORITE
        };
        // final int _ID = 0x0;
        final int RINK_ID = 0x1;
        final int KIND_ID = 0x2;
        final int DESC_FR = 0x3;
        final int DESC_EN = 0x4;
        final int CONDITION = 0x5;
        // final int IS_FAVORITE = 0x6;
    }

    private static class DialogListItemRink {
        public final int rinkId;
        public final String description;
        public final int image;

        public DialogListItemRink(int rinkId, String description, int resourceImage) {
            this.rinkId = rinkId;
            this.description = description;
            this.image = resourceImage;
        }
    }

    private class RinksDialogAdapter extends ArrayAdapter<DialogListItemRink> {
        final protected static String TAG = "RinksDialogAdapter";
        final private Context context;
        final private int textViewResourceId;

        public RinksDialogAdapter(Context context, int textViewResourceId,
                                  List<DialogListItemRink> objects) {
            super(context, textViewResourceId, objects);

            this.context = context;
            this.textViewResourceId = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(textViewResourceId, null);
            }

            DialogListItemRink item = getItem(position);

            if (item != null) {
                TextView rinkDesc = (TextView) convertView.findViewById(R.id.l_rink_desc);
                rinkDesc.setText(item.description);
                rinkDesc.setCompoundDrawablesWithIntrinsicBounds(item.image, 0, 0, 0);
            }

            return convertView;
        }
    }
}
