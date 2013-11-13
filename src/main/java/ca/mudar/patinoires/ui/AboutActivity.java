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
import ca.mudar.patinoires.utils.ActivityHelper;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItem;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PatinoiresApp) getApplicationContext()).updateUiLanguage();

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            AboutFragment about = new AboutFragment();
            fm.beginTransaction().add(android.R.id.content, about).commit();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * AboutFragment
     */
    public static class AboutFragment extends Fragment {

        public static AboutFragment newInstance() {
            AboutFragment about = new AboutFragment();

            return about;
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            if (container == null) {
                return null;
            }

            /**
             * Handle UI language changes.
             */
            getSupportActivity().getSupportActionBar().setTitle(R.string.app_name_map);

            /**
             * Inflate XML layout.
             */
            View root = inflater.inflate(R.layout.fragment_about, container, false);

            /**
             * Display version number in the About header.
             */
            ((TextView) root.findViewById(R.id.about_contents_project_version))
                    .setText(String.format(
                            getResources().getString(R.string.about_contents_project_version),
                            getResources()
                                    .getString(R.string.app_version)));

            /**
             * Handle web links.
             */
            MovementMethod method = LinkMovementMethod.getInstance();
            ((TextView) root.findViewById(R.id.about_contents_credits_1))
                    .setMovementMethod(method);
            ((TextView) root.findViewById(R.id.about_contents_credits_2))
                    .setMovementMethod(method);
            ((TextView) root.findViewById(R.id.about_contents_credits_3))
            .setMovementMethod(method);
            ((TextView) root.findViewById(R.id.about_contents_open_data))
                    .setMovementMethod(method);

            return root;
        }
    }
}
