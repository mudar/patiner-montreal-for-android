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

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
//import android.util.Log;
import ca.mudar.patinoires.PatinoiresDbAdapter.MapRink;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class PatinoiresGMaps extends MapActivity {
	private static final String TAG = "PatinoiresGMaps";

	private static int DEFAULT_ZOOM = 15; 

	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;
	private MapController mapController;
	private LocationManager myLocationManager;

	private PatinoiresOpenData mDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		mapController = mapView.getController();
		mapController.setZoom( DEFAULT_ZOOM );

		myLocationManager = (LocationManager)getSystemService(this.LOCATION_SERVICE);

		initMap();
	}

	private void initMap() {
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		mapView.getOverlays().add( myLocationOverlay );

		initialAnimateToPoint();

		mDbHelper = PatinerMontreal.getmDbHelper();
		mDbHelper.openDb();
		ArrayList<MapRink> rinksList = mDbHelper.fetchRinksForMap( "" , "" );
		mDbHelper.closeDb();

		//		Log.w( TAG , "NB rinks = " + rinksList.size() );

		List<Overlay> mapOverlays = mapView.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.ic_map_default_marker);
		PatinoiresItemizedOverlay itemizedoverlay = new PatinoiresItemizedOverlay(drawable , mapView.getContext() );

		for (MapRink r : rinksList) {
			OverlayItem overlayitem = new OverlayItem( r.geoPoint , r.rink, r.descriptionFr );
			itemizedoverlay.addOverlay(overlayitem);
			//Log.w( TAG , "Rink = " + r.rink + " Geopoint = " +r.geoPoint.toString()  );
		}
		mapOverlays.add(itemizedoverlay);

	} 

	/**
	 * Get the geopoint where the map should first be centered. Current location if available, otherwise MTL-centre
	 * If user is not on the Island, display MTL-centre
	 * @return GeoPoint 
	 */
	private void initialAnimateToPoint() {
		List<String > enabledProviders = myLocationManager.getProviders( true );

		if ( enabledProviders.contains( LocationManager.NETWORK_PROVIDER ) ) {
			// Display user current location
			myLocationOverlay.runOnFirstFix( new Runnable(  ) {
				public void run() {
					mapController.animateTo( myLocationOverlay.getMyLocation() );
				}
			} );
		}
		else {
			mapController.setZoom( DEFAULT_ZOOM - 3 );
			// Display city centre
			String coordinates[] = { "45.5", "-73.666667" };
			double lat = Double.parseDouble(coordinates[0]);
			double lng = Double.parseDouble(coordinates[1]);

			GeoPoint animateToPoint = new GeoPoint( (int) (lat * 1E6) , (int) (lng * 1E6) ); 
			mapController.setCenter( animateToPoint );
		}
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
