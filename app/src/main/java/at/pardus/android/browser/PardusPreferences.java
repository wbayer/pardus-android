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

import java.util.Date;

/**
 * Offers static functions to retrieve and persistently store user preferences.
 */
public abstract class PardusPreferences {

    public static final String GLUE = "|";

    private static final String NAME = "PardusPreferences";

    private static SharedPreferences preferences = null;

    private static int defaultInitialScale = 100;

    /**
     * Initializes required variables.
     *
     * @param context
     *         context of the application
     * @param defaultInitialScale
     *         default initial scale of the webview to determine its viewport
     */
    public static void init(Context context, Integer defaultInitialScale) {
        if (context != null) {
            preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
        if (defaultInitialScale != null) {
            PardusPreferences.defaultInitialScale = defaultInitialScale;
        }
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
     *         the image path to set
     */
    public static void setImagePath(String imagePath) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("imagePath", imagePath);
        editor.apply();
    }

    /**
     * @return whether the account should be logged out when the app is sent to the background, false if not
     * stored yet
     */
    public static boolean isLogoutOnHide() {
        return preferences.getBoolean("logoutOnHide", false);
    }

    /**
     * Stores whether to log out when the app is sent to the background.
     *
     * @param logoutOnHide
     *         true to log out, false else
     */
    public static void setLogoutOnHide(boolean logoutOnHide) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("logoutOnHide", logoutOnHide);
        editor.apply();
    }

    /**
     * @return the width of the Nav space chart, calc if not stored yet
     */
    public static int getNavSizeHor() {
        int navSizeHor = preferences.getInt("navSize", 0);
        if (navSizeHor == 0) {
            int w = (int) Math.round(Pardus.displayWidthPx / (defaultInitialScale / 100.0));
            int h = (int) Math.round(Pardus.displayHeightPx / (defaultInitialScale / 100.0));
            if (w < h) {
                int t = w;
                w = h;
                h = t;
            }
            w -= 293;
            h -= 40;
            navSizeHor = Math.min(Math.max((int) Math.floor(w / 64.0), 5), 11);
            if (navSizeHor % 2 == 0) {
                navSizeHor--;
            }
            int navSizeVer = Math.min(Math.max((int) Math.floor(h / 64.0), 5), 9);
            if (navSizeVer % 2 == 0) {
                navSizeVer--;
            }
            setNavSizeHor(navSizeHor);
            setNavSizeVer(navSizeVer);
        }
        return navSizeHor;
    }

