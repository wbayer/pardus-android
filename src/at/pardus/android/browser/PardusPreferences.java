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

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import at.pardus.android.content.LocalContentProvider;

/**
 * Offers static functions to retrieve and persistently store user preferences.
 */
public abstract class PardusPreferences {

	public static final String GLUE = "|";

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
	 * @return whether the app should be displayed in full screen mode, true if
	 *         not stored yet
	 */
	public static boolean isFullScreen() {
		return preferences.getBoolean("fullScreen", true);
	}

	/**
	 * Stores whether the phone's status bar should be visible.
	 * 
	 * @param fullScreen
	 *            true to hide status bar, false to show
	 */
	public static void setFullScreen(boolean fullScreen) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("fullScreen", fullScreen);
		editor.commit();
	}

	/**
	 * @return whether the app should make zoom in/out buttons visible, false if
	 *         not stored yet
	 */
	public static boolean isShowZoomControls() {
		return preferences.getBoolean("showZoomControls", false);
	}

	/**
	 * Stores whether to show the native zoom control buttons.
	 * 
	 * A boolean value of false only has an effect if the phone supports multi
	 * touch.
	 * 
	 * @param showZoomControls
	 *            true to show the zoom controls, false to hide
	 */
	public static void setShowZoomControls(boolean showZoomControls) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("showZoomControls", showZoomControls);
		editor.commit();
	}

	/**
	 * @return whether the app should remember the zoom level and scroll
	 *         position of each visited page, true if not stored yet
	 */
	public static boolean isRememberPageProperties() {
		return preferences.getBoolean("rememberPageProperties", true);
	}

	/**
	 * Stores whether the zoom level and scoll position of each page should be
	 * saved and restored in subsequent visits.
	 * 
	 * @param rememberPageProperties
	 *            true to remember properties of each visited page, false to use
	 *            default zoom level and top left position on each page load
	 */
	public static void setRememberPageProperties(boolean rememberPageProperties) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("rememberPageProperties", rememberPageProperties);
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

	/**
	 * @return which universe switches to offer in the menu (universes delimited
	 *         by GLUE)
	 */
	public static String getPlayedUniverses() {
		return preferences.getString("universes", "");
	}

	/**
	 * Stores which universe switches to offer in the menu.
	 * 
	 * @param universes
	 *            contains lower-case universes delimited by GLUE
	 */
	public static void setPlayedUniverses(String universes) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("universes", universes);
		editor.commit();
	}

	/**
	 * @return amount of inches * 10 scrolled over the border after which to
	 *         fade in the links menu bar (-1 for never)
	 */
	public static int getMenuSensitivity() {
		return preferences.getInt("menuSensitivity", 2);
	}

	/**
	 * Stores the menu sensitivity.
	 * 
	 * @param menuSensitivity
	 *            amount of inches * 10 scrolled over the border after which to
	 *            fade in the links menu bar (-1 for never)
	 */
	public static void setMenuSensitivity(int menuSensitivity) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("menuSensitivity", menuSensitivity);
		editor.commit();
	}

	/**
	 * @return the stored Pardus links in serialized form
	 */
	public static String getLinks() {
		return preferences.getString("links", null);
	}

	/**
	 * Stores the serialized form of Pardus links.
	 * 
	 * @param links
	 *            serialized links string
	 */
	public static void setLinks(String links) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("links", links);
		editor.commit();
	}

	/**
	 * @return the date of the next scheduled image pack update check
	 */
	public static Date getNextImagePackUpdateCheck() {
		return new Date(preferences.getLong("ipUpdateCheck", 0));
	}

	/**
	 * Stores the date to next check for an update of the image pack.
	 * 
	 * @param ipUpdateCheck
	 *            the date of the next scheduled image pack update check
	 */
	public static void setNextImagePackUpdateCheck(Date ipUpdateCheck) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("ipUpdateCheck", ipUpdateCheck.getTime());
		editor.commit();
	}

	/**
	 * @return the stored version code
	 */
	public static int getVersionCode() {
		return preferences.getInt("versionCode", -1);
	}

	/**
	 * Stores the app's version code.
	 * 
	 * @param versionCode
	 *            the app's version code
	 */
	public static void setVersionCode(int versionCode) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("versionCode", versionCode);
		editor.commit();
	}

}
