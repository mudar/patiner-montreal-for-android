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

package ca.mudar.patinoires.ui.view;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.Toast;

import java.util.ArrayList;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract;
import ca.mudar.patinoires.ui.fragment.BaseListFragment;
import ca.mudar.patinoires.utils.Lists;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ContextualActionbarListener implements AbsListView.MultiChoiceModeListener {
    protected static final String TAG = "ContextualActionbarListener";
    private final IMultiChoiceModeAdapter mAdapter;
    private final boolean mIsFavoritesList;
    private boolean phoneNumberEnabled;
    private ActionMode mActionMode;
    private Context mContext;
    private OnRinkActionsListener mListener;

    public ContextualActionbarListener(Context context, OnRinkActionsListener listener, IMultiChoiceModeAdapter adapter, boolean isFavoritesList) {
        mContext = context;
        mAdapter = adapter;
        mIsFavoritesList = isFavoritesList;
        mListener = listener;
        phoneNumberEnabled = true;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        final int nbItems = mAdapter.getSelectionSize();

        final Cursor cursor = mAdapter.getCursor();
        final int rinkId = cursor.getInt(BaseListFragment.RinksQuery.RINK_ID);
        final String phone = cursor.getString(BaseListFragment.RinksQuery.PARK_PHONE);

        phoneNumberEnabled = (phone != null && phone.length() > 0);

        mAdapter.setNewSelection(rinkId, checked);

        if (checked && nbItems <= 1) {
            mActionMode.invalidate();
        }

        mode.setTitle(String.valueOf(mAdapter.getSelectionSize()));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;

        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.context_actionbar_rink, menu);

        toggleFavoritesButtons(menu, mIsFavoritesList);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        final int nbItems = mAdapter.getSelectionSize();

        if (nbItems <= 1) {
            menu.findItem(R.id.map_view_rink).setVisible(true);
            menu.findItem(R.id.call_rink).setVisible(phoneNumberEnabled);
        } else {
            menu.findItem(R.id.map_view_rink).setVisible(false);
            menu.findItem(R.id.call_rink).setVisible(false);

            toggleFavoritesButtons(menu, mIsFavoritesList);
        }

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        final int nbItems = mAdapter.getSelectionSize();
        final String[] rinkIds = mAdapter.getSelectionItems();

        clearActionMode();

        if (nbItems == 1) {
            return onActionItemClickedSingle(item, rinkIds[0]);
        } else if (nbItems > 1) {
            return onActionItemClickedMulti(item, rinkIds);
        }


        return false;
    }

    /**
     * Handle CAB actions for a single rink. Additional buttons are call and viewMap.
     *
     * @param item
     * @param rinkId
     * @return
     */
    private boolean onActionItemClickedSingle(MenuItem item, String rinkId) {
        if (item.getItemId() == R.id.favorites_add) {
            mListener.addToFavorites(Integer.valueOf(rinkId));
            return true;
        } else if (item.getItemId() == R.id.favorites_remove) {
            mListener.removeFromFavorites(Integer.valueOf(rinkId));
            return true;
        } else {
            final Uri rinkUri = RinksContract.Rinks.buildRinkUri(rinkId);
            final RinkInfoHolder rinkInfo = getRinkInfo(rinkId);

            if (item.getItemId() == R.id.map_view_rink) {
                mListener.goMapCAB(rinkInfo.geoLat, rinkInfo.geoLng);
                return true;
            } else if (item.getItemId() == R.id.call_rink) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + rinkInfo.parkPhone));
                mContext.startActivity(intent);
                return true;
            }
        }

        return false;
    }

    /**
     * Handle CAB actions for multiple rinks: add/remove from favorites only
     *
     * @param item
     * @param rinkIds
     * @return
     */
    private boolean onActionItemClickedMulti(MenuItem item, String[] rinkIds) {
        final int nbRinks = rinkIds.length;
        final ContentResolver contentResolver = mContext.getContentResolver();

        if (item.getItemId() == R.id.favorites_add) {
            // Batch operation to allow DB transaction
            final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

            for (int i = 0; i < nbRinks; i++) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RinksContract.Favorites.CONTENT_URI);

                builder.withValue(RinksContract.FavoritesColumns.FAVORITE_RINK_ID, rinkIds[i]);
                batch.add(builder.build());
            }

            try {
                contentResolver.applyBatch(RinksContract.CONTENT_AUTHORITY, batch);
                mListener.notifyAllTabsCAB(contentResolver);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            } finally {
                ((PatinoiresApp) mContext.getApplicationContext()).showToastText(R.string.toast_favorites_added_multi_brief, Toast.LENGTH_SHORT);
                return true;
            }
        } else if (item.getItemId() == R.id.favorites_remove) {

            String argsJoined = "";
            for (int i = 0; i < nbRinks; i++) {
                argsJoined += rinkIds[i];
                if (i < nbRinks - 1) {
                    argsJoined += ",";
                }
            }

            contentResolver.delete(RinksContract.Favorites.CONTENT_URI,
                    RinksContract.FavoritesColumns.FAVORITE_RINK_ID + " IN(" + argsJoined + ") ", null);
            mListener.notifyAllTabsCAB(contentResolver);

            ((PatinoiresApp) mContext.getApplicationContext()).showToastText(R.string.toast_favorites_removed_multi_brief, Toast.LENGTH_SHORT);
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Clear selection on CAB destroy
        mAdapter.clearSelection();
    }

    public void clearActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /**
     * Display only one of the favorites add/remove buttons.
     * Used depending on the item's isFavorite status or the lists's type.
     *
     * @param menu
     * @param isFavorite
     */
    private void toggleFavoritesButtons(Menu menu, boolean isFavorite) {
        menu.findItem(R.id.favorites_add).setVisible(!isFavorite);
        menu.findItem(R.id.favorites_remove).setVisible(isFavorite);
    }

    /**
     * Get Rink information from database, using ContentResolver.
     * Information is returned in a RinkInfoHolder object
     *
     * @param rinkId
     * @return Rink details
     */
    private RinkInfoHolder getRinkInfo(String rinkId) {
        final Uri rinkUri = RinksContract.Rinks.buildRinkUri(rinkId);
        Cursor cursor = mContext.getContentResolver().query(
                rinkUri,
                RinksQueryCAB.RINKS_SUMMARY_PROJECTION,
                null, null, null
        );
        if (!cursor.moveToFirst()) {
            return null;
        }

        RinkInfoHolder rinkInfo = new RinkInfoHolder(
                cursor.getInt(RinksQueryCAB.RINK_ID),
                cursor.getInt(RinksQueryCAB.RINK_IS_FAVORITE) == 1,
                cursor.getDouble(RinksQueryCAB.PARK_GEO_LAT),
                cursor.getDouble(RinksQueryCAB.PARK_GEO_LNG),
                cursor.getString(RinksQueryCAB.PARK_PHONE)
        );
        cursor.close();

        return rinkInfo;
    }

    /**
     * ListFragment must implement the following interface
     */
    public interface OnRinkActionsListener {
        public void addToFavorites(int rinkId);

        public void removeFromFavorites(int rinkId);

        public void goMapCAB(double lat, double lng);

        public void notifyAllTabsCAB(ContentResolver contentResolver);
    }


    /**
     * DB Query interface
     */
    public static interface RinksQueryCAB {
        // int _TOKEN = 0x10;

        final String[] RINKS_SUMMARY_PROJECTION = new String[]{
                BaseColumns._ID,
                RinksContract.RinksColumns.RINK_ID,
                RinksContract.RinksColumns.RINK_IS_FAVORITE,
                RinksContract.ParksColumns.PARK_GEO_LAT,
                RinksContract.ParksColumns.PARK_GEO_LNG,
                RinksContract.ParksColumns.PARK_PHONE
        };
        final int _ID = 0;
        final int RINK_ID = 1;
        final int RINK_IS_FAVORITE = 2;
        final int PARK_GEO_LAT = 3;
        final int PARK_GEO_LNG = 4;
        final int PARK_PHONE = 5;
    }

    /**
     * Object holding a single rink information
     */
    private class RinkInfoHolder {
        final int id;
        final boolean isFavorite;
        final double geoLat;
        final double geoLng;
        final String parkPhone;

        RinkInfoHolder(int id, boolean isFavorite, double geoLat, double geoLng, String parkPhone) {
            this.id = id;
            this.isFavorite = isFavorite;
            this.geoLat = geoLat;
            this.geoLng = geoLng;
            this.parkPhone = parkPhone;
        }
    }
}
