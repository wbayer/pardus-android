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
import android.widget.Toast;

/**
 * Includes static helper functions to display small messages to the user.
 */
public abstract class PardusNotification {

	private static Context context = null;

	public static void init(Context context) {
		PardusNotification.context = context;
	}

	public static void show(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static void show(int messageId) {
		show(context.getResources().getString(messageId));
	}

	public static void showLong(String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	public static void showLong(int messageId) {
		showLong(context.getResources().getString(messageId));
	}

}
