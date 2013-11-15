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

package ca.mudar.patinoires.utils;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class SearchMessageHandler extends Handler {

    private final WeakReference<OnMessageHandledListener> mListener;

    /**
     * Caller must implement this interface to receive the handler's message
     */
    public interface OnMessageHandledListener {
        public void OnMessageHandled(Message msg);
    }

    public SearchMessageHandler(OnMessageHandledListener target) {
        mListener = new WeakReference<OnMessageHandledListener>(target);
    }

    @Override
    public void handleMessage(Message msg)
    {
        OnMessageHandledListener target = mListener.get();
        if (target != null) {
            target.OnMessageHandled(msg);
        }
    }
}
