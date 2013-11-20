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

package ca.mudar.patinoires.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.ui.fragment.BaseListFragment.RinksQuery;
import ca.mudar.patinoires.ui.view.IMultiChoiceModeAdapter;
import ca.mudar.patinoires.utils.Helper;

public class RinksCursorAdapter extends SimpleCursorAdapter implements SectionIndexer, IMultiChoiceModeAdapter {
    protected static final String TAG = "RinksCursorAdapter";
    private final int bgSelected;
    private AlphabetIndexer mIndexer;
    private boolean hasIndexer;
    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    public RinksCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
                              int flags, boolean hasIndexer) {
        super(context, layout, c, from, to, flags);
        this.hasIndexer = hasIndexer;
        if (hasIndexer) {
            mIndexer = new AlphabetIndexer(null, RinksQuery.RINK_NAME,
                    " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        bgSelected = context.getResources().getColor(R.color.listview_color_3);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return super.newView(context, cursor, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        int rinkId = cursor.getInt(RinksQuery.RINK_ID);
        int distance = cursor.getInt(RinksQuery.PARK_GEO_DISTANCE);
        int kindId = cursor.getInt(RinksQuery.RINK_KIND_ID);
        int condition = cursor.getInt(RinksQuery.RINK_CONDITION);
        String sDistance = (distance > 0 ? Helper.getDistanceDisplay(context, distance) : "");

        ((TextView) view.findViewById(R.id.rink_distance)).setText(sDistance);

        int imageResource = Helper.getRinkImage(kindId, condition);
        ((ImageView) view.findViewById(R.id.l_rink_kind_id))
                .setImageDrawable(context.getResources().getDrawable(imageResource));

        if (Const.SUPPORTS_HONEYCOMB) {
            toggleBackground(view, rinkId);
        }
    }

    private void toggleBackground(View view, int rinkId) {
        if (mSelection.get(rinkId) != null) {
            view.setBackgroundColor(bgSelected);
        } else {
            view.setBackgroundResource(R.drawable.list_selector);
        }
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

    @Override
    public void setNewSelection(int id, boolean checked) {
        if (checked) {
            mSelection.put(id, checked);
        } else {
            mSelection.remove(id);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getSelectionSize() {
        return mSelection.size();
    }

    @Override
    public String[] getSelectionItems() {

        final Set<String> args = new HashSet<String>(mSelection.size());
        for (int id : mSelection.keySet()) {
            args.add(String.valueOf(id));
        }
        return args.toArray(new String[0]);
    }

    @Override
    public void clearSelection() {
        mSelection = new HashMap<Integer, Boolean>();
        notifyDataSetChanged();
    }
}
