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

import ca.mudar.patinoires.utils.ActivityHelper;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;

public class MainActivity extends FragmentActivity {

    private String lang;
    protected PatinoiresApp mAppHelper;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppHelper = (PatinoiresApp) getApplicationContext();
        
        lang = mAppHelper.getLanguage();
        mAppHelper.updateUiLanguage();
        
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (!lang.equals(mAppHelper.getLanguage())) {
            lang = mAppHelper.getLanguage();
            this.onConfigurationChanged();
        }
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_dashboard, menu);

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }
    
    /**
     * Update the interface language, independently from the phone's UI
     * language. This does not override the parent function because the Manifest
     * does not include configChanges.
     */
    private void onConfigurationChanged() {
        View root = findViewById(android.R.id.content).getRootView();

        ((Button) root.findViewById(R.id.home_btn_skating))
                .setText(R.string.btn_skating);
        ((Button) root.findViewById(R.id.home_btn_hockey))
                .setText(R.string.btn_hockey);
        ((Button) root.findViewById(R.id.home_btn_map))
                .setText(R.string.btn_map);
        ((Button) root.findViewById(R.id.home_btn_favorites))
                .setText(R.string.btn_favorites);

        invalidateOptionsMenu();
    }
}
