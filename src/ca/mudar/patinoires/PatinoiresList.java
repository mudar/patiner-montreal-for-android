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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;


public class PatinoiresList extends ListActivity {
	private static final String TAG = "PatinoiresList";
	public static final String PREFS_NAME = "PatinoiresPrefsFile";

	private PatinoiresOpenData mDbHelper;
	private String interfaceLanguage;
	private boolean[] tempConditions;
	private static String currentTab;
	private ProgressDialog dialog;
	private Cursor cursor;
	private int currentPosition = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.rinks_list);

		mDbHelper = PatinerMontreal.getmDbHelper();

		currentTab = PatinerMontreal.getCurrentTabTag();

		if ( isFirstLaunch() ) {
			dialogUpdate();
		}

		//TODO Verify real need for this, seems managed by onResume
		loadPreferences();
//		fillData();

		registerForContextMenu( getListView() );

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				displayRinksDetails( arg3 );
			} } );
		
		if ( ( PatinerMontreal.getCurrentTabTag() == PatinerMontreal.TAB_FAVORITES ) && !mDbHelper.hasFavorites() ) {
			PatinerMontreal.setCurrentTabAllRinks();
		}

	}

	
	/**
	 * Verify is the first launch of the application, to import remote data
	 * @return boolean
	 */
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

	private void fillData() {
		mDbHelper.openDb();

		if ( currentTab.equals( PatinerMontreal.TAB_FAVORITES ) ) {
			cursor = mDbHelper.fetchRinksFavorites();
		}
		else if ( currentTab.equals( PatinerMontreal.TAB_SKATING ) ) {
			cursor = mDbHelper.fetchRinksSkating();
		}
		else if ( currentTab.equals( PatinerMontreal.TAB_HOCKEY ) ) {
			cursor = mDbHelper.fetchRinksHockey();
		}
		else {
			cursor = mDbHelper.fetchRinksAll();
		}

		startManagingCursor( cursor );
		
		String[] from = new String[] { PatinoiresDbAdapter.KEY_RINKS_NAME, 
				( interfaceLanguage.equals( "fr" ) ? PatinoiresDbAdapter.KEY_RINKS_DESC_FR : PatinoiresDbAdapter.KEY_RINKS_DESC_EN ) ,
//				PatinoiresDbAdapter.KEY_PARKS_GEO_DISTANCE  ,
				PatinoiresDbAdapter.KEY_BOROUGHS_NAME , 
				PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS };
		int[] to = new int[] { 
				R.id.l_rink_name , 
				R.id.l_rink_desc ,
//				R.id.l_park_geo_distance , 
				R.id.l_borough_name , 
				R.id.l_borough_remarks };

		RinksListCursorAdapter rinks = new RinksListCursorAdapter(this, R.layout.rinks_list_item , cursor, from, to, mDbHelper.isSortOnBorough() );
		
		setListAdapter( rinks );

		getListView().setSelection(currentPosition);
		
		mDbHelper.closeDb();
	}


	public void displayMap() {
		Intent intent = new Intent( this, PatinoiresGMaps.class );
		startActivity( intent );
	}

	
	public void displayPreferences() {
		Intent intent = new Intent( this, PatinoiresPreferences.class );
		startActivity( intent );
	}
	
	
	public void displaySearch() {
		onSearchRequested();
	}
	

	public void displayRinksDetails( long rinkId ) {
		Intent intent = new Intent( this, PatinoiresDetails.class );
		intent.putExtra( "rinkId" , rinkId );
		intent.putExtra( "interfaceLanguage" , interfaceLanguage );
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
	
	public String getInterfaceLanguage() {
		return interfaceLanguage;
	}


	/**
	 * Verify if last DB update was more than 24 hours ago (or was never done)
	 * @return True when last update is older than 24 hours.
	 */
	private boolean isDailySyncRequired() {
		SharedPreferences settings = getSharedPreferences( PREFS_NAME , MODE_PRIVATE );

		Long lastUpdateTime =  settings.getLong("lastUpdateTime", 0);

		if ( (lastUpdateTime + (24 * 60 * 60 * 1000) ) < System.currentTimeMillis() ) {
			//        	Log.i( TAG , "Daily sync required. Last update was: " + lastUpdateTime );

			settings.edit().putLong("lastUpdateTime", System.currentTimeMillis() ).commit();
			return true;
		}
		else {
			return false;
		}
	}


	/*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      // refresh your views here
      super.onConfigurationChanged(newConfig);
    }
	 */


	/**
	 * Display a progress bar (daily update of all rinks) or a spinning wheel (fas conditions update).
	 * Starts a new thread in the background 
	 */
	public void dialogUpdate() {
		
		if ( isConnected() == false ) {
			AlertDialog.Builder builder = new AlertDialog.Builder( this );
			builder.setTitle( R.string.dialog_network_connection_title  )
			.setMessage( R.string.dialog_network_connection_message  )
			.setPositiveButton( android.R.string.ok , null )
			.create()
			.show();
		}
		else {
			mDbHelper.openDb();
			int totalRinks = mDbHelper.countAllRinks();
			mDbHelper.closeDb();

			SyncOpenDataTask syncOpenDataTask = new SyncOpenDataTask();
			if ( isDailySyncRequired() || totalRinks == 0 ) {
				dialog = new ProgressDialog(this);
				dialog.setCancelable(true);
				dialog.setMessage( getResources().getText( R.string.dialog_updating_all_rinks ) );
				dialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
				dialog.setProgress( 0 );
				dialog.setMax( totalRinks  );
				dialog.show();

				syncOpenDataTask.execute( "openDataSyncAll" );
			}
			else {
				dialog = ProgressDialog.show( this , null ,  getResources().getText( R.string.dialog_updating_conditions ) , true , true);
				syncOpenDataTask.execute( "openDataUpdateConditions" );
			}
		} 
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		// TODO : what's the use of the onOptionsItemSelected return value?!
		switch ( item.getItemId() ) {
		case R.id.refresh_list:
			dialogUpdate();
			fillData();	// This goes here to avoid problem with async threads
			return true;
		case R.id.view_map:
			displayMap();
			return true;
		case R.id.search:
			displaySearch();
			return true;
		case R.id.preferences:
			displayPreferences();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override 
	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		Cursor c = (Cursor) getListView().getItemAtPosition( info.position );
		String name = c.getString( c.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_NAME ) );

		menu.setHeaderTitle( name );
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.rink_context_menu , menu);

		int isFavorite = c.getInt( c.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_FAVORITE ) );
		if ( isFavorite == 1 ) {
			menu.findItem( R.id.favorites_add ).setVisible( false );
			menu.findItem( R.id.favorites_remove ).setVisible( true );
		}

		String phone = c.getString( c.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_PHONE ) );
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
			mDbHelper.openDb();
			mDbHelper.updateFavorites( info.id , true );
			mDbHelper.closeDb();

			result = true;
			break;
		case R.id.favorites_remove:
			mDbHelper.openDb();
			mDbHelper.updateFavorites( info.id , false );
			mDbHelper.closeDb();
			result = true;

			fillData();
			break;
		case R.id.call_rink:
			result = true;
			Cursor c = (Cursor) getListView().getItemAtPosition( info.position );
			String phone = c.getString( c.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_PHONE ) );

			Intent intentPhone = new Intent(Intent.ACTION_DIAL , Uri.parse("tel:" + phone));
			startActivity( intentPhone );
			break;
		case R.id.map_view_rink:
			result = true;

			Intent intentMap = new Intent( getApplicationContext() , PatinoiresGMaps.class );
			startActivity( intentMap );
			break;
		}

		return result;
	}


	protected void onResume() {
//		getListView().invalidateViews();
		
		super.onResume();
		currentTab = PatinerMontreal.getCurrentTabTag();
		loadPreferences();
		fillData();
	}

	public boolean isConnected() {
		ConnectivityManager conMan = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
		if ( networkInfo == null ) { 
			return false; 
		}
		else {
			return networkInfo.isConnected(); 
		}
	}

	public class SyncOpenDataTask extends AsyncTask<String, Void , Boolean>
	{
		@Override
		protected Boolean doInBackground( String... params ) 
		{
			if ( params[ 0 ] == "openDataUpdateConditions" ) {
				return mDbHelper.openDataUpdateConditions();
			}
			else if ( params[ 0 ] == "openDataUpdateFirstLaunch" ) {
				return mDbHelper.openDataSyncAll( null );
			}
			else {
				return mDbHelper.openDataSyncAll( dialog );
			}
		}

		@Override
		protected void onPostExecute( Boolean result ) 
		{
			super.onPostExecute(result);
			if ( dialog != null) {
				dialog.dismiss();
			}

			fillData();
			String message = (String) getResources().getText( result ? R.string.dialog_updating_result_ok : R.string.dialog_updating_result_error );
			int displayLength = ( result ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG );
			Toast.makeText(getApplicationContext(), message , displayLength ).show();
		}
	}	

	
	protected void onPause() {
		if ( cursor != null ) {
			currentPosition = getListView().getFirstVisiblePosition();
Log.w( TAG , "currentPosition saved = " + currentPosition );
		}
		super.onPause();
	}

}
