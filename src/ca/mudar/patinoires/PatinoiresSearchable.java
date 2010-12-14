package ca.mudar.patinoires;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;



/**
 * The main activity for the dictionary.  Also displays search results triggered by the search
 * dialog.
 */
public class PatinoiresSearchable extends ListActivity {
	private static final String TAG = "PatinoiresSearchable";
	
    private static final int MENU_SEARCH = 1;

//    private TextView mTextView;
//    private ListView mList;
    private PatinoiresOpenData mDbHelper;
    private Cursor cursor;
    
    //TODO
    String interfaceLanguage;
  
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDbHelper = PatinerMontreal.getmDbHelper();
        interfaceLanguage = "en";
                
        
        Intent intent = getIntent();

        setContentView(R.layout.rinks_list);
        
//        mTextView = (TextView) findViewById(R.id.textField);
//        mList = (ListView) findViewById(R.id.list);

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
Log.w( TAG , "word found = " + intent.getDataString());

//long rinkId = intent.getData();
			long rinkId = 1;
			displayDetails( rinkId );

            finish();
 
        } 
        else if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
        	String query = intent.getStringExtra( SearchManager.QUERY );

//        	if ( mDbHelper== null ) {mDbHelper.openDb();}
        	//            mTextView.setText(getString(R.string.search_results, query));
        	fillData( query );
        }

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				displayDetails( arg3 );
			} } );
        
        Log.d( TAG , intent.toString());
        if (intent.getExtras() != null) {
            Log.d( TAG , intent.getExtras().keySet().toString());
        }
    }
    
    private void displayDetails( Long rinkId ) {
		Intent intent = new Intent( getApplicationContext() , PatinoiresDetails.class );
		intent.putExtra( "rinkId" , rinkId );
		intent.putExtra( "interfaceLanguage" , interfaceLanguage );
		startActivity( intent);
    }
    
    private void fillData( String query ) {
    	mDbHelper.openDb();
    	
    	cursor = mDbHelper.searchRinks( query , null );
    	startManagingCursor( cursor );

    	String[] from = new String[] { 
    			PatinoiresDbAdapter.KEY_RINKS_NAME, 
    			( interfaceLanguage.equals( "fr" ) ? PatinoiresDbAdapter.KEY_RINKS_DESC_FR : PatinoiresDbAdapter.KEY_RINKS_DESC_EN ) ,
    			PatinoiresDbAdapter.KEY_BOROUGHS_NAME ,
    			PatinoiresDbAdapter.KEY_BOROUGHS_REMARKS 

    	};
    	int[] to = new int[] { R.id.l_rink_name , R.id.l_rink_desc , R.id.l_borough_name , R.id.l_borough_remarks };

    	RinksListCursorAdapter rinks = new RinksListCursorAdapter(this, R.layout.rinks_list_item , cursor, from, to, mDbHelper.isSortOnBorough() );
    	setListAdapter( rinks );

    	mDbHelper.closeDb();
    }

    protected void onResume() {
    	super.onResume();
    	
    	Intent intent = getIntent();
    	String query = intent.getStringExtra( SearchManager.QUERY );
Log.w( TAG , "onResume query = " + query );
    	fillData(query);
    }
    
}
