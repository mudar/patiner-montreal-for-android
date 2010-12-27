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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.custom.CustomSimpleCursorAdapter;
import ca.mudar.patinoires.data.PatinoiresOpenData;


public class PatinoiresList extends ListActivity {
	private static final String TAG = "PatinoiresList";
	public static final String PREFS_NAME = "PatinoiresPrefsFile";

	private PatinoiresOpenData mDbHelper;
	private String interfaceLanguage;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Log.w( TAG , "onCreate");		
		setContentView(R.layout.rinks_list);

		Intent intent = getIntent();
		String tabTag = intent.getStringExtra( "tabTag" );

//		mDbHelper = PatinerMontreal.getmDbHelper();
		mDbHelper = new PatinoiresOpenData(this);
		mDbHelper.openDb();

		loadPreferences();
		fillData( tabTag );

		registerForContextMenu( getListView() );

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				displayRinksDetails( arg3 );
			} } );
		/*
// TODO this should be in TABS activity
		if ( ( PatinerMontreal.getCurrentTabTag() == PatinerMontreal.TAB_FAVORITES ) && !mDbHelper.hasFavorites() ) {
			PatinerMontreal.setCurrentTabAllRinks();
		}
*/
	}

	
	/**
	 * Close the DB connection. This also reopens the cursor!
	 */
	
	@Override
	protected void onResume() {
		Log.w( TAG , "onResume" );
		super.onResume();
		
		loadPreferences();
		
		CustomSimpleCursorAdapter adapter = (CustomSimpleCursorAdapter) getListView().getAdapter();
		adapter.getCursor().requery();
	}
	
	
	/**
	 * Reopen the DB connection. Cursor is not closed yet
	 */
	/*
	@Override
	protected void onPause() {
		super.onPause();

		SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListView().getAdapter();
		adapter.getCursor().deactivate();
	}
	*/
	/**
	 * Close the unmanaged cursor
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListView().getAdapter();
//		adapter.getCursor().close();
		Log.w( TAG , "onDestroy closeDb" );
		mDbHelper.closeDb();
	}


	


	
	
	/**
	 * Verify is the first launch of the application, to import remote data
	 * @return boolean
	 */
	/*
	private boolean isFirstLaunch() {
		SharedPreferences settings = getSharedPreferences( PatinoiresList.PREFS_NAME , MODE_PRIVATE );

		if ( settings.getString( "prefs_language" , null ) == null ) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putString( "prefs_language" , Locale.getDefault().getLanguage() ).commit();
			return true;
		}
		else {
			return false;
		}
	}
*/
	
	public void loadPreferences() {

		SharedPreferences settings = getSharedPreferences( PREFS_NAME , MODE_PRIVATE );

		mDbHelper.setSortList( Integer.parseInt( settings.getString( "prefs_sort" , Integer.toString( mDbHelper.getSortList() ) ) ) );
		interfaceLanguage = settings.getString( "prefs_language", Locale.getDefault().getLanguage() );

		boolean[] defaultConditions = mDbHelper.getConditions();
		
		mDbHelper.setShowExcellent( settings.getBoolean( "prefs_show_excellent" , defaultConditions[0] ) );
		mDbHelper.setShowGood( settings.getBoolean( "prefs_show_good" , defaultConditions[1] ) );
		mDbHelper.setShowBad( settings.getBoolean( "prefs_show_bad" , defaultConditions[2] ) );
		mDbHelper.setShowClosed( settings.getBoolean( "prefs_show_closed" , defaultConditions[3] ) );
	}

	private void fillData( String tabTag ) {

		Cursor cursor;
		if ( tabTag.equals( PatinerMontreal.TAB_FAVORITES ) ) {
			cursor = mDbHelper.fetchRinksFavorites();
		}
		else if ( tabTag.equals( PatinerMontreal.TAB_SKATING ) ) {
			cursor = mDbHelper.fetchRinksSkating();
		}
		else if ( tabTag.equals( PatinerMontreal.TAB_HOCKEY ) ) {
			cursor = mDbHelper.fetchRinksHockey();
		}
		else {
			cursor = mDbHelper.fetchRinksAll();
		}

		startManagingCursor( cursor );
		
		String[] from = new String[] { PatinoiresOpenData.KEY_ROWID , 
				PatinoiresOpenData.KEY_RINKS_NAME, 
				( interfaceLanguage.equals( "fr" ) ? PatinoiresOpenData.KEY_RINKS_DESC_FR : PatinoiresOpenData.KEY_RINKS_DESC_EN ) ,
//				PatinoiresDbAdapter.KEY_PARKS_GEO_DISTANCE  ,
				PatinoiresOpenData.KEY_BOROUGHS_NAME , 
				PatinoiresOpenData.KEY_BOROUGHS_REMARKS };
		int[] to = new int[] {
				R.id.l_rink__id ,
				R.id.l_rink_name , 
				R.id.l_rink_desc ,
//				R.id.l_park_geo_distance , 
				R.id.l_borough_name , 
				R.id.l_borough_remarks };

		CustomSimpleCursorAdapter rinks = new CustomSimpleCursorAdapter( this , R.layout.rinks_list_item , cursor, from, to, mDbHelper.isSortOnBorough() );
		
		setListAdapter( rinks );
	}



	

	public void displayRinksDetails( long rinkId ) {
//		Log.w( TAG , "interfaceLanguage = " + interfaceLanguage);
		
		Intent intent = new Intent( this, PatinoiresDetails.class );
		intent.putExtra( "rinkId" , rinkId );
		intent.putExtra( "interfaceLanguage" , "fr" );

		startActivity( intent);
	}


	public void displayInfoDialog( int resource ) {

		Resources res = getResources();
		LayoutInflater inflater =  getLayoutInflater(); 
		View view  = inflater.inflate( resource , null );

		String title = (String) res.getText( resource == R.layout.links ? R.string.dialog_links_title : R.string.dialog_about_title );

		if ( resource == R.layout.links ) {
			TextView textViewPatinerMtl = (TextView) view.findViewById( R.id.links_patiner_mtl );
			textViewPatinerMtl.setMovementMethod(LinkMovementMethod.getInstance());
			TextView textViewConditions = (TextView) view.findViewById( R.id.links_conditions_pdf );
			textViewConditions.setMovementMethod(LinkMovementMethod.getInstance());
			TextView textViewContact = (TextView) view.findViewById( R.id.links_rinks_contact );
			textViewContact.setMovementMethod(LinkMovementMethod.getInstance());
		}
		else if ( resource == R.layout.about ) {
			TextView textViewCredits = (TextView) view.findViewById( R.id.dialog_about_credits );
			textViewCredits.setMovementMethod(LinkMovementMethod.getInstance());
		} 

		new AlertDialog.Builder(this).setView( view )
		.setTitle( title )
		.setPositiveButton( android.R.string.ok , null )
		.show();
	}

