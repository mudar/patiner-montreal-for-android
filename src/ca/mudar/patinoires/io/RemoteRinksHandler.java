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

import ca.mudar.patinoires.providers.RinksContract.Boroughs;
import ca.mudar.patinoires.providers.RinksContract.BoroughsColumns;
import ca.mudar.patinoires.providers.RinksContract.Parks;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.utils.ApiStringHelper;
import ca.mudar.patinoires.utils.Lists;
import ca.mudar.patinoires.utils.Const.DbValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class RemoteRinksHandler extends JsonHandler {
    private static final String TAG = "RemoteRinksHandler";

    private ProgressDialog dialog;

    public RemoteRinksHandler(String authority) {
        super(authority);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONTokener jsonTokener,
            ContentResolver resolver) throws JSONException, IOException {

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        /**
         * Using 3 different builders for readability! 
         */
        ContentProviderOperation.Builder builderBoroughs;
        ContentProviderOperation.Builder builderParks;
        ContentProviderOperation.Builder builderRinks;

        boolean importResult = true;
        CharSequence createdAt = DateFormat.format(DbValues.DATE_FORMAT, new Date());

        JSONArray rinks = new JSONArray(jsonTokener);
        final int totalRinks = rinks.length();

        if (totalRinks == 0) {
            return batch;
        }
        else if (dialog != null) {
            dialog.setProgress(0);
            dialog.setMax(totalRinks);
        }
        
        String rinkName;
        String rinkDesc;
        String rinkDescEnglish;
        String[] splitName;

        JSONObject rink;
        JSONObject borough;
        JSONObject park;
        JSONObject geocoding;

        for (int i = 0; i < totalRinks; i++) {
            if (dialog != null) {
                dialog.incrementProgressBy(1);
            }

            /**
             * Get rink info and clean name and description. English description
             * is translated manually!
             */
            try {
                rink = (JSONObject) rinks.getJSONObject(i).get(RemoteTags.OBJECT_RINK);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }
            // Log.v(TAG, "Patinoire n." + i + ", (_id = " + rink.optInt("id"
            // ) + ") : " + rink.optString("name"));

            builderRinks = ContentProviderOperation.newInsert(Rinks.CONTENT_URI);
            
            splitName = rink.optString(RemoteTags.RINK_NAME).split(",");
            rinkName = splitName[1].replace(RemoteValues.RINK_TYPE_PSE, "")
                    .replace(RemoteValues.RINK_TYPE_PPL, "").replace(RemoteValues.RINK_TYPE_PP, "")
                    .trim();
            rinkDesc = splitName[0].trim();
            rinkDescEnglish = ApiStringHelper.translateRinkDescription(rinkDesc);

            builderRinks.withValue(RinksColumns.RINK_ID, rink.optString(RemoteTags.RINK_ID));
            builderRinks.withValue(RinksColumns.RINK_PARK_ID,
                    rink.optString(RemoteTags.RINK_PARK_ID));
            builderRinks.withValue(RinksColumns.RINK_KIND_ID,
                    rink.optString(RemoteTags.RINK_KIND_ID));
            builderRinks.withValue(RinksColumns.RINK_NAME, rinkName);
            builderRinks.withValue(RinksColumns.RINK_DESC_FR, rinkDesc);
            builderRinks.withValue(RinksColumns.RINK_DESC_EN, rinkDescEnglish);
            builderRinks.withValue(RinksColumns.RINK_IS_CLEARED,
                    rink.optString(RemoteTags.RINK_IS_CLEARED).equals(RemoteValues.BOOLEAN_TRUE));
            builderRinks.withValue(RinksColumns.RINK_IS_FLOODED,
                    rink.optString(RemoteTags.RINK_IS_FLOODED).equals(RemoteValues.BOOLEAN_TRUE));
            builderRinks.withValue(RinksColumns.RINK_IS_RESURFACED,
                    rink.optString(RemoteTags.RINK_IS_RESURFACED)
                            .equals(RemoteValues.BOOLEAN_TRUE));
            builderRinks.withValue(
                    RinksColumns.RINK_CONDITION,
                    ApiStringHelper.getConditionIndex(rink.optString(RemoteTags.RINK_IS_OPEN),
                            rink.optString(RemoteTags.RINK_CONDITION)));
            builderRinks.withValue(RinksColumns.RINK_CREATED_AT, createdAt);

            batch.add(builderRinks.build());

            /**
             * Get Borough info
             */
            if (rink.optString(RemoteTags.RINK_BOROUGH_ID).equals(RemoteValues.STRING_NULL)) {
                // Skip empty borough
                continue;
            }

            try {
                borough = (JSONObject) rink.get(RemoteTags.OBJECT_BOROUGH);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }
            // Log.i(TAG, "Borough n." + i + ", (_id = " + borough.optInt(
            // "id") + ") : " + borough.optString("name"));

            builderBoroughs = ContentProviderOperation.newInsert(Boroughs.CONTENT_URI);

            builderBoroughs.withValue(BoroughsColumns.BOROUGH_ID,
                    borough.optString(RemoteTags.BOROUGH_ID));
            builderBoroughs.withValue(BoroughsColumns.BOROUGH_NAME,
                    borough.optString(RemoteTags.BOROUGH_NAME));
            builderBoroughs.withValue(BoroughsColumns.BOROUGH_REMARKS,
                    borough.optString(RemoteTags.BOROUGH_REMARKS));
            builderBoroughs.withValue(BoroughsColumns.BOROUGH_UPDATED_AT,
                    borough.optString(RemoteTags.BOROUGH_UPDATED_AT));
            builderBoroughs.withValue(BoroughsColumns.BOROUGH_CREATED_AT, createdAt);

            batch.add(builderBoroughs.build());

            /**
             * Get Park info
             */
            if (rink.optString(RemoteTags.RINK_PARK_ID).equals(RemoteValues.STRING_NULL)) {
                // Skip empty park
                continue;
            }

            try {
                park = (JSONObject) rink.get(RemoteTags.OBJECT_PARK);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }
            // Log.i( TAG , "Parc  n." + i + ", (_id = " + park.optInt( "id" ) +
            // ") : " + park.optString("name") );

            /**
             * Get Geocoding info of the park
             */
            try {
                geocoding = (JSONObject) park.get(RemoteTags.OBJECT_GEOCODING);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }
            // Log.i(TAG, "Geocoding n." + i + ". parks.geocoding_id = " +
            // geocoding.optInt("id") + ", parks.geo_lat " +
            // geocoding.optString("lat") + ", parks.geo_lng " +
            // geocoding.optString("lng"));

            builderParks = ContentProviderOperation.newInsert(Parks.CONTENT_URI);

            builderParks.withValue(ParksColumns.PARK_ID, park.optString(RemoteTags.PARK_ID));
            builderParks.withValue(ParksColumns.PARK_BOROUGH_ID,
                    borough.optString(RemoteTags.BOROUGH_ID));
            builderParks.withValue(ParksColumns.PARK_NAME, park.optString(RemoteTags.PARK_NAME));
            builderParks.withValue(ParksColumns.PARK_GEO_ID,
                    geocoding.optString(RemoteTags.GEOCODING_ID));
            builderParks.withValue(ParksColumns.PARK_GEO_LAT,
                    geocoding.optString(RemoteTags.GEOCODING_LAT));
            builderParks.withValue(ParksColumns.PARK_GEO_LNG,
                    geocoding.optString(RemoteTags.GEOCODING_LNG));
            if (park.optString(RemoteTags.PARK_ADDRESS).trim().toLowerCase() != RemoteValues.STRING_NULL) {
                builderParks.withValue(ParksColumns.PARK_ADDRESS,
                        park.optString(RemoteTags.PARK_ADDRESS));
            }
            if (park.optString(RemoteTags.PARK_PHONE).trim().toLowerCase() != RemoteValues.STRING_NULL) {
                builderParks.withValue(ParksColumns.PARK_PHONE,
                        park.optString(RemoteTags.PARK_PHONE));
            }
            builderParks
                    .withValue(
                            ParksColumns.PARK_IS_CHALET,
                            park.optString(RemoteTags.PARK_IS_CHALET).equals(
                                    RemoteValues.BOOLEAN_TRUE) ? 1
                                    : 0);
            builderParks.withValue(ParksColumns.PARK_IS_CARAVAN,
                    park.optString(RemoteTags.PARK_IS_CARAVAN)
                            .equals(RemoteValues.BOOLEAN_TRUE) ? 1
                            : 0);
            builderParks.withValue(ParksColumns.PARK_CREATED_AT, createdAt);

            batch.add(builderParks.build());

        }

        return batch;
    }

    /** Remote columns */
    private static interface RemoteTags {
        final String OBJECT_RINK = "rink";
        final String OBJECT_BOROUGH = "borough";
        final String OBJECT_PARK = "park";
        final String OBJECT_GEOCODING = "geocoding";

        final String RINK_ID = "id";
        final String RINK_NAME = "name";
        final String RINK_PARK_ID = "park_id";
        final String RINK_BOROUGH_ID = "borough_id";
        final String RINK_KIND_ID = "kind_id";
        final String RINK_IS_CLEARED = "cleared";
        final String RINK_IS_FLOODED = "flooded";
        final String RINK_IS_RESURFACED = "resurfaced";
        final String RINK_IS_OPEN = "open";
        final String RINK_CONDITION = "condition";

        final String BOROUGH_ID = "id";
        final String BOROUGH_NAME = "name";
        final String BOROUGH_REMARKS = "remarks";
        // Following field name may be confusing!
        final String BOROUGH_UPDATED_AT = "posted_at";

        final String PARK_ID = "id";
        final String PARK_NAME = "name";
        final String PARK_ADDRESS = "address";
        final String PARK_PHONE = "telephone";
        final String PARK_IS_CHALET = "chalet";
        final String PARK_IS_CARAVAN = "caravan";

        final String GEOCODING_ID = "id";
        final String GEOCODING_LAT = "lat";
        final String GEOCODING_LNG = "lng";
    }

    private static interface RemoteValues {
        final String RINK_TYPE_PSE = "(PSE)";
        final String RINK_TYPE_PPL = "(PPL)";
        final String RINK_TYPE_PP = "(PP)";
        final String BOOLEAN_TRUE = "true";
        // final String BOOLEAN_FALSE = "false";
        final String STRING_NULL = "null";
    }
}
