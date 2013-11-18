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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ca.mudar.patinoires.R;

public class AboutActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            AboutFragment about = new AboutFragment();
            fm.beginTransaction().add(android.R.id.content, about).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);

        return true;
    }

    /**
     * AboutFragment
     */
    public static class AboutFragment extends Fragment {

        public static AboutFragment newInstance() {
            return new AboutFragment();
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (container == null) {
                return null;
            }

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
