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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.rinks_list);

		mDbHelper = new PatinoiresOpenData(this);

		currentTab = PatinerMontreal.getCurrentTabTag();

		if ( isFirstLaunch() ) {
			new SyncOpenDataTask().execute( "openDataUpdateFirstLaunch" );
		}

		loadPreferences();

		fillData();

		registerForContextMenu( getListView() );

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				displayRinksDetails( arg3 );
			} } );

	}

	// TODO: remove this duplicate function
	private boolean isFirstLaunch() {
		SharedPreferences settings = getSharedPreferences( PatinoiresList.PREFS_NAME , MODE_PRIVATE );

		return ( settings.getString( "language" , null ) == null );
	}

	private void loadPreferences() {

		SharedPreferences settings = getSharedPreferences( PREFS_NAME , MODE_PRIVATE );

		mDbHelper.setSortList( settings.getInt( "sort" , mDbHelper.getSortList() ) );
		setInterfaceLanguage( settings.getString("language", Locale.getDefault().getLanguage() ) );

		boolean[] defaultConditions = mDbHelper.getConditions();

		mDbHelper.setShowExcellent( settings.getBoolean( "showExcellent" , defaultConditions[0] ) );
		mDbHelper.setShowGood( settings.getBoolean( "showGood" , defaultConditions[1] ) );
		mDbHelper.setShowBad( settings.getBoolean( "showBad" , defaultConditions[2] ) );
		mDbHelper.setShowClosed( settings.getBoolean( "showClosed" , defaultConditions[3] ) );
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

		String[] from = new String[] { 
				PatinoiresDbAdapter.KEY_RINKS_NAME, 
				( interfaceLanguage.equals( "fr" ) ? PatinoiresDbAdapter.KEY_RINKS_DESC_FR : PatinoiresDbAdapter.KEY_RINKS_DESC_EN ) ,
				//				PatinoiresDbAdapter.KEY_RINKS_IS_FAVORITE ,
				PatinoiresDbAdapter.KEY_BOROUGHS_NAME ,
				PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS 

		};
		int[] to = new int[] { R.id.l_rink_name , R.id.l_rink_desc ,  
				//				R.id.l_rink_kind_id , 
				R.id.l_borough_name ,
				R.id.l_borough_remarks , 
				//				R.id.l_rink_is_favorite 
		};

		RinksListCursorAdapter rinks = new RinksListCursorAdapter(this, R.layout.rinks_list_item , cursor, from, to, mDbHelper.isSortOnBorough() );
		//		SimpleCursorAdapter rinks = new SimpleCursorAdapter(this, R.layout.rinks_list_item , c, from, to);

		setListAdapter( rinks );

		mDbHelper.closeDb();
	}


	public void displayMap() {
		Intent intent = new Intent( this, PatinoiresGMaps.class );
		startActivity( intent );
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


	public void dialogSort() {

		Resources res = getResources();

		// TODO: verify this syntax
		final CharSequence[] items = { 
				res.getText( R.string.dialog_sort_distance ) , 
				res.getText( R.string.dialog_sort_rink ) , 
				res.getText( R.string.dialog_sort_park ) , 
				res.getText( R.string.dialog_sort_borough ) };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle( R.string.dialog_sort_title );

		builder.setSingleChoiceItems( items, mDbHelper.getSortList() , new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				setSortOrder( item );
				Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
				dialog.cancel();
			}
		} );
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void setSortOrder( int order ) {
		SharedPreferences.Editor editor = getSharedPreferences( PREFS_NAME, MODE_PRIVATE ).edit();
		editor.putInt("sort", order ).commit();
		mDbHelper.setSortList( order );

		fillData();
	}


	public void dialogConditions() {

		Resources res = getResources();
		tempConditions = mDbHelper.getConditions();

		// TODO: verify this syntax
		final CharSequence[] items = { 
				res.getText( R.string.dialog_condition_excellent ) ,
				res.getText( R.string.dialog_condition_good ) ,
				res.getText( R.string.dialog_condition_bad ) ,
				res.getText( R.string.dialog_condition_closed ) };

		final boolean[] selections =  mDbHelper.getConditions();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle( R.string.dialog_condition_title );
		builder.setMultiChoiceItems( items , selections , new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
				tempConditions[ whichButton ] = isChecked;
			}
		} );

		builder.setPositiveButton( R.string.dialog_ok , new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setConditions( tempConditions );

				fillData();
			}
		} );
		builder.setNegativeButton( R.string.dialog_cancel , new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {}
		} );

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void setConditions( boolean[] conditions ) {
		SharedPreferences.Editor editor = getSharedPreferences( PREFS_NAME, MODE_PRIVATE ).edit();
		editor.putBoolean( "showExcellent", conditions[0] );
		editor.putBoolean( "showGood", conditions[1] );
		editor.putBoolean( "showBad", conditions[2] );
		editor.putBoolean( "showClosed", conditions[3] );
		editor.commit();

		mDbHelper.setShowExcellent( conditions[0] );
		mDbHelper.setShowGood( conditions[1] );
		mDbHelper.setShowBad( conditions[2] );
		mDbHelper.setShowClosed( conditions[3] );

		fillData();
	}


	public void dialogLanguage() {
		Resources res = getResources();

		// TODO: verify this syntax
		final CharSequence[] items = { 
				res.getText( R.string.dialog_language_french ) , 
				res.getText( R.string.dialog_language_english ) };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle( R.string.dialog_language_title );
		builder.setSingleChoiceItems(items, ( interfaceLanguage.equals( "fr" ) ? 0 : 1 ) , new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				setInterfaceLanguage( item == 0 ? "fr" : "en" );

				Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}


	private void setInterfaceLanguage( String lg ) {
		//		Log.w(TAG, "Switch interface language to " + lg);

		SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE ).edit();
		editor.putString( "language" , lg ).commit();
		interfaceLanguage = lg;

		Locale locale = new Locale( lg );
		//		Log.w(TAG, "New locale is " + locale.toString() );
		Configuration config = getBaseContext().getResources().getConfiguration();
		config.locale = locale;
		Locale.setDefault( locale );
		getBaseContext().getResources().updateConfiguration( config , getBaseContext().getResources().getDisplayMetrics() );

		fillData();
	}

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


	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		//		Log.w(TAG, "onCreateOptionsMenu. Language: " +  interfaceLanguage);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}


	/**
	 * Display a progress bar (daily update of all rinks) or a spinning wheel (fas conditions update).
	 * Starts a new thread in the background 
	 */
	public void dialogUpdate() {
		//		mDbHelper.openDataUpdateConditions();
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


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		// TODO : what's the use of the onOptionsItemSelected return value?!
		switch ( item.getItemId() ) {
		case R.id.refresh_list:
			dialogUpdate();
			fillData();	// This goes here to avoid problem with async threads
			return true;
		case R.id.select_conditions:
			dialogConditions();
			return true;
		case R.id.view_map:
			displayMap();
			return true;
		case R.id.options_sort:
			dialogSort();
			return true;
		case R.id.language:
			dialogLanguage();
			return true;
		case R.id.links:
			displayInfoDialog( R.layout.links );
			return true;
		case R.id.about:
			displayInfoDialog( R.layout.about );
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
		//		c.close();
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

		fillData();

		return result;
	}


	protected void onResume() {
		currentTab = PatinerMontreal.getCurrentTabTag();
		loadPreferences();
		fillData();
		super.onResume();
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

	/*
	protected void onPause() {

		super.onPause();
		cursor.close();
	}
	 */
}