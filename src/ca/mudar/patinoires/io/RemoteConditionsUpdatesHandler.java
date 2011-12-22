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
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.utils.Const;
import ca.mudar.patinoires.utils.Const.DbValues;
import ca.mudar.patinoires.utils.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class RemoteConditionsUpdatesHandler extends JsonHandler {
    private static final String TAG = "RemoteConditionsUpdatesHandler";

    public RemoteConditionsUpdatesHandler(String authority) {
        super(authority);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONTokener jsonTokener,
            ContentResolver resolver) throws JSONException, IOException {

        Random rand = new Random();

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        /**
         * Using 2 different builders for readability!
         */
        ContentProviderOperation.Builder builderBoroughs;
        ContentProviderOperation.Builder builderRinks;

        boolean importResult = true;
        CharSequence createdAt = DateFormat.format(DbValues.DATE_FORMAT, new Date());

        JSONArray boroughs = new JSONArray(jsonTokener);
        final int totalBoroughs = boroughs.length();
        int totalRinks = 0;

        if (totalBoroughs == 0) {
            return batch;
        }

        JSONObject borough;
        JSONArray rinks;
        JSONObject rink;

        for (int i = 0; i < totalBoroughs; i++) {
            /**
             * Get Borough remarks and update_at
             */
            try {
                borough = (JSONObject) boroughs.getJSONObject(i).get(RemoteTags.OBJECT_BOROUGH);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }
            // Log.i(TAG,"Borough n." + i + ", (_id = " + borough.optInt("id") +
            // ") : "+ borough.optString("name"));

            builderBoroughs = ContentProviderOperation.newUpdate(Boroughs.CONTENT_URI);

            builderBoroughs.withValue(BoroughsColumns.BOROUGH_REMARKS,
                    borough.optString(RemoteTags.BOROUGH_REMARKS));
            builderBoroughs.withValue(BoroughsColumns.BOROUGH_UPDATED_AT,
                    borough.optString(RemoteTags.BOROUGH_UPDATED_AT));

            batch.add(builderBoroughs.build());

            /**
             * Get the borough's rinks
             */
            try {
                rinks = new JSONArray(borough.optString(RemoteTags.ARRAY_RINKS));
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
                importResult = false;
                continue;
            }

            totalRinks = rinks.length();
            for (int j = 0; j < totalRinks; j++) {
                /**
                 * Get Rink updates: cleared, flooded, resurfaced and condition
                 * (0.Excellent to 3.Closed)
                 */
                try {
                    rink = (JSONObject) rinks.getJSONObject(j);
                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                    importResult = false;
                    continue;
                }

                builderRinks = ContentProviderOperation.newUpdate(Rinks.CONTENT_URI);

                builderRinks.withValue(RinksColumns.RINK_IS_CLEARED,
                        rink.optString(RemoteTags.RINK_IS_CLEARED)
                                .equals(RemoteValues.BOOLEAN_TRUE));
                builderRinks.withValue(RinksColumns.RINK_IS_FLOODED,
                        rink.optString(RemoteTags.RINK_IS_FLOODED)
                                .equals(RemoteValues.BOOLEAN_TRUE));
                builderRinks.withValue(RinksColumns.RINK_IS_RESURFACED,
                        rink.optString(RemoteTags.RINK_IS_RESURFACED)
                                .equals(RemoteValues.BOOLEAN_TRUE));
                // builderRinks.withValue(
                // RinksColumns.RINK_CONDITION,
                // ApiStringHelper.getConditionIndex(rink.optString(RemoteTags.RINK_IS_OPEN),
                // rink.optString(RemoteTags.RINK_CONDITION)));
                int condition = rand.nextInt(4);
                builderRinks.withValue(RinksColumns.RINK_CONDITION, condition);
                if ((condition == Const.DbValues.CONDITION_EXCELLENT)
                        || (condition == Const.DbValues.CONDITION_GOOD)) {
                    Log.v(TAG, "condition = " + condition);
                }

                builderRinks.withValue(RinksColumns.RINK_CREATED_AT, createdAt);

                batch.add(builderRinks.build());
            }
        }

        return batch;
    }

    /** Remote columns */
    private static interface RemoteTags {
        final String OBJECT_BOROUGH = "borough";
        final String ARRAY_RINKS = "rinks";

        final String RINK_IS_CLEARED = "cleared";
        final String RINK_IS_FLOODED = "flooded";
        final String RINK_IS_RESURFACED = "resurfaced";
        final String RINK_IS_OPEN = "open";
        final String RINK_CONDITION = "condition";

        final String BOROUGH_REMARKS = "remarks";
        // Following field name may be confusing!
        final String BOROUGH_UPDATED_AT = "posted_at";
    }

    private static interface RemoteValues {
        final String BOOLEAN_TRUE = "true";
        // final String BOOLEAN_FALSE = "false";
    }
}
