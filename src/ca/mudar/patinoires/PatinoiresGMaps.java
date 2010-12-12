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

import android.os.Bundle;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class PatinoiresGMaps extends MapActivity {
	
    private MapView mapView;
    private MyLocationOverlay myLocationOverlay;
    private MapController mapController;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.map);

	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
        mapView.setClickable(true);
        mapView.setEnabled(true);
	    mapView.setBuiltInZoomControls(true);

	    mapController = mapView.getController();
	    mapController.setZoom(15);

    	initMap();
	}

    private void initMap() {
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        myLocationOverlay.enableCompass();
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        myLocationOverlay.runOnFirstFix( new Runnable(  ) {
            public void run() {
            	mapController.animateTo( myLocationOverlay.getMyLocation() );
             }
        } );
    } 

	@Override
	protected void onResume() {
		myLocationOverlay.enableMyLocation();
		super.onResume();
	}

	@Override
	protected void onPause() {
		myLocationOverlay.disableMyLocation();
		super.onPause();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}
}
