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

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class PatinerMontreal extends TabActivity {
	private static final String TAG = "PatinerMontreal";

	public static final String TAB_FAVORITES = "tab_favorites";
	public static final String TAB_SKATING   = "tab_skating";
	public static final String TAB_HOCKEY    = "tab_hockey";
	public static final String TAB_ALL       = "tab_all";

	private static TabHost tabHost; 
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		Resources res = getResources();
		tabHost = getTabHost();
		TabHost.TabSpec spec;
		
		Intent intent = new Intent().setClass(this, PatinoiresList.class);
		spec = tabHost.newTabSpec( TAB_FAVORITES ).setIndicator( res.getText( R.string.tab_favorites ) ).setContent(intent);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec( TAB_SKATING ).setIndicator( res.getText( R.string.tab_skating ) ).setContent(intent);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec( TAB_HOCKEY ).setIndicator( res.getText( R.string.tab_hockey ) ).setContent(intent);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec( TAB_ALL ).setIndicator( res.getText( R.string.tab_all ) ).setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab( 0 );
	}
	
	public static String getCurrentTabTag() {
		return tabHost.getCurrentTabTag();
	}
	
	public static void setCurrentTabAllRinks() {
		tabHost.setCurrentTab( 3 );
	}
	
// TODO: remove this duplicate function
	private boolean isFirstLaunch() {
		SharedPreferences settings = getSharedPreferences( PatinoiresList.PREFS_NAME , MODE_PRIVATE );
		
		return ( settings.getString( "language" , null ) == null );
	}
}