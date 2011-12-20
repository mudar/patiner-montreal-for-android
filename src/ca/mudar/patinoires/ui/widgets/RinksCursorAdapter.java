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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class RinksCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    protected static final String TAG = "RinksCursorAdapter";

    private AlphabetIndexer mIndexer;

    public RinksCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
        Log.v(TAG, "RinksCursorAdapter");

        final int columnName = 0x2;

        mIndexer = new AlphabetIndexer(null, columnName, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);

        mIndexer.setCursor(cursor);
    }

    @Override
    public int getPositionForSection(int section) {
        return mIndexer.getPositionForSection(section);
    }

    @Override
    public int getSectionForPosition(int position) {
        return mIndexer.getSectionForPosition(position);
    }

    @Override
    public Object[] getSections() {
        return mIndexer.getSections();
    }

}
