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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


/**
 * Based on "Custom CursorAdapters" article 
 * @author jwei512
 * {@link http://thinkandroid.wordpress.com/2010/01/11/custom-cursoradapters/}
 */
public class RinksListCursorAdapter extends SimpleCursorAdapter implements Filterable {
	protected static final String TAG = "RinksListCursorAdapter";

	private boolean showBorough;
	private int mColFieldBorough;
//	private int mColFieldBoroughRemarks;
	private int mColFieldCondition;
	private int mColFieldKindId;


	public RinksListCursorAdapter (Context context, int layout, Cursor cursor , String[] from, int[] to , boolean showBorough ) {
		super( context , layout , cursor , from , to );

		this.showBorough = showBorough;

		mColFieldBorough        = cursor.getColumnIndex( PatinoiresDbAdapter.KEY_PARKS_BOROUGH_ID );
//		mColFieldBoroughRemarks = cursor.getColumnIndex( PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS );
		mColFieldCondition      = cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_CONDITION );
		mColFieldKindId         = cursor.getColumnIndex( PatinoiresDbAdapter.KEY_RINKS_KIND_ID);
		
	}

	public static int getConditionColor( int condition ) {
		if ( condition == PatinoiresDbAdapter.CONDITION_EXCELLENT_INDEX ) {	
			return Color.parseColor( "#00CC00" );
		}
		else if ( condition == PatinoiresDbAdapter.CONDITION_GOOD_INDEX ) {	
			return Color.parseColor( "#FF9900" );
		}
		else if ( condition == PatinoiresDbAdapter.CONDITION_BAD_INDEX ) {	
			return Color.parseColor( "#CC0000" );
		}
		else {	// Closed	
			return Color.parseColor( "#808080" );
		}
	}


	private boolean isNewBorough(Cursor cursor, int position) {
		if ( position < 1 ) { return true; }	// First is always a new borough

		long nBoroughCurrent = cursor.getInt( mColFieldBorough );

		cursor.moveToPosition( position - 1 );
		long nBoroughPrevious = cursor.getInt(mColFieldBorough);

		cursor.moveToPosition(position);	// Restore cursor position

		return ( nBoroughPrevious !=  nBoroughCurrent );
	}


	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		return super.newView(context, cursor, parent);
	}


	@Override
	public void bindView( View view , Context context , Cursor cursor ) {
		int visibility;
		int position = cursor.getPosition();
		if ( showBorough && isNewBorough( cursor , position ) ) {
			visibility = View.VISIBLE;
		}
		else {
			visibility = View.GONE;
		}

		TextView textViewBorough = (TextView) view.findViewById( R.id.l_borough_name );
		textViewBorough.setVisibility( visibility );

		TextView textViewRemarks = (TextView) view.findViewById( R.id.l_borough_remarks );
		textViewRemarks.setVisibility( visibility );
		//TODO fix the layout_alignWithParentIfMissing issue on 1.5
		/*
		String boroughRemarks = cursor.getString( mColFieldBoroughRemarks );
		if ( boroughRemarks.trim().length() > 0 ) {
			textViewRemarks.setVisibility( visibility );				
		}
		else {
			textViewRemarks.setVisibility( View.GONE );
		}
		 */
		TextView textViewSeparator = (TextView) view.findViewById( R.id.l_borough_separator );
		textViewSeparator.setVisibility( visibility );

		int conditionIndex = cursor.getInt( mColFieldCondition );
		int imageResource;
		if ( cursor.getInt( mColFieldKindId ) == PatinoiresDbAdapter.OPEN_DATA_INDEX_PSE ) {
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
		( (ImageView) view.findViewById( R.id.l_rink_kind_id ) ).setImageDrawable( context.getResources().getDrawable(imageResource) );
		
		super.bindView(view, context, cursor);
	}

	/*
    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
Log.w(TAG, " runQueryOnBackgroundThread ");
        if (getFilterQueryProvider() != null) { return getFilterQueryProvider().runQuery(constraint); }
Log.w(TAG, " searchRinks: " + constraint );
PatinoiresOpenData mDbHelper = new PatinoiresOpenData( context );
return mDbHelper.searchRinks( constraint.toString() , "" );


        StringBuilder buffer = null;
        String[] args = null;
        if (constraint != null) {
            buffer = new StringBuilder();
            buffer.append("UPPER(");
            buffer.append( PatinoiresDbAdapter.KEY_RINKS_NAME );
            buffer.append(") GLOB ?");
            args = new String[] { constraint.toString().toUpperCase() + "*" };
        }
        PatinoiresOpenData mDbHelper = new PatinoiresOpenData( context );
        String sqlConditionsFilter = "";
    	boolean[] conditions = mDbHelper.getConditions();
    	for (int i = 0; i < conditions.length ; i++) {
    		if ( conditions[i] ) { sqlConditionsFilter += " OR condition = " + i; }
    	}

        return mDbHelper.query( "SELECT b.* , p.* , r.* , ( f._id IS NOT NULL ) AS " + PatinoiresDbAdapter.KEY_RINKS_IS_FAVORITE
    			+ " FROM " + PatinoiresDbAdapter.TABLE_NAME_BOROUGHS + " AS b "
    			+ " JOIN " + PatinoiresDbAdapter.TABLE_NAME_PARKS + " AS p ON p.borough_id = b._id "
    			+ " JOIN " + PatinoiresDbAdapter.TABLE_NAME_RINKS + " AS r ON r.park_id = p._id " 
    			+ " LEFT JOIN " + PatinoiresDbAdapter.TABLE_NAME_FAVORITES + " AS f ON r._id = f.rink_id "
    			+ " WHERE 0 " + sqlConditionsFilter 
    			+ " ORDER BY " + PatinoiresDbAdapter.KEY_RINKS_NAME , 
    			null );

    }
	 */
}
