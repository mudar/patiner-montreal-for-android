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

import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.BaseListFragment.RinksQuery;
import ca.mudar.patinoires.utils.Helper;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class RinksCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    protected static final String TAG = "RinksCursorAdapter";

    private AlphabetIndexer mIndexer;
    private boolean hasIndexer;

    public RinksCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags, boolean hasIndexer) {
        super(context, layout, c, from, to, flags);
        this.hasIndexer = hasIndexer;
        if (hasIndexer) {
            mIndexer = new AlphabetIndexer(null, RinksQuery.RINK_NAME,
                    " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return super.newView(context, cursor, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        int distance = cursor.getInt(RinksQuery.PARK_GEO_DISTANCE);
        int kindId = cursor.getInt(RinksQuery.RINK_KIND_ID);
        int condition = cursor.getInt(RinksQuery.RINK_CONDITION);
        String sDistance = (distance > 0 ? Helper.getDistanceDisplay(context, distance) : "");

        ((TextView) view.findViewById(R.id.rink_distance)).setText(sDistance);
        
        int imageResource = Helper.getRinkImage(kindId, condition);
        ( (ImageView) view.findViewById( R.id.l_rink_kind_id ) ).setImageDrawable( context.getResources().getDrawable(imageResource) );
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        super.swapCursor(cursor);

        if (hasIndexer) {
            mIndexer.setCursor(cursor);
        }

        return cursor;
    }

    @Override
    public int getPositionForSection(int section) {
        return (hasIndexer ? mIndexer.getPositionForSection(section) : 0);
    }

    @Override
    public int getSectionForPosition(int position) {
        return (hasIndexer ? mIndexer.getSectionForPosition(position) : 0);
    }

    @Override
    public Object[] getSections() {
        return (hasIndexer ? mIndexer.getSections() : null);
    }

}
