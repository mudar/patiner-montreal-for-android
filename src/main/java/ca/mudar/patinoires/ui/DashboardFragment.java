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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ca.mudar.patinoires.R;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;

public class DashboardFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        final ActivityHelper mActivityHelper = ActivityHelper.createInstance(getActivity());
        
        root.findViewById(R.id.home_btn_skating).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {

                        Intent intent = new Intent(Intent.ACTION_SYNC, null, getActivity()
                                .getApplicationContext(),
                                TabsPagerActivity.class);
                        intent.putExtra(Const.INTENT_EXTRA_TABS_CURRENT, Const.TABS_INDEX_SKATING);
                        startActivity(intent);
                    }
                });
        root.findViewById(R.id.home_btn_hockey).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity().getApplicationContext(),
                                TabsPagerActivity.class);
                        intent.putExtra(Const.INTENT_EXTRA_TABS_CURRENT, Const.TABS_INDEX_HOCKEY);
                        startActivity(intent);
                    }
                });
        root.findViewById(R.id.home_btn_map).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        mActivityHelper.goMap();
                    }
                });
        root.findViewById(R.id.home_btn_favorites).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity().getApplicationContext(),
                                TabsPagerActivity.class);
                        intent.putExtra(Const.INTENT_EXTRA_TABS_CURRENT, Const.TABS_INDEX_FAVORITES);
                        startActivity(intent);
                    }
                });

        return root;
    }

}
