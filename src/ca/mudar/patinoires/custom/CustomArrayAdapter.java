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

package ca.mudar.patinoires.custom;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.data.PatinoiresDbAdapter.MapRink;

public class CustomArrayAdapter extends ArrayAdapter<MapRink> {

	private LayoutInflater inflater;
	private CharSequence[] labels;
	private int[] rinkIcons;
	
	public CustomArrayAdapter(Context context, int textViewResourceId, ArrayList<MapRink> rinksArrayList){
		super(context, textViewResourceId, rinksArrayList);
		this.inflater = LayoutInflater.from( context );
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = inflater.inflate( R.layout.maps_parks_rink_item , parent, false );
		
		TextView label = (TextView) row.findViewById( R.id.l_rink_desc );
		label.setText( labels[ position ] );
		
		ImageView icon=(ImageView)row.findViewById( R.id.l_rink_kind_id  );
		icon.setImageResource( rinkIcons[ position ] );

		return row;
	}
	
	public void setLabels( CharSequence[] labels ) {
		this.labels = labels;
	}
	public void setIcons( int[] rinkIcons ) {
		this.rinkIcons = rinkIcons;
	}	
}