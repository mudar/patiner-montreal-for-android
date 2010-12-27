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

package ca.mudar.patinoires.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
//import android.util.Log;

public class PatinoiresOpenData extends PatinoiresDbAdapter {

	private static final String TAG = "PatinoiresOpenData";
	private static final String URL_JSON_INITIAL_IMPORT = "http://patinoires.heroku.com/fr/rinks/all.json?include=park,geocoding,borough";
	private static final String URL_JSON_CONDITIONS_UPDATES = "http://patinoires.heroku.com/fr/boroughs.json?include=rinks";


	public PatinoiresOpenData(Context ctx) {
		super(ctx);
	}


	/**
	 * @return boolean Did the process catch any JSONExceptions or SQLException
	 */
	public boolean openDataUpdateConditions() {

		boolean importResult = true;

		String queryResult = jsonImportRemote( URL_JSON_CONDITIONS_UPDATES );

		JSONArray boroughs;
		try {
			boroughs = new JSONArray( queryResult );
			final int totalBoroughs = boroughs.length();
			int totalRinks = 0;

			mDb.beginTransaction();
			// TODO: remove this
//			Random rand = new Random();

//			Log.i( TAG , "Updating OpenData boroughs. Total: " + totalBoroughs );
			try {
				JSONObject borough;
				JSONArray rinks;
				JSONObject rink;
				ContentValues initialValues = new ContentValues();
				for (int i = 0; i < totalBoroughs ; i++) {

					/**
					 *  Get Borough remarks and update_at
					 */
					try {
						borough = (JSONObject) boroughs.getJSONObject(i).get("borough");
					}
					catch ( JSONException e ) { /* Log.e( TAG , "Borough JSONException" ); */ importResult = false; continue; }
					// Log.i( TAG , "Borough n." + i + ", (_id = " + borough.optInt( "id" ) + ") : " + borough.optString("name") );

					initialValues.clear();
					initialValues.put( KEY_BOROUGHS_REMARKS , borough.optString( "remarks" ) );
					initialValues.put( KEY_BOROUGHS_UPDATED_AT , borough.optString( "posted_at" ) );	// !! field names may be confusing	
					try {
						// TODO : verify existing park_id before inserting
						mDb.update( TABLE_NAME_BOROUGHS , initialValues , KEY_ROWID + "=" + borough.optString( "id" ) , null);
					} catch ( SQLException e ) { /* Log.e( TAG , "Borough SQLException" ); */ importResult = false; }


					/**
					 *  Get the borough's rinks
					 */
					try {
						rinks = new JSONArray( borough.optString( "rinks" ) );
					}
					catch ( JSONException e ) { /* Log.e( TAG , "Rinks JSONException" );*/ importResult = false; continue; }

					totalRinks = rinks.length();
					// Log.i( TAG , "totalRinks : " + totalRinks );
					for ( int j = 0; j < totalRinks ; j++ ) {
						/**
						 *  Get Rink updates: cleared, flooded, resurfaced and condition (0.Excellent to 3.Closed)
						 */
						try {
							rink = (JSONObject) rinks.getJSONObject(j);
						}
						catch ( JSONException e ) { /* Log.e( TAG , "Rink JSONException" ); */ importResult = false; continue; }

						initialValues.clear();
						initialValues.put( KEY_RINKS_IS_CLEARED , rink.optString("cleared").toLowerCase().equals( "true" ) );
						initialValues.put( KEY_RINKS_IS_FLOODED , rink.optString("flooded").toLowerCase().equals( "true" ) );
						initialValues.put( KEY_RINKS_IS_RESURFACED , rink.optString("resurfaced").toLowerCase().equals( "true" ) );

						initialValues.put( KEY_RINKS_CONDITION , getConditionIndex( rink.optString("open") , rink.optString("condition") ) );
						//TODO : remove this
//						initialValues.put( KEY_RINKS_CONDITION , rand.nextInt(4) );
						try {
							mDb.update( TABLE_NAME_RINKS , initialValues , KEY_ROWID + "=" + rink.optString( "id" ) , null);
						} 
						catch ( SQLException e ) { /* Log.e( TAG , "Rink SQLException" ); */ }
					}
				}
				mDb.setTransactionSuccessful();
			} 
			finally {
				mDb.endTransaction();
			}

		} catch (JSONException e) { importResult = false; /* Log.e( TAG , "queryResult JSONException" ); */ }

		return importResult;
	}


