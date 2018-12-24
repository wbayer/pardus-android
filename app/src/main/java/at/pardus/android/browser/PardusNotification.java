/*
 *    Copyright 2010 Werner Bayer
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.pardus.android.browser;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * Includes static helper functions to display small messages to the user.
 */
public abstract class PardusNotification {

    private static WeakReference<Context> context = null;

    static void init(Context context) {
        PardusNotification.context = new WeakReference<>(context);
    }

    public static void show(String message) {
        try {
            Toast.makeText(context.get(), message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.i(PardusNotification.class.getSimpleName(), "Error displaying message " + message);
        }
    }

    static void showLong(String message) {
        try {
            Toast.makeText(context.get(), message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.i(PardusNotification.class.getSimpleName(), "Error displaying message " + message);
        }
    }

}
