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

package ca.mudar.patinoires.ui.widgets;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.RinkDetailsActivity;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Const.PrefsValues;
import ca.mudar.patinoires.utils.Helper;

import com.google.android.maps.MapView;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MyItemizedOverlay extends BalloonItemizedOverlay<MyOverlayItem> {
    private static final String TAG = "MyItemizedOverlay";

    private ArrayList<MyOverlayItem> m_overlays = new ArrayList<MyOverlayItem>();

    private Context context;
    private PatinoiresApp mAppHelper;

    public MyItemizedOverlay(Drawable defaultMarker, MapView mapView) {
        super(boundCenterBottom(defaultMarker), mapView);

        context = mapView.getContext();
        mAppHelper = (PatinoiresApp) context.getApplicationContext();
    }

    public void addOverlay(MyOverlayItem overlay) {
        m_overlays.add(overlay);
        populate();
    }

    @Override
    protected MyOverlayItem createItem(int i) {
        return m_overlays.get(i);
    }

    @Override
    public int size() {
        return m_overlays.size();
    }

    @Override
    protected boolean onBalloonTap(int index, MyOverlayItem item) {

        String parkId = item.getItemId();
        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        String uri = RinksContract.Parks.buildRinksUri(parkId).toString();

        Cursor cur = context.getContentResolver()
                .query(RinksContract.Parks.buildRinksUri(parkId), ParkRinksQuery.PROJECTION,
                        filter,
                        null, Rinks.DEFAULT_SORT);

        ArrayList<DialogListItemRink> rinksArrayList = new ArrayList<DialogListItemRink>();
        if (cur.moveToFirst()) {
            DialogListItemRink rink;
            do {
                String description = cur.getString(mAppHelper.getLanguage().equals(
                        PrefsValues.LANG_FR) ?
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

        displayDialog(item, rinksArrayList);

        return true;
    }

    @Override
    protected BalloonOverlayView<MyOverlayItem> createBalloonOverlayView() {
        return new MyBalloonOverlayView<MyOverlayItem>(getMapView().getContext(),
                getBalloonBottomOffset());
    }

    private void displayDialog(MyOverlayItem item,
            final ArrayList<DialogListItemRink> rinksArrayList) {
        RinksDialogAdapter parkRinksAdapter = new RinksDialogAdapter(context,
                R.layout.maps_parks_rink_item, rinksArrayList);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(item.getTitle())
                .setAdapter(
                        parkRinksAdapter,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                DialogListItemRink rink = rinksArrayList.get(item);

                                Intent intent = new Intent(context, RinkDetailsActivity.class);
                                intent.putExtra(Const.INTENT_EXTRA_ID_RINK, rink.rinkId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                context.startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alert = builder.create();
        alert.show();
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
        protected static final String TAG = "RinksDialogAdapter";

        private Context context;
        private int textViewResourceId;

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

    private static interface ParkRinksQuery {
        final String[] PROJECTION = new String[] {
                BaseColumns._ID,
                RinksColumns.RINK_ID,
                RinksColumns.RINK_KIND_ID,
                RinksColumns.RINK_DESC_FR,
                RinksColumns.RINK_DESC_EN,
                RinksColumns.RINK_CONDITION,
                // RinksColumns.RINK_IS_FAVORITE
        };

        // final int _ID = 0x0;
        final int RINK_ID = 0x1;
        final int KIND_ID = 0x2;
        final int DESC_FR = 0x3;
        final int DESC_EN = 0x4;
        final int CONDITION = 0x5;
        // final int IS_FAVORITE = 0x6;
    }
}
