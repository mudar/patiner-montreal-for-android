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

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.ListView;

import ca.mudar.patinoires.R;
import ca.mudar.patinoires.providers.RinksContract.Rinks;
import ca.mudar.patinoires.ui.view.ContextualActionbarListener;

public class FavoritesListFragment extends BaseListFragment {

    public FavoritesListFragment() {
        super(Rinks.CONTENT_FAVORITES_URI);
        super.resLayoutListView = R.layout.fragment_list_rinks_favorites;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void createContextActionbar() {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mCABListener = new ContextualActionbarListener(getActivity(), this, mAdapter, true);
        getListView().setMultiChoiceModeListener(mCABListener);
    }
}
