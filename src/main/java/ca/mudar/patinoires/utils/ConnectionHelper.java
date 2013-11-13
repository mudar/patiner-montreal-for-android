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

package ca.mudar.patinoires.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;

public class ConnectionHelper {
    public static boolean hasConnection(final Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        } else {
            return networkInfo.isConnected();
        }
    }

    public static void showDialogNoConnection(final Activity activity) {
        PatinoiresApp mAppHelper = (PatinoiresApp) activity.getApplicationContext();

        mAppHelper.showToastText(R.string.toast_network_connection_error, Toast.LENGTH_LONG);
    }

}
