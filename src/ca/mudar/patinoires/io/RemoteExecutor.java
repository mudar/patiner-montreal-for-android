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
 * - copied from iosched
 * - renamed package 
 */

package ca.mudar.patinoires.io;

import ca.mudar.patinoires.io.XmlHandler.HandlerException;
import ca.mudar.patinoires.utils.ParserUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Executes an {@link HttpUriRequest} and passes the result as an
 * {@link XmlPullParser} to the given {@link XmlHandler}.
 */
public class RemoteExecutor {
    private static final String TAG = "RemoteExecutor";
    private final HttpClient mHttpClient;
    private final ContentResolver mResolver;

    public RemoteExecutor(HttpClient httpClient, ContentResolver resolver) {
        mHttpClient = httpClient;
        mResolver = resolver;
    }

    /**
     * Execute a {@link HttpGet} request, passing a valid response through
     * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
     */
    public void executeGet(String url, XmlHandler handler) throws HandlerException {
        Log.v(TAG, "executeGet XML");
        final HttpUriRequest request = new HttpGet(url);
        execute(request, handler);
    }

    public void executeGet(String url, JsonHandler handler) throws HandlerException {
        Log.v(TAG, "executeGet JSON");
        final HttpUriRequest request = new HttpGet(url);
        execute(request, handler);
    }

    /**
     * Execute this {@link HttpUriRequest}, passing a valid response through
     * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
     */
    public void execute(HttpUriRequest request, XmlHandler handler) throws HandlerException {
        try {
            final HttpResponse resp = mHttpClient.execute(request);
            final int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new HandlerException("Unexpected server response " + resp.getStatusLine()
                        + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity().getContent();
            try {
                final XmlPullParser parser = ParserUtils.newXmlPullParser(input);
                handler.parseAndApply(parser, mResolver);
            } catch (XmlPullParserException e) {
                throw new HandlerException("Malformed response for " + request.getRequestLine(), e);
            } finally {
                if (input != null)
                    input.close();
            }
        } catch (HandlerException e) {
            throw e;
        } catch (IOException e) {
            throw new HandlerException("Problem reading remote response for "
                    + request.getRequestLine(), e);
        }
    }

    public void execute(HttpUriRequest request, JsonHandler handler) throws HandlerException {
        try {
            final HttpResponse resp = mHttpClient.execute(request);
            final int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new HandlerException("Unexpected server response " + resp.getStatusLine()
                        + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity().getContent();
            try {
                final JSONTokener parser = ParserUtils.newJsonTokenerParser(input);
                handler.parseAndApply(parser, mResolver);
            } finally {
                if (input != null)
                    input.close();
            }
        } catch (HandlerException e) {
            throw e;
        } catch (IOException e) {
            throw new HandlerException("Problem reading remote response for "
                    + request.getRequestLine(), e);
        }

    }
}