	/**
	 * Deletes contents of 3 tables (boroughs, parks, rinks) and imports new content for these same tables.
	 * The favorites table is not modified, to keep user's data.
	 * @return boolean Did the process catch any JSONExceptions
	 */
	public boolean openDataSyncAll( ProgressDialog dialog ) {
		boolean importResult = true;

		// Do not drop tables on first launch
		if ( dialog != null ) {
			//			Log.w( TAG , "Deleting old contents. Tables: " + DB_NAME + "." + TABLE_NAME_BOROUGHS + ", " + DB_NAME + "." + TABLE_NAME_PARKS + ", " + DB_NAME + "." + TABLE_NAME_RINKS );
			
			Random rand = new Random();
			dialog.incrementProgressBy( rand.nextInt( 10 ) + 5 );	// Cheating, to encourage users to wait ;)
		}

		String queryResult = jsonImportRemote( URL_JSON_INITIAL_IMPORT );

		JSONArray rinks;
		try {
			rinks = new JSONArray( queryResult );
			String rinkName;
			String rinkDesc;
			String rinkDescEnglish;
			String[] splitName;
			final int totalRinks = rinks.length();
			
			if ( totalRinks == 0 ) { 
				return false; 
			}
			else if ( dialog != null ) {
				dialog.setProgress( 0 );
				dialog.setMax(totalRinks);
			}
			
			try {
				mDb.beginTransaction();
				mDb.execSQL("DELETE FROM " + TABLE_NAME_BOROUGHS );
				mDb.execSQL("DELETE FROM " + TABLE_NAME_PARKS );
				mDb.execSQL("DELETE FROM " + TABLE_NAME_RINKS );
				mDb.setTransactionSuccessful();
			} 
			finally {
				mDb.endTransaction();
			}

			//			Log.i( TAG , "Inserting OpenData rinks. Total: " + totalRinks );
			try {
				mDb.beginTransaction();

				JSONObject rink;
				JSONObject borough;
				JSONObject park;
				JSONObject geocoding;
				ContentValues initialValues = new ContentValues();
				for (int i = 0; i < totalRinks ; i++) {
					if ( dialog != null ) {
						dialog.incrementProgressBy(1);
					}

					/**
					 *  Get rink info and clean name and description. English description is translated manually!
					 */
					try {
						rink = (JSONObject) rinks.getJSONObject(i).get("rink");						
					}
					catch ( JSONException e ) { /* Log.e( TAG , "Rink JSONException" ); */ importResult = false; continue; }
					// Log.i( TAG , "Patinoire n." + i + ", (_id = " + rink.optInt( "id" ) + ") : " + rink.optString("name") );

					splitName = rink.optString("name").split( "," );
					rinkName = splitName[1].replace( "(PSE)" , "" ).replace( "(PPL)" , "" ).replace( "(PP)" , "" ).trim();
					rinkDesc = splitName[0].trim();
					rinkDescEnglish = translateRinkDescription( rinkDesc );					 

					initialValues.clear();
					initialValues.put( KEY_ROWID , rink.optString( "id" ) );
					initialValues.put( KEY_RINKS_PARK_ID , rink.optString( "park_id" ) ); 
					initialValues.put( KEY_RINKS_KIND_ID , rink.optString( "kind_id" ) );
					initialValues.put( KEY_RINKS_NAME , rinkName );
					initialValues.put( KEY_RINKS_DESC_FR , rinkDesc );
					initialValues.put( KEY_RINKS_DESC_EN , rinkDescEnglish );
					initialValues.put( KEY_RINKS_IS_CLEARED , rink.optString("cleared").equals( "true" ) );
					initialValues.put( KEY_RINKS_IS_FLOODED , rink.optString("flooded").equals( "true" ) );
					initialValues.put( KEY_RINKS_IS_RESURFACED , rink.optString("resurfaced").equals( "true" ) );
					initialValues.put( KEY_RINKS_CONDITION , getConditionIndex( rink.optString("open") , rink.optString("condition") ) );
					try {
						mDb.insertOrThrow( TABLE_NAME_RINKS , null, initialValues );	// insertWithOnConflict is API level 8			        	
					} 
					catch ( SQLException e ) { /* Log.w( TAG , "Rink SQLException" ); */ }


					/**
					 *  Get Borough info
					 */
					if ( rink.optString( "borough_id" ).equals( "null" ) ) { continue; }	// Skip empty borough
					
					try {
						borough = (JSONObject) rink.get("borough");
					}
					catch ( JSONException e ) { /* Log.e( TAG , "Borough JSONException" ); */ importResult = false; continue; }
					//Log.i( TAG , "Borough n." + i + ", (_id = " + borough.optInt( "id" ) + ") : " + borough.optString("name") );

					initialValues.clear();
					initialValues.put( KEY_ROWID , borough.optString( "id" ) );
					initialValues.put( KEY_BOROUGHS_NAME , borough.optString( "name" ) );
					initialValues.put( KEY_BOROUGHS_REMARKS , borough.optString( "remarks" ) );
					initialValues.put( KEY_BOROUGHS_UPDATED_AT , borough.optString( "posted_at" ) );	// !! field names may be confusing	
					try {
						// TODO : verify existing park_id before inserting
						mDb.insertOrThrow( TABLE_NAME_BOROUGHS , null, initialValues );	// insertWithOnConflict is API level 8			        	
					} catch ( SQLException e ) { /* Log.w( TAG , "Borough SQLException" ); */ }


					/**
					 *  Get Park info
					 */
					if ( rink.optString( "park_id" ).equals( "null" ) ) { continue; }	// Skip empty park
					
					try {
						park = (JSONObject) rink.get("park");
					}
					catch ( JSONException e ) { /* Log.e( TAG , "Park JSONException" ); */ importResult = false; continue; }
					// Log.i( TAG , "Parc  n." + i + ", (_id = " + park.optInt( "id" ) + ") : " + park.optString("name") );


					/**
					 *  Get Geocoding info of the park
					 */
					try {
						geocoding = (JSONObject) park.get("geocoding");
					}
					catch ( JSONException e ) {/* Log.e( TAG , "Geocoding JSONException" );  */ importResult = false ; continue; }
					// Log.i( TAG , "Geocoding n." + i + ". parks.geocoding_id = " + geocoding.optInt( "id" ) + ", parks.geo_lat " + geocoding.optString("lat") + ", parks.geo_lng " + geocoding.optString("lng") );

					initialValues.clear();
					initialValues.put( KEY_ROWID , park.optString( "id" ) );
					initialValues.put( KEY_PARKS_BOROUGH_ID , borough.optString( "id" ) );
					initialValues.put( KEY_PARKS_NAME , park.optString( "name" ) );
					initialValues.put( KEY_PARKS_GEO_ID , geocoding.optString( "id" ) );
					initialValues.put( KEY_PARKS_GEO_LAT , geocoding.optString("lat") );
					initialValues.put( KEY_PARKS_GEO_LNG , geocoding.optString("lng") );
					if ( park.optString( "address" ).trim().toLowerCase() != "null" ) {
						initialValues.put( KEY_PARKS_ADDRESS , park.optString( "address" ) );						
					}
					if ( park.optString( "telephone" ).trim().toLowerCase() != "null" ) {
						initialValues.put( KEY_PARKS_PHONE , park.optString( "telephone" ) );						
					}
					initialValues.put( KEY_PARKS_IS_CHALET , park.optString( "chalet" ).equals("true") ? "1" : "0" ); 
					initialValues.put( KEY_PARKS_IS_CARAVAN , park.optString( "caravan" ).equals("true") ? "1" : "0" );
					try {
						// TODO : verify existing park_id before inserting
						mDb.insertOrThrow( TABLE_NAME_PARKS , null, initialValues );	// insertWithOnConflict is API level 8			        	
					} catch ( SQLException e ) { /* Log.w( TAG , "Park SQLException" ); */ }

				}
				mDb.setTransactionSuccessful();
			} 
			finally {
				mDb.endTransaction();
			}
		} catch (JSONException e) { importResult = false; /* Log.e( TAG , "queryResult JSONException" ); */ }

		return importResult;
	}


