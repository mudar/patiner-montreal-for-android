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
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.providers.RinksContract.Favorites;
import ca.mudar.patinoires.providers.RinksContract.FavoritesColumns;
import ca.mudar.patinoires.providers.RinksContract.ParksColumns;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.providers.RinksContract.RinksColumns;
import ca.mudar.patinoires.ui.widgets.RinksCursorAdapter;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuInflater;
import android.support.v4.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public abstract class BaseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    protected static final String TAG = "BaseListFragment";

    protected ActivityHelper mActivityHelper;
    protected PatinoiresApp mAppHelper;

    protected Uri mContentUri;

    protected RinksCursorAdapter mAdapter;

    protected Cursor cursor = null;

    public BaseListFragment(Uri contentUri) {
        mContentUri = contentUri;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityHelper = ActivityHelper.createInstance(getActivity());
        mAppHelper = ((PatinoiresApp) getActivity().getApplicationContext());

        setListAdapter(null);

        final String RINK_DESC = (mAppHelper.getLanguage().equals("fr") ? RinksColumns.RINK_DESC_FR
                : RinksColumns.RINK_DESC_EN);

        mAdapter = new RinksCursorAdapter(getActivity(),
                R.layout.fragment_list_item_rinks,
                cursor,
                new String[] {
                        RinksColumns.RINK_NAME, RINK_DESC, RinksColumns.RINK_ID
                },
                new int[] {
                        R.id.rink_name, R.id.rink_address
                },
                0);

        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_list_rinks, null);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(this.getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Cursor c = mAdapter.getCursor();

        String name =
                c.getString(c.getColumnIndexOrThrow(Rinks.RINK_NAME));
        String phone =
                c.getString(c.getColumnIndexOrThrow(ParksColumns.PARK_PHONE));
        int isFavorite =
                c.getInt(c.getColumnIndexOrThrow(Rinks.RINK_IS_FAVORITE));

        menu.setHeaderTitle(name);
        MenuInflater inflater = (MenuInflater) getSupportActivity().getMenuInflater();
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
        super.onListItemClick(l, v, position, id);

        Cursor c = mAdapter.getCursor();
        c.moveToPosition(position);

        int rinkId = c.getInt(c.getColumnIndexOrThrow(Rinks.RINK_ID));

        mActivityHelper.goRinkDetails(rinkId);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor c = mAdapter.getCursor();
        ContentResolver contentResolver = getSupportActivity().getContentResolver();

        int rinkId = c.getInt(c.getColumnIndexOrThrow(RinksColumns.RINK_ID));
        Intent intent;

        switch (item.getItemId()) {
            case R.id.map_view_rink:
                mActivityHelper.goMap(null);
                return true;
            case R.id.favorites_add:
                /**
                 * Add to favorites, and update all ContentResolvers to update
                 * the context menu for this same item.
                 */
                final ContentValues values = new ContentValues();
                values.put(RinksContract.Favorites.FAVORITE_RINK_ID, rinkId);
                contentResolver.insert(Favorites.CONTENT_URI, values);
                mActivityHelper.notifyAllTabs(contentResolver);
                return true;
            case R.id.favorites_remove:
                /**
                 * Remove from favorites, and update all ContentResolvers to
                 * update the context menu for this same item.
                 */
                String[] args = new String[] {
                        Integer.toString(rinkId)
                };
                contentResolver.delete(Favorites.CONTENT_URI,
                        FavoritesColumns.FAVORITE_RINK_ID + "=?", args);
                mActivityHelper.notifyAllTabs(contentResolver);
                return true;
            case R.id.call_rink:
                final String phone = c.getString(c.getColumnIndexOrThrow(ParksColumns.PARK_PHONE));

                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String filter = Helper.getSqliteConditionsFilter(mAppHelper.getConditionsFilter());

        return new CursorLoader(getSupportActivity().getApplicationContext(), mContentUri,
                RINKS_SUMMARY_PROJECTION, filter, null, Rinks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    // TODO change this into a query interface with indexes.
    static final String[] RINKS_SUMMARY_PROJECTION = new String[] {
            BaseColumns._ID,
            RinksColumns.RINK_ID,
            RinksColumns.RINK_NAME,
            RinksColumns.RINK_DESC_EN,
            RinksColumns.RINK_DESC_FR,
            ParksColumns.PARK_PHONE,
            RinksColumns.RINK_IS_FAVORITE
    };

}
