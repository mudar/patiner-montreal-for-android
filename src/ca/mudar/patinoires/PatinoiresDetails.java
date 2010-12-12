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

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class PatinoiresDetails extends Activity {
    protected static final String TAG = "PatinoiresDetails";
    private PatinoiresOpenData mDbHelper ; 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new PatinoiresOpenData( this );
        
        LayoutInflater mInflater = LayoutInflater.from( this );
        View view = mInflater.inflate( R.layout.rinks_details , null , false );

        Bundle extras = getIntent().getExtras();
        if( extras != null ) {
        	long rinkId = extras.getLong("rinkId");
        	String interfaceLanguage = extras.getString("interfaceLanguage");
    	
        	view = fillData( view , rinkId , interfaceLanguage );
        }
        
        setContentView( view );
    }

    
    private String getConditionText( int condition ) {
    	int conditionIndex;
    	
    	switch( condition ) {
    	case PatinoiresDbAdapter.CONDITION_EXCELLENT_INDEX : 
    		conditionIndex = R.string.dialog_condition_excellent;
    		break;
    	case PatinoiresDbAdapter.CONDITION_GOOD_INDEX : 
    		conditionIndex = R.string.dialog_condition_good;
    		break;
    	case PatinoiresDbAdapter.CONDITION_BAD_INDEX : 
    		conditionIndex = R.string.dialog_condition_bad;
    		break;
    	default : 
    		conditionIndex = R.string.dialog_condition_closed;
    		break;
    	}
    	
    	return (String) getResources().getText( conditionIndex );
    }
    
    
    private String getSurfaceText( int isCleared , int isFlooded , int isResurfaced ) {
    	int surfaceIndex;

    	if ( isResurfaced == 1 ) {
    		surfaceIndex = R.string.rink_details_is_resurfaced; 
    	}
    	else if ( isFlooded == 1 ) {
    		surfaceIndex = R.string.rink_details_is_flooded; 
    	}
    	else if ( isCleared == 1 ) {
    		surfaceIndex = R.string.rink_details_is_cleared;
    	}
    	else {
    		return "";
    	}
    	
    	return (String) getResources().getText( surfaceIndex );
    }

    
    public View fillData( View view , final long rinkId , final String language ) {
        mDbHelper.openDb();
    	
		Cursor cursor = mDbHelper.fetchRinkDetails( rinkId );
		startManagingCursor( cursor );

		/**
		 * Display the hockey/skating image, color is based on conditions
		 */
		int conditionIndex = cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_CONDITION ) );
		int imageResource;
		if ( cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_KIND_ID ) ) == PatinoiresDbAdapter.OPEN_DATA_INDEX_PSE ) {
			switch ( conditionIndex ) {
			case 0  : imageResource =  R.drawable.ic_rink_hockey_0; break;
			case 1  : imageResource =  R.drawable.ic_rink_hockey_1; break;
			case 2  : imageResource =  R.drawable.ic_rink_hockey_2; break;
			default : imageResource =  R.drawable.ic_rink_hockey_3; break;
			}
		}
		else {
			switch ( conditionIndex ) {
			case 0  : imageResource =  R.drawable.ic_rink_skating_0; break;
			case 1  : imageResource =  R.drawable.ic_rink_skating_1; break;
			case 2  : imageResource =  R.drawable.ic_rink_skating_2; break;
			default : imageResource =  R.drawable.ic_rink_skating_3; break;
			}
		}
	    ( (ImageView) view.findViewById(R.id.l_rink_kind_id) ).setImageResource( imageResource );
	    

	    /**
	     * Display Rink name and description
	     */
	    ( (TextView) view.findViewById(R.id.l_rink_name) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_NAME ) )  
	    );
	    ( (TextView) view.findViewById(R.id.l_rink_desc) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( ( language.equals( "fr" ) ? PatinoiresDbAdapter.KEY_RINKS_DESC_FR : PatinoiresDbAdapter.KEY_RINKS_DESC_EN ) ) )  
	    );

	    /**
	     * Display condition with background color
	     * If closed, hide the surface information
	     */
	    TextView viewRinkCondition = ( (TextView) view.findViewById(R.id.l_rink_condition) );
	    viewRinkCondition.append( getConditionText( conditionIndex ) );
	    
	    viewRinkCondition.setBackgroundColor( RinksListCursorAdapter.getConditionColor( conditionIndex ) );
	    if ( ( conditionIndex == PatinoiresDbAdapter.CONDITION_EXCELLENT_INDEX ) || ( conditionIndex == PatinoiresDbAdapter.CONDITION_GOOD_INDEX ) ) {
	    	viewRinkCondition.setTextColor( Color.parseColor( "#000000" ) );
	    } 
	    else {
	    	viewRinkCondition.setTextColor( Color.parseColor( "#FFFFFF" ) );
	    }


	    /**
	     * Display surface on/off icons, or hide if rink is closed
	     * Text information equals the best surface service only (cannot be surfaced if not cleared!). 
	     */
	    String surface;
	    if ( conditionIndex == PatinoiresDbAdapter.CONDITION_CLOSED_INDEX ) {
	    	// Hidden, since rink is closed
	    	view.findViewById(R.id.l_rink_surface).setVisibility( View.GONE );
		    view.findViewById(R.id.l_rink_is_cleared).setVisibility( View.GONE );
		    view.findViewById(R.id.l_rink_is_flooded).setVisibility( View.GONE );
		    view.findViewById(R.id.l_rink_is_resurfaced).setVisibility( View.GONE );
	    } 
	    else {
	    	// Display the on/off icons. Set text to the best available surface service 
	    	int isCleared = cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_CLEARED ) );
	    	int isFlooded = cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_FLOODED ) );
	    	int isResurfaced = cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_RESURFACED ) );
	    	surface = getSurfaceText( isCleared , isFlooded , isResurfaced );
	    	( (TextView) view.findViewById(R.id.l_rink_surface) ).append( surface );

	    	( (ImageView) view.findViewById(R.id.l_rink_is_cleared) ).setImageResource(
	    			isCleared == 1 ? R.drawable.ic_surface_cleared_on : R.drawable.ic_surface_cleared_off  
	    	);
	    	( (ImageView) view.findViewById(R.id.l_rink_is_flooded) ).setImageResource(
	    			isFlooded == 1 ? R.drawable.ic_surface_flooded_on : R.drawable.ic_surface_flooded_off
	    	);
	    	( (ImageView) view.findViewById(R.id.l_rink_is_resurfaced) ).setImageResource(
	    			isResurfaced == 1 ? R.drawable.ic_surface_resurfaced_on : R.drawable.ic_surface_resurfaced_off 
	    	);
	    }
	    

	    /*
	    ( (TextView) view.findViewById(R.id.l_rink_is_favorite) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_FAVORITE ) )  
	    );
	    */
	    ( (TextView) view.findViewById(R.id.l_park_name) ).append( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_NAME ) )  
	    );
	    /*
	    ( (TextView) view.findViewById(R.id.l_park_geo_id) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_GEO_ID ) )  
	    );
	    */
	    ( (TextView) view.findViewById(R.id.l_park_address) ).append( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_ADDRESS ) )  
	    );
	    final String phone = cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_PHONE ) );
	    if ( phone == null ) {
	    	view.findViewById(R.id.l_park_phone).setVisibility( View.GONE );
	    	view.findViewById(R.id.l_rink_call).setVisibility( View.GONE );
	    }
	    else {
	    	( (TextView) view.findViewById(R.id.l_park_phone) ).append( phone );

	    	ImageButton dialerButton = (ImageButton) view.findViewById(R.id.l_rink_call);
	    	dialerButton.setOnClickListener(new OnClickListener() {
	    		@Override
	    		public void onClick(View v) {
	    			Intent intent = new Intent(Intent.ACTION_DIAL , Uri.parse("tel:" + phone));
	    			startActivity(intent);
	    		}
	    	});
	    }

	    /*
	    ( (TextView) view.findViewById(R.id.l_park_is_chalet) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_IS_CHALET ) )  
	    );
	    ( (TextView) view.findViewById(R.id.l_park_is_caravan) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_IS_CARAVAN ) )  
	    );
	    */
	    ( (TextView) view.findViewById(R.id.l_borough_name) ).setText( 
	    		cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_BOROUGHS_NAME ) )  
	    );
	    String remarks = cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS ) );
	    if ( ( remarks == null ) || ( remarks.trim().length() == 0 ) ) {
	    	( (TextView) view.findViewById(R.id.l_borough_remarks) ).setVisibility( View.GONE );	
	    }
	    else {
	    	( (TextView) view.findViewById(R.id.l_borough_remarks) ).setText( remarks );	    	
	    }
	    

	    String updatedAt =  cursor.getString( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_BOROUGHS_UPDATED_AT ) );
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss" );  
	    try {
	    	dateFormat.setTimeZone( TimeZone.getTimeZone("EST") );
			Long millis = dateFormat.parse( updatedAt ).getTime(); 
			updatedAt = (String) DateUtils.getRelativeTimeSpanString( millis , System.currentTimeMillis() , 0 , DateUtils.FORMAT_ABBREV_RELATIVE );
		} catch (java.text.ParseException e) { }
	    ( (TextView) view.findViewById(R.id.l_borough_updated_at) ).append( updatedAt );


	    // The color of the star for Favorites button
	    ( ( ImageButton ) view.findViewById(R.id.l_rink_favorites) ).setImageResource( 
	    		cursor.getInt( cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_IS_FAVORITE ) ) == 1 ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off 
	    );
	    
	    // Close here to avoid problems!
    	mDbHelper.closeDb();	
    	cursor.close();
	    
	    
	    /**
	     * Define the bottom menu click listeners
	     */
	    ( (ImageButton) view.findViewById(R.id.l_rink_favorites) ).setOnClickListener(new OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		mDbHelper.openDb();
	    		boolean isFavorite = mDbHelper.toggleFavorites( rinkId );
	    		mDbHelper.closeDb();

	    		((ImageButton) v).setImageResource( 
	    				isFavorite  ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off 
	    		);
	    	}
	    });

    	( (ImageButton) view.findViewById(R.id.l_rink_refresh) ).setOnClickListener(new OnClickListener() {

    		@Override
    		public void onClick(View v) {
    			
    			Context context = v.getContext();
    			mDbHelper.openDb();
    			final ProgressDialog progressDialog = ProgressDialog.show( context  , null ,  context.getResources().getText( R.string.dialog_updating_conditions ) , true , true);
    			
    			new Thread(new Runnable(){
    				public void run(){
    					
    					mDbHelper.openDataUpdateConditions();
    					
    					progressDialog.dismiss();
    				}
    			} ).start();
    			mDbHelper.closeDb();
    			
//    			mDbHelper.openDataUpdateConditions( false );
    		}

    		
    	});

    	( (ImageButton) view.findViewById(R.id.l_rink_mapmode) ).setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			Intent intent = new Intent( getApplicationContext() , PatinoiresGMaps.class );
    			startActivity( intent );
    		}
    	});

    	
	    return view;
    }
    

	@Override
	protected void onResume() {
		super.onResume();
		
        LayoutInflater mInflater = LayoutInflater.from( this );
        View view = mInflater.inflate( R.layout.rinks_details , null , false );
		
		Bundle extras = getIntent().getExtras();
    	long rinkId = extras.getLong("rinkId");
    	String interfaceLanguage = extras.getString("interfaceLanguage");
		
//    	mDbHelper.openDb();        	
    	view = fillData( view , rinkId , interfaceLanguage );
		
	}
/*
	protected void onDestroy() {
		mDbHelper.closeDb();
		super.onDestroy();
	}

	protected void onPause() {
		mDbHelper.closeDb();
		super.onPause();
	}
*/
	
    
}
