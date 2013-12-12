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

package ca.mudar.patinoires.googlemap;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;

import ca.mudar.patinoires.Const;

public class GeoHelper {
    private static final String TAG = "GeoHelper";
    private static final int MAX_RESULTS = 5;

    public static Address findAddressFromName(Context c, String name) throws IOException {
        Geocoder geocoder = new Geocoder(c);

        if (!Geocoder.isPresent()) {
            throw new IOException("Service not Present", null);
        }

        List<Address> adr;

        adr = geocoder.getFromLocationName(name, MAX_RESULTS, Const.MAPS_GEOCODER_LIMITS[0],
                Const.MAPS_GEOCODER_LIMITS[1], Const.MAPS_GEOCODER_LIMITS[2],
                Const.MAPS_GEOCODER_LIMITS[3]);

        if (!adr.isEmpty()) {
            for (Address address : adr) {
                if (address.hasLatitude() && address.hasLongitude()) {
                    if (Double.compare(address.getLatitude(), Const.MAPS_GEOCODER_LIMITS[0]) >= 0
                            &&
                            Double.compare(address.getLongitude(), Const.MAPS_GEOCODER_LIMITS[1]) >= 0
                            &&
                            Double.compare(address.getLatitude(), Const.MAPS_GEOCODER_LIMITS[2]) <= 0
                            &&
                            Double.compare(address.getLongitude(), Const.MAPS_GEOCODER_LIMITS[3]) <= 0) {

                        return address;
                    }
                }
            }
        }

        return null;
    }
}
