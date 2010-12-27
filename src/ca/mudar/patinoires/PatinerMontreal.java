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
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;
import ca.mudar.patinoires.custom.CustomPreferenceActivity;
import ca.mudar.patinoires.data.PatinoiresOpenData;

public class PatinerMontreal extends TabActivity {
	private static final String TAG = "PatinerMontreal";
	
	public static final String PREFS_NAME = "PatinoiresPrefsFile";

	private static PatinoiresOpenData mDbHelper;
	
	public static final String TAB_FAVORITES = "tab_favorites";
	public static final String TAB_SKATING   = "tab_skating";
	public static final String TAB_HOCKEY    = "tab_hockey";
	public static final String TAB_ALL       = "tab_all";
	
	public static final int TAB_INDEX_FAVORITES = 0;
	public static final int TAB_INDEX_ALL       = 3;

	private static TabHost tabHost;
	private ProgressDialog dialog;
	private SharedPreferences prefs;
	private OnSharedPreferenceChangeListener listener;
	private boolean prefsChanged = false;

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	
		super.onCreate(savedInstanceState);
		
		mDbHelper = new PatinoiresOpenData( getApplicationContext() );
		mDbHelper.openDb();
		

		/*	
		if ( isFirstLaunch() ) {
			dialogUpdate();
		}
		 */

		setInterfaceLanguage();

		setContentView(R.layout.main);
		Resources res = getResources();
		tabHost = getTabHost();
		TabHost.TabSpec spec;

		Intent intent = new Intent().setClass(this, PatinoiresList.class);
		intent.putExtra( "tabTag" , TAB_FAVORITES );
		spec = tabHost.newTabSpec( TAB_FAVORITES ).setIndicator( res.getText( R.string.tab_favorites ) , res.getDrawable( R.drawable.ic_tab_favorites ) ).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, PatinoiresList.class);
		intent.putExtra( "tabTag" , TAB_SKATING );
		spec = tabHost.newTabSpec( TAB_SKATING ).setIndicator( res.getText( R.string.tab_skating ) , res.getDrawable( R.drawable.ic_tab_skating ) ).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, PatinoiresList.class);
		intent.putExtra( "tabTag" , TAB_HOCKEY );
		spec = tabHost.newTabSpec( TAB_HOCKEY ).setIndicator( res.getText( R.string.tab_hockey ) , res.getDrawable( R.drawable.ic_tab_hockey ) ).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, PatinoiresList.class);
		intent.putExtra( "tabTag" , TAB_ALL );
		spec = tabHost.newTabSpec( TAB_ALL ).setIndicator( res.getText( R.string.tab_all ) , res.getDrawable( R.drawable.ic_tab_all ) ).setContent(intent);
		tabHost.addTab(spec);
		
//		tabHost.setCurrentTab( mDbHelper.hasFavorites() ? TAB_INDEX_FAVORITES : TAB_INDEX_ALL );
		tabHost.setCurrentTab( TAB_INDEX_FAVORITES );
		
// onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)

		prefs = getSharedPreferences( PatinoiresList.PREFS_NAME , MODE_PRIVATE );

		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				Log.w( TAG , "onSharedPreferenceChanged" );
prefsChanged = true;

			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}
	
	@Override
	protected void onResume() {
		
		if ( prefsChanged ) {
			prefsChanged = false;
			Bundle bundle =getIntent().getExtras();
			onDestroy();
			onCreate( bundle );
			onResume();
	
//			Intent intent = new Intent( getApplicationContext() , PatinerMontreal.class );
//			intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
//			intent.setFlags( Intent.FLAG_ACTIVITY_NO_ANIMATION );
//			startActivity( intent );
	
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		Log.w( TAG , "onDestroy closeDb" );
		mDbHelper.closeDb();
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void onConfigurationChanged(Configuration conf) {
		super.onConfigurationChanged(conf);
	}

	
	/**
	 * Create menu when button presed
	 */
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	/**
	 * Handle the menu actions
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case R.id.refresh_list:
			dialogUpdate();
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

	
	final public String getInterfaceLanguage() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE );
		return settings.getString( "prefs_language" , Locale.getDefault().getLanguage() );
	}
	
	private void setInterfaceLanguage() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE );
		
		String lg = settings.getString( "prefs_language" , Locale.getDefault().getLanguage() );
		Locale locale = new Locale( lg );

		Configuration config = getBaseContext().getResources().getConfiguration();
		config.locale = locale;
		Locale.setDefault( locale );
		getBaseContext().getResources().updateConfiguration( config , getBaseContext().getResources().getDisplayMetrics() );
	}
	
	
	public void displayMap() {
		Intent intent = new Intent( this, PatinoiresGMaps.class );
		startActivity( intent );
	}

	public void displaySearch() {
		onSearchRequested();
	}
	
	public void displayPreferences() {
		Intent intent = new Intent( this, CustomPreferenceActivity.class );
		startActivity( intent );
	}
	

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
			int totalRinks = mDbHelper.countAllRinks();

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
	
	
	/**
	 * Verify available internet connection
	 * @return Boolean
	 */
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
	
	
	/**
	 * Verify if last DB update was more than 24 hours ago (or was never done)
	 * @return Boolean. True when last update is older than 24 hours.
	 */
	private boolean isDailySyncRequired() {
		SharedPreferences settings = getSharedPreferences( PREFS_NAME , MODE_PRIVATE );
		settings.edit().putLong("lastFastUpdateTime", System.currentTimeMillis() ).commit();

		Long lastFullUpdateTime =  settings.getLong("lastFullUpdateTime", 0);

		if ( (lastFullUpdateTime + (24 * 60 * 60 * 1000) ) < System.currentTimeMillis() ) {
			// Log.i( TAG , "Daily sync required. Last full update was: " + lastFullUpdateTime );

			settings.edit().putLong("lastFullUpdateTime", System.currentTimeMillis() ).commit();
			return true;
		}
		else {
			return false;
		}
	}
	
	private class SyncOpenDataTask extends AsyncTask<String, Void , Boolean>
	{
		Intent intent = getIntent();
		final String tabTag = intent.getStringExtra( "tabTag" );
		
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
			/*			
			CursorAdapter adapter = (CursorAdapter) getListView().getAdapter();
			adapter.getCursor().requery();
			Log.e( TAG , "tabTag = " + tabTag );
			*/
			// TODO Verify activity life cycle
			onResume();
			
			String message = (String) getResources().getText( result ? R.string.dialog_updating_result_ok : R.string.dialog_updating_result_error );
			int displayLength = ( result ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG );
			Toast.makeText(getApplicationContext(), message , displayLength ).show();
		}
	}
}
