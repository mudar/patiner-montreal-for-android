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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.ui.fragment.SearchFragment;

public class SearchableActivity extends BaseActivity implements
        SearchFragment.OnSearchResultClickListener {
    private static final String TAG = "SearchableActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            SearchFragment searchFragment = new SearchFragment(true);
            fm.beginTransaction().add(android.R.id.content, searchFragment, Const.TAG_FRAGMENT_SEARCH).commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);

        FragmentManager fm = getSupportFragmentManager();
        SearchFragment searchFragment = (SearchFragment) fm.findFragmentByTag(Const.TAG_FRAGMENT_SEARCH);

        searchFragment.createRinksList();
    }

    /**
     * Implementation of SearchFragment.OnSearchResultClickListener
     * Updates the fragment content onNewIntent
     */
    @Override
    public void updateSearchIntent(String query) {
        final Bundle bundle = new Bundle();
        bundle.putString(SearchManager.EXTRA_DATA_KEY, query);

        final Intent intent = getIntent();
        intent.replaceExtras(bundle);
        setIntent(intent);
    }
}

