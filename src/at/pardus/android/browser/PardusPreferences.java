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
import android.content.SharedPreferences;
import at.pardus.android.content.LocalContentProvider;

/**
 * Offers static functions to retrieve and persistently store user preferences.
 */
public abstract class PardusPreferences {

	public static boolean useHttps = true;

	public static boolean logoutOnHide = false;

	private static final String NAME = "PardusPreferences";

	private static SharedPreferences preferences = null;

	public static void init(Context context) {
		preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
	}

	public static String getImagePath() {
		return preferences.getString("imagePath", "");
	}

	public static void setImagePath(String imagePath) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("imagePath", imagePath);
		editor.commit();
		LocalContentProvider.FILEPATH = imagePath;
	}

}
