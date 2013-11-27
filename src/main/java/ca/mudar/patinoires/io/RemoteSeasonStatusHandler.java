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

package ca.mudar.patinoires.io;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;


public class RemoteSeasonStatusHandler extends JsonHandler {
    private static final String TAG = "RemoteSeasonStatusHandler";

    public RemoteSeasonStatusHandler(String authority) {
        super(authority);
    }

    @Override
    public boolean parse(JSONTokener jsonTokener) throws JSONException, IOException {
        Log.v(TAG, "parse");

        JSONObject seasonInfo = new JSONObject(jsonTokener);

        final boolean isMaintenance = seasonInfo.optBoolean(RemoteTags.MAINTENANCE, false);
        final String seasonStatus = seasonInfo.optString(RemoteTags.SEASON, RemoteValues.SEASON_ON);
        final boolean isSeasonOn = seasonStatus.toLowerCase(Locale.US).equals(RemoteValues.SEASON_ON);

        Log.v(TAG, "isMaintenance = " + isMaintenance);
        Log.v(TAG, "seasonStatus = " + seasonStatus);

        return (isSeasonOn && !isMaintenance);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONTokener jsonTokener, ContentResolver resolver) throws JSONException, IOException {
        return null;
    }


    /**
     * Remote columns
     */
    private static interface RemoteTags {
        final String MAINTENANCE = "maintenance";
        final String SEASON = "season";
    }

    public static interface RemoteValues {
        final String SEASON_ON = "on";
//        final String SEASON_OFF = "off";
//        final String BOOLEAN_TRUE = "true";
//        final String BOOLEAN_FALSE = "false";
    }
}