	/**
	 * Manual translation.. yeah!
	 * @param descFr
	 * @return Translation into english
	 */
	private String translateRinkDescription( String descFr ) {

		if ( descFr.equals( "Patinoire de patin libre" ) ) { return "Free skating rink"; }
		else if ( descFr.equals( "Patinoire avec bandes" ) ) { return "Rink with boards"; }
		else if ( descFr.equals( "Patinoire décorative" ) ) { return "Landskaped rink"; }
		else if ( descFr.equals( "Aire de patinage libre" ) ) { return "Free skating area"; }
		else if ( descFr.equals( "Patinoire réfrigérée" ) ) { return "Refrigerated rink"; }
		else if ( descFr.equals( "Patinoire de patin libre no 1" ) ) { return "Free skating rink #1"; }
		else if ( descFr.equals( "Patinoire avec bandes no 1" ) ) { return "Rink with boards #1"; }
		else if ( descFr.equals( "Patinoire avec bandes no 2" ) ) { return "Rink with boards #2"; }
		else if ( descFr.equals( "Patinoire avec bandes no 3" ) ) { return "Rink with boards #3"; }
		else if ( descFr.equals( "Patinoire avec bandes nord" ) ) { return "Rink with boards North"; }
		else if ( descFr.equals( "Patinoire avec bandes sud" ) ) { return "Rink with boards South"; }
		else if ( descFr.equals( "Grande patinoire avec bandes" ) ) { return "Big rink with boards"; }
		else if ( descFr.equals( "Petite patinoire avec bandes" ) ) { return "Small rink with boards"; }
		else { return descFr; }
	}


