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

import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class PatinoiresPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getPreferenceManager().setSharedPreferencesName( PatinoiresList.PREFS_NAME );

		addPreferencesFromResource(R.xml.preferences);
		

	    PreferenceScreen credits_about = (PreferenceScreen) findPreference("prefs_credits_about");
	    credits_about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	        	displayInfoDialog( R.layout.about );
	            return false;
	        }
	    });
	    
	    PreferenceScreen credits_links = (PreferenceScreen) findPreference("prefs_credits_links");
	    credits_links.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	        	displayInfoDialog( R.layout.links );
	            return false;
	        }
	    });

		
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
		.setInverseBackgroundForced(true)
		.setPositiveButton( android.R.string.ok , null )
		.show();
	}

}
