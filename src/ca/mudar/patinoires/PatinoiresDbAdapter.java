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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
//import android.util.Log;

public class PatinoiresDbAdapter {
	
    protected static final String TAG = "PatinoiresDbAdapter";
    protected static final int DATABASE_VERSION = 2;
	
	public static final String DB_NAME = "patinoires_mtl";
	public static final String TABLE_NAME_BOROUGHS  = "boroughs";
	public static final String TABLE_NAME_PARKS     = "parks";
	public static final String TABLE_NAME_RINKS     = "rinks";
	public static final String TABLE_NAME_FAVORITES = "favorites";

	// "_id" is standard name for ID column 
    public static final String KEY_ROWID = "_id";
    
    // boroughs table structure
    public static final String KEY_BOROUGHS_NAME       = "borough";
    public static final String KEY_BOROUGHS_REMARKS    = "remarks";
    public static final String KEY_BOROUGHS_UPDATED_AT = "updated_at";
    
    // parks table structure
    public static final String KEY_PARKS_BOROUGH_ID = "borough_id";
    public static final String KEY_PARKS_NAME       = "park";
    public static final String KEY_PARKS_GEO_ID     = "geo_id";
    public static final String KEY_PARKS_GEO_LAT    = "geo_lat";
    public static final String KEY_PARKS_GEO_LNG    = "geo_lng";
    public static final String KEY_PARKS_ADDRESS    = "address";
    public static final String KEY_PARKS_PHONE      = "phone";
    public static final String KEY_PARKS_IS_CHALET  = "is_chalet";
    public static final String KEY_PARKS_IS_CARAVAN = "is_caravan";
    
    // rinks table structure
    public static final String KEY_RINKS_PARK_ID       = "park_id";
    public static final String KEY_RINKS_KIND_ID       = "kind_id";
    public static final String KEY_RINKS_NAME          = "rink";
    public static final String KEY_RINKS_DESC_FR       = "desc_fr";
    public static final String KEY_RINKS_DESC_EN       = "desc_en";
    public static final String KEY_RINKS_IS_CLEARED    = "is_cleared";
    public static final String KEY_RINKS_IS_FLOODED    = "is_flooded";
    public static final String KEY_RINKS_IS_RESURFACED = "is_resurfaced";
    public static final String KEY_RINKS_CONDITION     = "condition";
    public static final String KEY_RINKS_IS_FAVORITE   = "is_favorite";	// Not a DB column. SQLite alias of "( f._id IS NOT NULL )"
    
    // favorites table structure
    public static final String KEY_FAVORITES_RINK_ID = "rink_id";
    
    // The city's
    public static final int OPEN_DATA_INDEX_PP  = 4;	// paysagée
    public static final int OPEN_DATA_INDEX_PPL = 5;	// patin libre
    public static final int OPEN_DATA_INDEX_PSE = 6;	// sport d'équipe
    
    protected DatabaseHelper mDbHelper;
    protected SQLiteDatabase mDb;
    protected final Context mCtx;

    protected boolean showExcellent = true;
    protected boolean showGood = true;
    protected boolean showBad = true;
    protected boolean showClosed = true;
    
    protected int sortOrder = 0;
    protected String[] sortOrderValues = { "distance" , "rink" , "park" , "borough" };
//    protected String[] conditionValues = { "excellent" , "good" , "bad" , "closed" };
    
    protected static final int CONDITION_EXCELLENT_INDEX = 0;	// Based on the order in the line above.
    protected static final int CONDITION_GOOD_INDEX      = 1;	
    protected static final int CONDITION_BAD_INDEX       = 2;	
    protected static final int CONDITION_CLOSED_INDEX    = 3;	
    	