	/**
	 * This function helps define "Open/Closed" as a 4th condition besides excellent/good/bad. 
	 * Logically, conditions cannot be "good" when the rink is closed! 
	 * 
	 * @param condition Description (in words) of the condition
	 * @return the index used in the DB for conditions
	 */
	public int getConditionIndex( String open , String condition ) {
		condition = condition.toLowerCase();
		open = open.toLowerCase();
		if ( open.equals( "false" ) ) {
			return CONDITION_CLOSED_INDEX;
		}
		else if ( condition.equals( "excellent" ) || condition.equals( "excellente" ) ) {
			return 0;
		} 
		else if ( condition.equals( "good" ) || condition.equals( "bonne" ) ) {
			return 1;
		}  
		else if ( condition.equals( "bad" ) || condition.equals( "mauvaise" ) ) {
			return 2;
		}
		else {
			//			Log.e( TAG , "Rink is open (" + open + "). Condition not found (" + condition + ")." );
			return CONDITION_CLOSED_INDEX;
		}
	}


	/**
	 * @author Andrew Pearson
	 * {@link http://blog.andrewpearson.org/2010/07/android-why-to-use-json-and-how-to-use.html}
	 * 
	 * @param archiveQuery URL of JSON resources 
	 * @return String Raw content, requires use of JSONArray() and getJSONObject() 
	 */
	private String jsonImportRemote( String archiveQuery ) {
		InputStream in = null;
		String queryResult = "";
		try {
			URL url = new URL(archiveQuery);
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			httpConn.setAllowUserInteraction(false);
			httpConn.connect();
			in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while(true){
				read = bis.read(buffer);
				if(read==-1){
					break;
				}
				baf.append(buffer, 0, read);
			}
			queryResult = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			// DEBUG
			//			Log.e(TAG, "MalformedURLException: " + e.toString());
		} catch (IOException e) {
			// DEBUG
			//			Log.e(TAG, "IOException: " + e.toString());
		}

		return queryResult;
	}

	/*
	 * notifyDataSetChanged()
	 * getFirstVisiblePosition()
	 * getLastVisiblePosition()
	 * getChildAt(int index)
	 */
}