/*
	private void setInterfaceLanguage( String lg ) {
		SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE ).edit();
		editor.putString( "prefs_language" , lg ).commit();

// TODO: fix complete language switch, with or without restart
		interfaceLanguage = lg;

		Locale locale = new Locale( lg );
//		Log.w(TAG, "New locale is " + locale.toString() );
		Configuration config = getBaseContext().getResources().getConfiguration();
		config.locale = locale;
		Locale.setDefault( locale );
		getBaseContext().getResources().updateConfiguration( config , getBaseContext().getResources().getDisplayMetrics() );

		fillData();
	}
*/
	/*
	public String getInterfaceLanguage() {
		return interfaceLanguage;
	}
*/




	/*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      // refresh your views here
      super.onConfigurationChanged(newConfig);
    }
	 */




	@Override 
	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		Cursor c = (Cursor) getListView().getItemAtPosition( info.position );
		String name = c.getString( c.getColumnIndex( PatinoiresOpenData.KEY_RINKS_NAME ) );

		menu.setHeaderTitle( name );
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.rink_context_menu , menu);

		int isFavorite = c.getInt( c.getColumnIndex( PatinoiresOpenData.KEY_RINKS_IS_FAVORITE ) );
		if ( isFavorite == 1 ) {
			menu.findItem( R.id.favorites_add ).setVisible( false );
			menu.findItem( R.id.favorites_remove ).setVisible( true );
		}

		String phone = c.getString( c.getColumnIndex( PatinoiresOpenData.KEY_PARKS_PHONE ) );
		if ( phone == null ) {
			menu.findItem( R.id.call_rink ).setVisible( false );
		}
		// TODO: verify if closing this cursor is required
		// c.close();
	}


	public boolean onContextItemSelected( MenuItem item ) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		boolean result = super.onContextItemSelected(item);

		
		
		switch( item.getItemId() ) {
		case R.id.favorites_add:
			mDbHelper.updateFavorites( info.id , true );

			CustomSimpleCursorAdapter adapterAdding = (CustomSimpleCursorAdapter) getListView().getAdapter();
//			adapterRemoving.notifyDataSetChanged();
			adapterAdding.getCursor().requery();
		
			result = true;
			break;
		case R.id.favorites_remove:
			mDbHelper.updateFavorites( info.id , false );
		
			CustomSimpleCursorAdapter adapterRemoving = (CustomSimpleCursorAdapter) getListView().getAdapter();
//			adapterRemoving.notifyDataSetChanged();
			adapterRemoving.getCursor().requery();
			
			result = true;
			break;
		case R.id.call_rink:
			result = true;
			Cursor cursorPhone = (Cursor) getListView().getItemAtPosition( info.position );
			String phone = cursorPhone.getString( cursorPhone.getColumnIndex( PatinoiresOpenData.KEY_PARKS_PHONE ) );

			Intent intentPhone = new Intent(Intent.ACTION_DIAL , Uri.parse("tel:" + phone));
			startActivity( intentPhone );
			break;
		case R.id.map_view_rink:
			result = true;
			Cursor cursorMap = (Cursor) getListView().getItemAtPosition( info.position );
			Intent intentMap = new Intent( getApplicationContext() , PatinoiresGMaps.class );
			intentMap.putExtra( "geoLat" , cursorMap.getString( cursorMap.getColumnIndex( PatinoiresOpenData.KEY_PARKS_GEO_LAT ) ) );
			intentMap.putExtra( "geoLng" , cursorMap.getString( cursorMap.getColumnIndex( PatinoiresOpenData.KEY_PARKS_GEO_LNG ) ) );

			startActivity( intentMap );
			break;
		}

		return result;
	}

}
