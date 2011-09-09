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

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import at.pardus.android.content.LocalContentProvider;

/**
 * Offers static functions to retrieve and persistently store user preferences.
 */
public abstract class PardusPreferences {

	private static final String NAME = "PardusPreferences";

	private static SharedPreferences preferences = null;

	/**
	 * Initializes required variables.
	 * 
	 * @param context
	 *            context of the application
	 */
	public static void init(Context context) {
		preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
	}

	/**
	 * Checks if an image path exists and is readable.
	 * 
	 * Creates a .nomedia file to hide from Android's gallery app.
	 * 
	 * @return true if valid, false else
	 */
	public static boolean checkImagePath(String imagePath) {
		boolean valid = false;
		if (!imagePath.equals("")) {
			File galleryHideFile = new File(imagePath + "/.nomedia");
			if (galleryHideFile.exists() && galleryHideFile.canRead()) {
				// .nomedia file exists - expect image pack to be there
				valid = true;
			} else {
				// .nomedia file does not exist
				File imagePathCheckFile = new File(imagePath + "/vip.png");
				if (imagePathCheckFile.exists() && imagePathCheckFile.canRead()) {
					valid = true;
					// image pack exists -> create .nomedia file
					try {
						galleryHideFile.createNewFile();
					} catch (IOException e) {
						if (PardusConstants.DEBUG) {
							Log.w("PardusPreferences",
									"Error creating new file "
											+ galleryHideFile.getAbsolutePath());
						}
					}
				}
			}
		}
		return valid;
	}

	/**
	 * @return the stored image path or an empty string
	 */
	public static String getImagePath() {
		return preferences.getString("imagePath", "");
	}

	/**
	 * Stores an image path and applies it to the local content provider.
	 * 
	 * @param imagePath
	 *            the image path to set
	 */
	public static void setImagePath(String imagePath) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("imagePath", imagePath);
		editor.commit();
		LocalContentProvider.FILEPATH = imagePath;
	}

	/**
	 * @return whether HTTPS should be used, true if not stored yet
	 */
	public static boolean isUseHttps() {
		return preferences.getBoolean("useHttps", true);
	}

	/**
	 * Stores whether to use HTTPS.
	 * 
	 * @param useHttps
	 *            true to use HTTPS, false for HTTP
	 */
	public static void setUseHttps(boolean useHttps) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("useHttps", useHttps);
		editor.commit();
	}

	/**
	 * @return whether the account should be logged out when the app is sent to
	 *         the background, false if not stored yet
	 */
	public static boolean isLogoutOnHide() {
		return preferences.getBoolean("logoutOnHide", false);
	}

	/**
	 * Stores whether to log out when the app is sent to the background.
	 * 
	 * @param logoutOnHide
	 *            true to log out, false else
	 */
	public static void setLogoutOnHide(boolean logoutOnHide) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("logoutOnHide", logoutOnHide);
		editor.commit();
	}

	/**
	 * @return the size of the Nav space chart, 5 if not stored yet
	 */
	public static int getNavSize() {
		return preferences.getInt("navSize", 5);
	}

	/**
	 * Stores the requested size of the Nav screen's space chart.
	 * 
	 * @param navSize
	 *            the size in number of tiles
	 */
	public static void setNavSize(int navSize) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("navSize", navSize);
		editor.commit();
	}

	/**
	 * @return whether AJAX should be used for the Nav screen, true if not
	 *         stored yet
	 */
	public static boolean isPartialRefresh() {
		return preferences.getBoolean("partialRefresh", true);
	}

	/**
	 * Stores whether to use AJAX on the Nav screen.
	 * 
	 * @param partialRefresh
	 *            true to use AJAX, false else
	 */
	public static void setPartialRefresh(boolean partialRefresh) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("partialRefresh", partialRefresh);
		editor.commit();
	}

	/**
	 * @return whether ship movement should be animated, false if not stored yet
	 */
	public static boolean isShipAnimation() {
		return preferences.getBoolean("shipAnimation", false);
	}

	/**
	 * Stores whether ship animation is to be done.
	 * 
	 * @param shipAnimation
	 *            true to animate, false else
	 */
	public static void setShipAnimation(boolean shipAnimation) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("shipAnimation", shipAnimation);
		editor.commit();
	}

	/**
	 * @return whether ships should face the direction they are heading, true if
	 *         not stored yet
	 */
	public static boolean isShipRotation() {
		return preferences.getBoolean("shipRotation", true);
	}

	/**
	 * Stores whether ships should face the direction they are heading.
	 * 
	 * @param shipRotation
	 *            true to rotate, false else
	 */
	public static void setShipRotation(boolean shipRotation) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("shipRotation", shipRotation);
		editor.commit();
	}

	/**
	 * @return whether the amount of loaded chat lines should be reduced
	 */
	public static boolean isMobileChat() {
		return preferences.getBoolean("mobileChat", true);
	}

	/**
	 * Stores whether the amount of loaded chat lines should be reduced.
	 * 
	 * @param mobileChat
	 *            true to reduce, false else
	 */
	public static void setMobileChat(boolean mobileChat) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("mobileChat", mobileChat);
		editor.commit();
	}

}