    protected static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context ctx) {
            super(ctx, DB_NAME , null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
//        	Log.w( TAG , "Creating 4 database tables: " + TABLE_NAME_BOROUGHS + ", " + TABLE_NAME_PARKS + ", "  + TABLE_NAME_RINKS+ " and " + TABLE_NAME_FAVORITES + ". DB name: " + DB_NAME );

        	db.execSQL( "CREATE TABLE " + TABLE_NAME_BOROUGHS 
    				+ " ( " + KEY_ROWID       + " INTEGER PRIMARY KEY , "
    				+ KEY_BOROUGHS_NAME       + " VARCHAR(100) NOT NULL , " 
    	        	+ KEY_BOROUGHS_REMARKS    + " VARCHAR(255) NULL , "
    				+ KEY_BOROUGHS_UPDATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP );" );
        	
        	db.execSQL( "CREATE TABLE " + TABLE_NAME_PARKS 
    				+ " ( " + KEY_ROWID    + " INTEGER PRIMARY KEY  , "
    	        	+ KEY_PARKS_BOROUGH_ID + " INTEGER NOT NULL DEFAULT '0' , "
    	        	+ KEY_PARKS_NAME       + " VARCHAR(100) NOT NULL , "
    	        	+ KEY_PARKS_GEO_ID     + " INTEGER NOT NULL DEFAULT '0' , "
    	        	+ KEY_PARKS_GEO_LAT    + " VARCHAR(20) NULL , "
    	        	+ KEY_PARKS_GEO_LNG    + " VARCHAR(20) NULL , " 
    	        	+ KEY_PARKS_ADDRESS    + " VARCHAR(255) NULL , "
    	        	+ KEY_PARKS_PHONE      + " VARCHAR(100) NULL , "
    	        	+ KEY_PARKS_IS_CHALET  + " BOOLEAN NOT NULL DEFAULT '0' , "
    	        	+ KEY_PARKS_IS_CARAVAN + " BOOLEAN NOT NULL DEFAULT '0' );" );

        	db.execSQL( "CREATE TABLE " + TABLE_NAME_RINKS 
					+ " ( " + KEY_ROWID       + " INTEGER PRIMARY KEY , "
					+ KEY_RINKS_PARK_ID       + " INTEGER NOT NULL DEFAULT '0' , " 
					+ KEY_RINKS_KIND_ID       + " INTEGER NOT NULL DEFAULT '0' , " 
					+ KEY_RINKS_NAME          + " VARCHAR(100) NOT NULL , " 
					+ KEY_RINKS_DESC_FR       + " VARCHAR(100) NULL , "
					+ KEY_RINKS_DESC_EN       + " VARCHAR(100) NULL , " 
					+ KEY_RINKS_IS_CLEARED    + " BOOLEAN NOT NULL DEFAULT '0' , "
		        	+ KEY_RINKS_IS_FLOODED    + " BOOLEAN NOT NULL DEFAULT '0' , "
		        	+ KEY_RINKS_IS_RESURFACED + " BOOLEAN NOT NULL DEFAULT '0' , "
		        	+ KEY_RINKS_CONDITION     + " INTEGER NOT NULL DEFAULT '" + CONDITION_CLOSED_INDEX + "' );" );
        	
        	db.execSQL( "CREATE TABLE " + TABLE_NAME_FAVORITES 
    				+ " ( " + KEY_ROWID     + " INTEGER PRIMARY KEY AUTOINCREMENT , "
    				+ KEY_FAVORITES_RINK_ID + " INTEGER UNIQUE NOT NULL );" );
         }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            Log.w( TAG , "Upgrading database from version " + oldVersion + " to "  + newVersion + ". Old data will be destroyed. DB name: " + DB_NAME );

            db.execSQL( "DROP TABLE IF EXISTS " + TABLE_NAME_BOROUGHS );
            db.execSQL( "DROP TABLE IF EXISTS " + TABLE_NAME_PARKS );
			db.execSQL( "DROP TABLE IF EXISTS " + TABLE_NAME_RINKS );
			//db.execSQL( "DROP TABLE IF EXISTS " + TABLE_NAME_FAVORITES );
            
            onCreate( db );
        }
    }
    
    
    /**
     * Constructor - takes the context to allow the database to be opened/created
     * 
     * @param ctx the Context within which to work
     */
    public PatinoiresDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    
    /**
     * @return this (self reference, allowing this to be chained in an initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public PatinoiresDbAdapter openDb() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    public void closeDb() {
        mDbHelper.close();
    }

    
	public void setShowExcellent( boolean status ) {
		showExcellent = status;
	}
	public void setShowGood( boolean status ) {
		showGood = status;
	}
	public void setShowBad( boolean status ) {
		showBad = status;
	}
	public void setShowClosed( boolean status ) {
		showClosed = status;
	}
	public boolean[] getConditions() {
		boolean[] conditions = { showExcellent , showGood , showBad , showClosed }; 
		return conditions;
	}
    /**
     * @param int The index of the new sort order. This is relative to sortOrderValues[]
     */
    public void setSortList( int sort ) {
    	    	
    	sortOrder = sort;
    }
    /**
     * @return int The index of the sort order. This is relative to sortOrderValues[] and equivalent to UI radio buttons order. 
     */
    public int getSortList() {
    	return sortOrder;
    }
    public boolean isSortOnBorough() {
    	return sortOrderValues[ sortOrder ].equals( "borough" ); 
    }
    
    public int countAllRinks() {
    	// Select all columns
    	Cursor mCursor = mDb.rawQuery( "SELECT _id FROM " + TABLE_NAME_RINKS ,    			null );

    	return mCursor.getCount();
    }
    
    
    /**
     * @return Cursor over all rinks
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor fetchRinksAll() throws SQLException {
    	
    	return searchRinks( "" , "" );
    }
    
    /**
     * @return Cursor over all rinks
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor fetchRinksFavorites() throws SQLException {
    	
    	return searchRinks( "" , "favorites" );
    }
    
    /**
     * @return Cursor over all rinks
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor fetchRinksSkating() throws SQLException {
    	
    	return searchRinks( "" , "skating" );
    }
    
    /**
     * @return Cursor over all rinks
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor fetchRinksHockey() throws SQLException {
    	
    	return searchRinks( "" , "hockey" );
    }
    
    /**
     * @return Cursor over all rinks
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor searchRinks( String searchString , String tabFilter ) throws SQLException {
    	
    	String sqlOrder;
    	String sqlConditionsFilter = "";
    	String sqlSearchFilter = "";
    	String sqlTabFilter = "";

    	// Get the list order
    	int sqlOrderIndex = getSortList();
// TODO : calculate distance for each rink
if ( sqlOrderIndex == 0 ) { sqlOrderIndex++; }
    	sqlOrder = sortOrderValues[sqlOrderIndex];

    	// Get the filters for the rink conditions
    	boolean[] conditions = getConditions();
    	for (int i = 0; i < conditions.length ; i++) {
    		if ( conditions[i] ) { sqlConditionsFilter += " OR condition = " + i; }
    	}
    	
    	// Build the search string
    	searchString = searchString.trim();
    	if ( searchString.length() > 0 ) {
    		sqlSearchFilter = " AND ( " + KEY_PARKS_NAME + " LIKE '%" + searchString + "%' ) " ;
    	}
    	
    	// Build the tab filter string
    	if ( tabFilter.equals( "favorites" ) ) {
    		sqlTabFilter = " AND ( " + KEY_RINKS_IS_FAVORITE + " = 1 ) " ;
    	}
    	else if ( tabFilter.equals( "skating" ) ) {
    		sqlTabFilter = " AND ( " + KEY_RINKS_KIND_ID + " = " + OPEN_DATA_INDEX_PP + " OR " + KEY_RINKS_KIND_ID + " = " + OPEN_DATA_INDEX_PPL + ") " ;
    	}
    	else if ( tabFilter.equals( "hockey" ) ) {
    		sqlTabFilter = " AND ( " + KEY_RINKS_KIND_ID + " = " + OPEN_DATA_INDEX_PSE + " ) " ;
    	}

    	Cursor mCursor = mDb.rawQuery( "SELECT b.* , p.* , r.* , ( f._id IS NOT NULL ) AS " + KEY_RINKS_IS_FAVORITE
    			+ " FROM " + TABLE_NAME_BOROUGHS + " AS b "
    			+ " JOIN " + TABLE_NAME_PARKS + " AS p ON p.borough_id = b._id "
    			+ " JOIN " + TABLE_NAME_RINKS + " AS r ON r.park_id = p._id " 
    			+ " LEFT JOIN " + TABLE_NAME_FAVORITES + " AS f ON r._id = f.rink_id "
    			+ " WHERE ( 0 " + sqlConditionsFilter + " ) " + sqlSearchFilter + sqlTabFilter 
    			+ " ORDER BY " + sqlOrder , 
    			null );

        return mCursor;
    }

    
    /**
     * @param rowId id of rink to retrieve
     * @return Cursor positioned to matching rink, if found
     * @throws SQLException if rink could not be found/retrieved
     */
    public Cursor fetchRinkDetails( long rowId ) throws SQLException {
    	
    	// Select all columns
    	Cursor mCursor = mDb.rawQuery( "SELECT b.* , p.* , r.* , ( f._id IS NOT NULL ) AS " + KEY_RINKS_IS_FAVORITE
    			+ " FROM " + TABLE_NAME_BOROUGHS + " AS b "
    			+ " JOIN " + TABLE_NAME_PARKS + " AS p ON p.borough_id = b._id "
    			+ " JOIN " + TABLE_NAME_RINKS + " AS r ON r.park_id = p._id " 
    			+ " LEFT JOIN " + TABLE_NAME_FAVORITES + " AS f ON r._id = f.rink_id " 
    			+ " WHERE r._id = " + rowId , 
    			null );

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    
    /**
     * @param rowId id of rink to add to favorites
     * @param isFavorite Boolean value: add or remove rink from favorites
     */
    public void updateFavorites( long rowId , boolean isFavorite ) {
//Log.w( TAG , "updateFavorites" );
        ContentValues initialValues = new ContentValues();
        initialValues.put( KEY_FAVORITES_RINK_ID , rowId );
        if ( isFavorite ) {
        	mDb.insert( TABLE_NAME_FAVORITES , null, initialValues );
        }
        else {	
        	mDb.delete( TABLE_NAME_FAVORITES , KEY_FAVORITES_RINK_ID + " = " + rowId , null );
        }
    }

    
    /**
     * @param rowId id of rink to add to favorites
     * @return Boolean The new value of isFavorite   
     */
    public boolean toggleFavorites( long rowId ) {
    	Cursor mCursor = mDb.rawQuery( "SELECT * FROM " + TABLE_NAME_FAVORITES + " WHERE " + KEY_FAVORITES_RINK_ID + " = " + rowId , null );
    	boolean wasFavorite = ( mCursor.getCount() == 1 ); 
    	mCursor.close();

    	ContentValues initialValues = new ContentValues();
    	initialValues.put( KEY_FAVORITES_RINK_ID , rowId );
    	if ( wasFavorite ) {
//    		Log.w( TAG , "Removed from favorites");
    		// Already in favorites, so remove from favorites
    		mDb.delete( TABLE_NAME_FAVORITES , KEY_FAVORITES_RINK_ID + " = " + rowId , null );
    		return false;
    	}
    	else {
    		// Not found in favorites, so add to favorites
//    		Log.w( TAG , "added to favorites");
    		mDb.insert( TABLE_NAME_FAVORITES , null, initialValues );
    		return true;
    	}
    }
    
	/*
	 * notifyDataSetChanged()
	 * getFirstVisiblePosition()
	 * getLastVisiblePosition()
	 * getChildAt(int index)
	 */

}