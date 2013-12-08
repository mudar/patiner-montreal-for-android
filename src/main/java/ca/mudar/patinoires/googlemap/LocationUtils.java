/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modifications:
 * - Copied from IOSched
 * - Renamed package
 * - Renamed variables and removed unused
  */

package ca.mudar.patinoires.googlemap;

import android.text.format.DateUtils;

/**
 * Defines app-wide constants and utilities
 */
public final class LocationUtils {
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    /*
     * Constants for location update parameters
     */
    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_ACTIVE_IN_MILLIS =
            DateUtils.SECOND_IN_MILLIS * 5;
    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_ACTIVE_IN_MILLIS =
            DateUtils.SECOND_IN_MILLIS * 2;
    // Passive Update interval in milliseconds
    public static final long UPDATE_INTERVAL_PASSIVE_IN_MILLIS =
            DateUtils.MINUTE_IN_MILLIS * 15;   // fifteen minutes. This is equal to Const.MAX_TIME
    // A fast ceiling of passive update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_PASSIVE_IN_MILLIS =
            DateUtils.MINUTE_IN_MILLIS * 5;    // five minutes

}
