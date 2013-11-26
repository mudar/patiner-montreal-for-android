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

package ca.mudar.patinoires.ui.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import ca.mudar.patinoires.Const;
import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.ui.widget.RinksCursorAdapter;

public class SearchFragment extends ListFragment implements
        SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SearchFragment";
    private Cursor mCursor = null;
    private RinksCursorAdapter mAdapter;
    private OnSearchResultClickListener mListener;
    private String mSearchQuery = "";
    private SearchView mSearchView;
    private boolean mHasInitialIntent = false;
    private TextView mEmptyListView;

    /**
     * Constructor, called when System recreates the Fragment
     */
    public SearchFragment() {
        mHasInitialIntent = false;
    }

    /**
     * Custom Constructor, called when Activity creates the Fragment
     *
     * @param hasNewIntent Should always be true
     */
    public SearchFragment(boolean hasNewIntent) {
        mHasInitialIntent = hasNewIntent;
    }

    /**
     * Attach a listener.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSearchResultClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSearchResultClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_search, null);
        mEmptyListView = (TextView) view.findViewById(android.R.id.empty);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        setListAdapter(null);

        final String RINK_DESC = (((PatinoiresApp) getActivity().getApplicationContext()).getLanguage().equals(Const.PrefsValues.LANG_FR)
                ? RinksContract.RinksColumns.RINK_DESC_FR
                : RinksContract.RinksColumns.RINK_DESC_EN);

        mAdapter = new RinksCursorAdapter(getActivity(),
                R.layout.fragment_list_item_rinks,
                mCursor,
                new String[]{
                        RinksContract.RinksColumns.RINK_NAME, RINK_DESC, RinksContract.ParksColumns.PARK_GEO_DISTANCE,
                        RinksContract.RinksColumns.RINK_ID
                },
                new int[]{
                        R.id.rink_name, R.id.rink_address, R.id.rink_distance
                },
                0, false);
        setListAdapter(mAdapter);

        showRinksList(mHasInitialIntent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_search, menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.menu_text_search);
        initializeSearchView(searchMenuItem);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        final String query = mSearchQuery.trim();

        final String sqlFilter = (query.length() >= Const.Search.MIN_LENGTH_SEARCH ? "%" : "") + query + "%";
        String[] args = new String[]{sqlFilter};

        return new CursorLoader(getActivity().getApplicationContext(),
                RinksContract.Rinks.CONTENT_SEARCH_URI,
                SearchQuery.RINKS_SUMMARY_PROJECTION,
                RinksContract.RinksColumns.RINK_NAME + " LIKE ? ",
                args,
                RinksContract.Rinks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = mAdapter.getCursor();
        c.moveToPosition(position);

        int rinkId = c.getInt(SearchQuery.RINK_ID);
        mListener.goRinkDetails(rinkId);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchView.clearFocus();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query != null) {
            // Toggle the emptyList message and update list content
            if (query.isEmpty()) {
                mEmptyListView.setText(R.string.search_listview_hint);
            } else {
                mEmptyListView.setText(R.string.empty_list_Search);
                refreshRinksList(query);
                return true;
            }
        }

        return false;
    }

    /**
     * Initializes the SearchView, handling focus and updating query based on the Intent
     * @param item
     */
    private void initializeSearchView(MenuItem item) {
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);

        mSearchView.setQueryHint(getResources().getString(R.string.search_hint_global));

        mSearchView.setIconifiedByDefault(false);
        if (mSearchQuery.length() == 0) {
            mSearchView.requestFocus();
        } else {
            mSearchView.setQuery(mSearchQuery, false);
            mSearchView.clearFocus();
        }

        mSearchView.setOnQueryTextListener(this);
    }

    /**
     * Update the search query from intent extras. Can also launch RinkDetails activity
     *
     * @param intent
     * @param hasRinkDetailsIntent
     */
    private void handleSearchIntent(Intent intent, boolean hasRinkDetailsIntent) {
        if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
            mSearchQuery = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
        } else if (intent.hasExtra(SearchManager.QUERY)) {
            mSearchQuery = intent.getStringExtra(SearchManager.QUERY);
        }

        if (hasRinkDetailsIntent && Intent.ACTION_VIEW.equals(intent.getAction())) {
            try {
                final int rinkId = Integer.valueOf(RinksContract.Rinks.getRinkId(intent.getData()));
                mListener.goRinkDetails(rinkId);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Show the rinks: gets query from intent, updates searchView query
     * and (re)starts the Loader.
     */
    private void showRinksList(boolean hasNewIntent) {

        handleSearchIntent(getActivity().getIntent(), hasNewIntent);

        if (hasNewIntent && mSearchView != null) {
            mSearchView.setQuery(mSearchQuery, false);
        }
        mHasInitialIntent = false;

        if (!mSearchQuery.isEmpty()) {
            getLoaderManager().restartLoader(SearchQuery._TOKEN, null, this);
        }
    }

    /**
     * Display search list following a new Intent
     */
    public void createRinksList() {
        mHasInitialIntent = true;
        showRinksList(true);
    }

    /**
     * Display search list after QueryTextChange and updates the Activity's intent
     * to the new search query.
     */
    private void refreshRinksList(String query) {
        mSearchQuery = query;
        mListener.updateSearchIntent(query);
        showRinksList(false);
    }


    /**
     * Container Activity must implement this interface to receive the list item
     * clicks.
     */
    public interface OnSearchResultClickListener {
        public void goRinkDetails(int id);

        public void updateSearchIntent(String query);
    }

    public static interface SearchQuery extends BaseListFragment.RinksQuery {
        int _TOKEN = 0x30;
    }
}