    /**
     * Stores the requested width of the Nav screen's space chart.
     *
     * @param navSizeHor
     *         the width in number of tiles
     */
    public static void setNavSizeHor(int navSizeHor) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("navSize", navSizeHor);
        editor.apply();
    }

    /**
     * @return the height of the Nav space chart, navSizeHor if not stored yet
     */
    public static int getNavSizeVer() {
        return preferences.getInt("navSizeVer", getNavSizeHor());
    }

    /**
     * Stores the requested height of the Nav screen's space chart.
     *
     * @param navSizeVer
     *         the height in number of tiles
     */
    public static void setNavSizeVer(int navSizeVer) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("navSizeVer", navSizeVer);
        editor.apply();
    }

    /**
     * @return whether the app should be displayed in full screen mode, true if not stored yet
     */
    public static boolean isFullScreen() {
        return preferences.getBoolean("fullScreen", true);
    }

    /**
     * Stores whether the phone's status bar should be visible.
     *
     * @param fullScreen
     *         true to hide status bar, false to show
     */
    public static void setFullScreen(boolean fullScreen) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("fullScreen", fullScreen);
        editor.apply();
    }

    /**
     * @return whether the app should make zoom in/out buttons visible, false if not stored yet
     */
    public static boolean isShowZoomControls() {
        return preferences.getBoolean("showZoomControls", false);
    }

    /**
     * Stores whether to show the native zoom control buttons.
     *
     * A boolean value of false only has an effect if the phone supports multi touch.
     *
     * @param showZoomControls
     *         true to show the zoom controls, false to hide
     */
    public static void setShowZoomControls(boolean showZoomControls) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("showZoomControls", showZoomControls);
        editor.apply();
    }

    /**
     * @return whether the app should remember the zoom level and scroll position of each visited page, true
     * if not stored yet
     */
    public static boolean isRememberPageProperties() {
        return preferences.getBoolean("rememberPageProperties", true);
    }

    /**
     * Stores whether the zoom level and scoll position of each page should be saved and restored in
     * subsequent visits.
     *
     * @param rememberPageProperties
     *         true to remember properties of each visited page, false to use default zoom level and top left
     *         position on each page load
     */
    public static void setRememberPageProperties(boolean rememberPageProperties) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("rememberPageProperties", rememberPageProperties);
        editor.apply();
    }

    /**
     * @return whether AJAX should be used for the Nav screen, true if not stored yet
     */
    public static boolean isPartialRefresh() {
        return preferences.getBoolean("partialRefresh", true);
    }

    /**
     * Stores whether to use AJAX on the Nav screen.
     *
     * @param partialRefresh
     *         true to use AJAX, false else
     */
    public static void setPartialRefresh(boolean partialRefresh) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("partialRefresh", partialRefresh);
        editor.apply();
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
     *         true to animate, false else
     */
    public static void setShipAnimation(boolean shipAnimation) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shipAnimation", shipAnimation);
        editor.apply();
    }

    /**
     * @return whether ships should face the direction they are heading, true if not stored yet
     */
    public static boolean isShipRotation() {
        return preferences.getBoolean("shipRotation", true);
    }

    /**
     * Stores whether ships should face the direction they are heading.
     *
     * @param shipRotation
     *         true to rotate, false else
     */
    public static void setShipRotation(boolean shipRotation) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shipRotation", shipRotation);
        editor.apply();
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
     *         true to reduce, false else
     */
    public static void setMobileChat(boolean mobileChat) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("mobileChat", mobileChat);
        editor.apply();
    }

    /**
     * @return which universe switches to offer in the menu (universes delimited by GLUE)
     */
    public static String getPlayedUniverses() {
        return preferences.getString("universes", "");
    }

    /**
     * Stores which universe switches to offer in the menu.
     *
     * @param universes
     *         contains lower-case universes delimited by GLUE
     */
    public static void setPlayedUniverses(String universes) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("universes", universes);
        editor.apply();
    }

    /**
     * @return amount of inches * 10 scrolled over the border after which to fade in the links menu bar (-1
     * for never)
     */
    public static int getMenuSensitivity() {
        return preferences.getInt("menuSensitivity", 2);
    }

    /**
     * Stores the menu sensitivity.
     *
     * @param menuSensitivity
     *         amount of inches * 10 scrolled over the border after which to fade in the links menu bar (-1
     *         for never)
     */
    public static void setMenuSensitivity(int menuSensitivity) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("menuSensitivity", menuSensitivity);
        editor.apply();
    }

    /**
     * @return the storeCredentials setting previously selected by the user
     */
    public static StoreCredentials getStoreCredentials() {
        return StoreCredentials.fromInt(preferences.getInt("storeCredentials", StoreCredentials.NO.value));
    }

    /**
     * Stores whether to remember credentials. If set to {@code NO} or {@code NEVER}, also resets any stored
     * account and password.
     *
     * @param storeCredentials
     *         whether to remember credentials
     */
    public static void setStoreCredentials(StoreCredentials storeCredentials) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("storeCredentials", storeCredentials.value);
        editor.apply();
        if (storeCredentials == StoreCredentials.NO || storeCredentials == StoreCredentials.NEVER) {
            setAccount("");
            setPassword("");
        }
    }

    /**
     * @return the user's account
     */
    public static String getAccount() {
        return preferences.getString("account", "");
    }

    /**
     * Stores the user's account.
     *
     * @param account
     *         the user's account
     */
    public static void setAccount(String account) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("account", account);
        editor.apply();
    }

    /**
     * @return the user's hashed password
     */
    public static String getPassword() {
        return preferences.getString("password", "");
    }

    /**
     * Stores the user's hashed password.
     *
     * @param password
     *         the user's hashed password
     */
    public static void setPassword(String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("password", password);
        editor.apply();
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
     *         serialized links string
     */
    public static void setLinks(String links) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("links", links);
        editor.apply();
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
     *         the date of the next scheduled image pack update check
     */
    public static void setNextImagePackUpdateCheck(Date ipUpdateCheck) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("ipUpdateCheck", ipUpdateCheck.getTime());
        editor.apply();
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
     *         the app's version code
     */
    public static void setVersionCode(int versionCode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("versionCode", versionCode);
        editor.apply();
    }

    public enum StoreCredentials {
        NO(0), NEVER(1), YES(2);

        final int value;

        StoreCredentials(int value) {
            this.value = value;
        }

        static StoreCredentials fromInt(int value) {
            StoreCredentials[] allStoreCredentials = StoreCredentials.values();
            for (StoreCredentials storeCredentials : allStoreCredentials) {
                if (storeCredentials.value == value) {
                    return storeCredentials;
                }
            }
            return null;
        }
    }

}
