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

package ca.mudar.patinoires.ui;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Helper;
import ca.mudar.patinoires.utils.NotifyingAsyncQueryHandler;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public abstract class BaseListFragment extends ListFragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener {
    protected static final String TAG = "BaseListFragment";

    protected ActivityHelper mActivityHelper;
    protected PatinoiresApp mAppHelper;

    protected Uri mContentUri;

    protected int QUERY_TOKEN;

    protected NotifyingAsyncQueryHandler mHandler;
    protected SimpleCursorAdapter mAdapter;
    protected Cursor mCursor;

    public BaseListFragment(Uri contentUri) {
        mContentUri = contentUri;
        QUERY_TOKEN = 0x1;
    }

    static final String[] RINKS_SUMMARY_PROJECTION = new String[] {
            BaseColumns._ID,
            RinksColumns.RINK_ID,
            RinksColumns.RINK_NAME,
            RinksColumns.RINK_DESC_EN,
            RinksColumns.RINK_DESC_FR,
            ParksColumns.PARK_PHONE,
            RinksColumns.RINK_IS_FAVORITE
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityHelper = ActivityHelper.createInstance(getActivity());
        mAppHelper = ((PatinoiresApp) getActivity().getApplicationContext());

        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);

        if (mCursor != null) {
            getActivity().stopManagingCursor(mCursor);
            mCursor = null;
        }

        setListAdapter(null);
        mHandler.cancelOperation(QUERY_TOKEN);

        mCursor = getActivity().getContentResolver().query(
                mContentUri,
                RINKS_SUMMARY_PROJECTION, null, null, null);
        getActivity().startManagingCursor(mCursor);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.fragment_list_item_rinks,
                mCursor,
                new String[] {
                        RinksColumns.RINK_NAME,
                        (mAppHelper.getLanguage().equals("fr") ? RinksColumns.RINK_DESC_FR
                                : RinksColumns.RINK_DESC_EN),
                        RinksColumns.RINK_ID
                }, new int[] {
                        R.id.rink_name, R.id.rink_address
                }, 0);

        setListAdapter(mAdapter);

        /**
         * Filter rinks by conditions.
         */
        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        mHandler.startQuery(QUERY_TOKEN, null,
                mContentUri,
                RINKS_SUMMARY_PROJECTION, filter, null,
                Rinks.DEFAULT_SORT);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_list_rinks, null);
        // View root = inflater.inflate(R.layout.fragment_list_rinks, container,
        // false);

        // if ( root.findViewById(R.id.rink_list_item) == null) {
        // Log.v(TAG, "null");
        // }
        // else {
        // Log.v(TAG, "not null");
        // }
        // registerForContextMenu( root.findViewById(R.id.rink_list_item) );
        // Get the list header - to be added later in the lifecycle
        // during onActivityCreated()
        // mheaderView = inflater.inflate(R.layout.list_header, null);
        return root;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(this.getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        // toggleUpdatesWhenLocationChanges(true);

        getActivity().getContentResolver().registerContentObserver(
                mContentUri,
                true, mTransactionsChangesObserver);
        if (mCursor != null) {
            mCursor.requery();
        }

        getListView().setFastScrollEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        // toggleUpdatesWhenLocationChanges(false);

        getActivity().getContentResolver().unregisterContentObserver(mTransactionsChangesObserver);
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (this == null) {
            return;
        }

        getActivity().stopManagingCursor(mCursor);
        mCursor = cursor;
        getActivity().startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // Log.v(TAG, "onCreateContextMenu");
        super.onCreateContextMenu(menu, v, menuInfo);

        String name = mCursor.getString(mCursor.getColumnIndexOrThrow(Rinks.RINK_NAME));
        String phone = mCursor.getString(mCursor.getColumnIndexOrThrow(ParksColumns.PARK_PHONE));
        int isFavorite = mCursor.getInt(mCursor.getColumnIndexOrThrow(Rinks.RINK_IS_FAVORITE));

        menu.setHeaderTitle(name);
        MenuInflater inflater = getSupportActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_rink, menu);

        if (isFavorite == 1) {
            menu.findItem(R.id.favorites_add).setVisible(false);
            menu.findItem(R.id.favorites_remove).setVisible(true);
        }

        if (phone == null) {
            menu.findItem(R.id.call_rink).setVisible(false);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.v(TAG, "onListItemClick. post = " + position);

        int rinkId = mCursor.getInt(mCursor.getColumnIndexOrThrow(Rinks.RINK_ID));
        String name = mCursor.getString(mCursor.getColumnIndexOrThrow(Rinks.RINK_NAME));
        Log.v(TAG, "rinkId = " + rinkId + ". name = " + name);
        mActivityHelper.goRinkDetails(rinkId);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Log.v(TAG, "onContextItemSelected . item = " + item.getItemId());
        switch (item.getItemId()) {
        // case R.id.edit:
        // editNote(info.id);
        // return true;
        // case R.id.delete:
        // deleteNote(info.id);
        // return true;
            default:
                return super.onContextItemSelected(item);
        }
        // return super.onContextItemSelected(item);
    }

    /**
     * Content observer, update cursor on changes.
     */
    protected ContentObserver mTransactionsChangesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mCursor != null) {
                mAdapter.notifyDataSetChanged();
                mCursor.requery();
            }
        }
    };
}
