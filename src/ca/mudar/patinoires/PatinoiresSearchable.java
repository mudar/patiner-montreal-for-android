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

package ca.mudar.patinoires;

import java.util.Locale;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.custom.CustomSimpleCursorAdapter;
import ca.mudar.patinoires.data.PatinoiresDbAdapter;
import ca.mudar.patinoires.data.PatinoiresOpenData;


/**
 * The main activity for the dictionary.  Also displays search results triggered by the search
 * dialog.
 */
public class PatinoiresSearchable extends ListActivity {
	private static final String TAG = "PatinoiresSearchable";

	private static final int MENU_SEARCH = 1;

	private PatinoiresOpenData mDbHelper;
	private Cursor cursor;

	//TODO
	String interfaceLanguage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbHelper = new PatinoiresOpenData(this);
		
		SharedPreferences settings = getSharedPreferences( PatinerMontreal.PREFS_NAME , MODE_PRIVATE );
		interfaceLanguage = settings.getString( "prefs_language", Locale.getDefault().getLanguage() );

		// TODO verify this
//		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		Intent intent = getIntent();

		setContentView(R.layout.rinks_list);


		if (Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
//			Log.w( TAG , "rink found = " + intent.getDataString());
			//long rinkId = intent.getData();
			long rinkId = 1;
			displayDetails( rinkId );

			finish();
		} 

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				displayDetails( arg3 );
			} } );
		/*        
        Log.d( TAG , intent.toString());
        if (intent.getExtras() != null) {
            Log.d( TAG , intent.getExtras().keySet().toString());
        }
		 */
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			String title;
			
			// Different display for map search
			String itemizedOverlay = intent.getStringExtra( "itemizedOverlay" );
			if ( itemizedOverlay != null ) {
				String parkPrefix = (String) getResources().getText( R.string.map_context_park_prefix );
				query = query.replaceFirst( parkPrefix , "" );
				title = parkPrefix + query;
			}
			else {
				title = getString( R.string.search_results, query );
			}

			mDbHelper.openDb();
			fillData( query , title );
			mDbHelper.closeDb();
		}
	}

	private void displayDetails( Long rinkId ) {
		Intent intent = new Intent( getApplicationContext() , PatinoiresDetails.class );
		intent.putExtra( "rinkId" , rinkId );
		intent.putExtra( "interfaceLanguage" , interfaceLanguage );
		startActivity( intent);
	}

	private void fillData( String query , String title ) {
		TextView mTextView = ( TextView ) findViewById( R.id.search_query );
		mTextView.setVisibility( View.VISIBLE );
		mTextView.setText( title );

		cursor = mDbHelper.searchRinks( query , null );
		startManagingCursor( cursor );

		String[] from = new String[] { 
				PatinoiresDbAdapter.KEY_RINKS_NAME, 
				( interfaceLanguage.equals( "fr" ) ? PatinoiresDbAdapter.KEY_RINKS_DESC_FR : PatinoiresDbAdapter.KEY_RINKS_DESC_EN ) ,
				PatinoiresDbAdapter.KEY_BOROUGHS_NAME ,
				PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS 

		};
		int[] to = new int[] { R.id.l_rink_name , R.id.l_rink_desc , R.id.l_borough_name , R.id.l_borough_remarks };

		CustomSimpleCursorAdapter rinks = new CustomSimpleCursorAdapter(this, R.layout.rinks_list_item , cursor, from, to, mDbHelper.isSortOnBorough() );
		setListAdapter( rinks );
	}

	protected void onResume() {
		super.onResume();

		handleIntent(getIntent());
	}

}
