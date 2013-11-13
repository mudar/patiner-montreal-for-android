/*
 * Copyright 2011 Google Inc.
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
 * - Removed reference to Blocks and Tracks
 * - Replaced ScheduleContract by SecurityContract  
 */

package ca.mudar.patinoires.utils;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import ca.mudar.patinoires.io.XmlHandler;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.SyncColumns;

/**
 * Various utility methods used by {@link XmlHandler} implementations.
 */
public class ParserUtils {
    protected static final String TAG = "ParserUtils";

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

    /** Used to split a comma-separated string. */
    private static final Pattern sCommaPattern = Pattern.compile("\\s*,\\s*");

    private static Time sTime = new Time();
    private static XmlPullParserFactory sFactory;

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if (input == null)
            return null;
        if (stripParen) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input).replaceAll("");
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

    /**
     * Split the given comma-separated string, returning all values.
     */
    public static String[] splitComma(CharSequence input) {
        if (input == null)
            return new String[0];
        return sCommaPattern.split(input);
    }

    /**
     * Build and return a new {@link XmlPullParser} with the given
     * {@link InputStream} assigned to it.
     */
    public static XmlPullParser newXmlPullParser(InputStream input) throws XmlPullParserException {
        if (sFactory == null) {
            sFactory = XmlPullParserFactory.newInstance();
        }
        final XmlPullParser parser = sFactory.newPullParser();
        parser.setInput(input, null);
        return parser;
    }

    /**
     * @author Andrew Pearson 
     * {@link http://blog.andrewpearson.org/2010/07/android-why-to-use-json-and-how-to-use.html}
     * @param archiveQuery URL of JSON resources
     * @return String Raw content, requires use of JSONArray() and
     *         getJSONObject()
     * @throws IOException
     * @throws JSONException
     */
    public static JSONTokener newJsonTokenerParser(InputStream input) throws IOException {
        String queryResult = "";

        BufferedInputStream bis = new BufferedInputStream(input);
        ByteArrayBuffer baf = new ByteArrayBuffer(50);
        int read = 0;
        int bufSize = 512;
        byte[] buffer = new byte[bufSize];
        while (true) {

            read = bis.read(buffer);
            if (read == -1) {
                break;
            }
            baf.append(buffer, 0, read);
        }
        queryResult = new String(baf.toByteArray());

        JSONTokener data = new JSONTokener(queryResult);
        return data;
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String time) {
        sTime.parse3339(time);
        return sTime.toMillis(false);
    }

    /**
     * Query and return the {@link SyncColumns#UPDATED} time for the requested
     * {@link Uri}. Expects the {@link Uri} to reference a single item.
     */
    public static long queryItemUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = {
                SyncColumns.UPDATED
        };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return RinksContract.UPDATED_NEVER;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Query and return the newest {@link SyncColumns#UPDATED} time for all
     * entries under the requested {@link Uri}. Expects the {@link Uri} to
     * reference a directory of several items.
     */
    public static long queryDirUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = {
                "MAX(" + SyncColumns.UPDATED + ")"
        };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            cursor.moveToFirst();
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    /** XML tag constants used by the Atom standard. */
    public interface AtomTags {
        String ENTRY = "entry";
        String UPDATED = "updated";
        String TITLE = "title";
        String LINK = "link";
        String CONTENT = "content";

        String REL = "rel";
        String HREF = "href";
    }
}
