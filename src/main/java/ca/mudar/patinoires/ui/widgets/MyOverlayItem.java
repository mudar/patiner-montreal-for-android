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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MyOverlayItem extends OverlayItem {

    protected String itemId;
    protected String extra;

    public MyOverlayItem(GeoPoint point, String title, String snippet, String itemId, String extra) {
        super(point, title, snippet);

        this.itemId = itemId;
        this.extra = extra;
    }

    public String getItemId() {
        return this.itemId;
    }

    public void setItemId(String id) {
        this.itemId = id;
    }

    public String getExtra() {
        return this.extra;
    }

    public void setItemExtra(String extra) {
        this.extra = extra;
    }

}
