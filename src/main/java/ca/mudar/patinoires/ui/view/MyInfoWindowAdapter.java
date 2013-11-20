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

package ca.mudar.patinoires.ui.view;

import android.app.Activity;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import ca.mudar.patinoires.R;

public class MyInfoWindowAdapter implements InfoWindowAdapter {

    private final View mView;

    public MyInfoWindowAdapter(Activity activity) {

        mView = activity.getLayoutInflater().inflate(R.layout.custom_info_window, null);
    }

    /**
     * Override to transform Snippet String into SpannableString, allowing the
     * use of line-separators.
     */
    @Override
    public View getInfoContents(Marker marker) {

        final String title = marker.getTitle();
        final String snippet = marker.getSnippet();
        final SpannableString snippetText = new SpannableString(snippet);

        final TextView titleUi = (TextView) mView.findViewById(R.id.title);
        final TextView snippetUi = ((TextView) mView.findViewById(R.id.snippet));
        titleUi.setText(title);
        snippetUi.setText(snippetText);

        return mView;
    }

    @Override
    public View getInfoWindow(Marker marker) {

        return null;
    }

}