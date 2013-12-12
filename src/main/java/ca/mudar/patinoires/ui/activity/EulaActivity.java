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


package ca.mudar.patinoires.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.utils.EulaHelper;

public class EulaActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_eula);

        final ActionBar ab = getSupportActionBar();
        if (EulaHelper.hasAcceptedEula(this)) {
            ab.setDisplayHomeAsUpEnabled(true);
        } else {
            ab.setHomeButtonEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        }

        // Remove accept/decline buttons if license is already accepted
        if ( EulaHelper.hasAcceptedEula(this)) {
            findViewById(R.id.eula_buttons).setVisibility(View.GONE);
        }

        WebView v = (WebView) findViewById(R.id.webview);
        v.setWebViewClient(new MyWebViewClient());
        v.setVisibility(View.VISIBLE);
        v.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        v.loadUrl("file:///android_asset/" + Const.LocalAssets.LICENSE);
    }

    public void acceptEula(View v) {
        Intent intent = new Intent();
        setResult(Const.INTENT_REQ_CODE_EULA, intent);
        setResult(RESULT_OK, intent);
        this.finish();
    }

    public void declineEula(View v) {
        Intent intent = new Intent();
        setResult(Const.INTENT_REQ_CODE_EULA, intent);
        setResult(RESULT_CANCELED, intent);
        this.finish();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

}