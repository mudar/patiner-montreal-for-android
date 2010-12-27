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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import ca.mudar.patinoires.PatinoiresDetails;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.data.PatinoiresDbAdapter;
import ca.mudar.patinoires.data.PatinoiresOpenData;
import ca.mudar.patinoires.data.PatinoiresDbAdapter.MapRink;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;


public class CustomItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	protected static final String TAG = "PatinoiresItemizedOverlay";

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	public Context mContext;
	private PatinoiresOpenData mDbHelper;

	public CustomItemizedOverlay( Drawable defaultMarker ,  Context context ) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get( index );

		String parkId = item.getSnippet();

		mDbHelper = new PatinoiresOpenData(mContext);
		mDbHelper.openDb();
		ArrayList<MapRink> rinksArrayList = mDbHelper.fetchParkRinksForMap( parkId );
		mDbHelper.closeDb();

		int nbRinks = rinksArrayList.size();
		if ( nbRinks == 0 ) { return true; } 

		final CharSequence[] labels = new CharSequence[ nbRinks ] ;
		final long[] rinkId = new long[ nbRinks ] ;
		final int[] rinkIcons = new int[ nbRinks ] ;
		int imageResource;
		for ( MapRink rink : rinksArrayList ) {
			labels[ rinksArrayList.indexOf( rink ) ] = rink.descriptionFr ;
			rinkId[ rinksArrayList.indexOf( rink ) ] = rink.rinkId ;

			if ( rink.kindId == PatinoiresDbAdapter.OPEN_DATA_INDEX_PSE ) {
				switch ( rink.conditionIndex ) {
				case 0  : imageResource =  R.drawable.ic_rink_hockey_0; break;
				case 1  : imageResource =  R.drawable.ic_rink_hockey_1; break;
				case 2  : imageResource =  R.drawable.ic_rink_hockey_2; break;
				default : imageResource =  R.drawable.ic_rink_hockey_3; break;
				}
			}
			else {
				switch ( rink.conditionIndex ) {
				case 0  : imageResource =  R.drawable.ic_rink_skating_0; break;
				case 1  : imageResource =  R.drawable.ic_rink_skating_1; break;
				case 2  : imageResource =  R.drawable.ic_rink_skating_2; break;
				default : imageResource =  R.drawable.ic_rink_skating_3; break;
				}
			}
			rinkIcons[ rinksArrayList.indexOf( rink ) ] = imageResource;
		}

		CustomArrayAdapter parkRinksAdapter = new CustomArrayAdapter( mContext , R.id.l_rink_desc, rinksArrayList );

		parkRinksAdapter.setLabels( labels );
		parkRinksAdapter.setIcons( rinkIcons );

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle( item.getTitle() );
		builder.setAdapter( parkRinksAdapter , new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {

				Intent intent = new Intent( mContext, PatinoiresDetails.class );
				intent.putExtra( "rinkId" , rinkId[item] );
				intent.putExtra( "interfaceLanguage" , "fr" );	
				mContext.startActivity(intent);
			}
		} );
        builder.setNegativeButton( android.R.string.cancel , null );


		AlertDialog alert = builder.create();
		alert.show();

		return true;
	}
}
