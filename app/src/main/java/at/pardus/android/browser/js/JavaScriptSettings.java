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

package at.pardus.android.browser.js;

import android.app.Activity;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import at.pardus.android.browser.PardusNotification;
import at.pardus.android.browser.PardusPreferences;
import at.pardus.android.browser.PardusWebView;

/**
 * Contains methods to be called by JavaScript from the Settings screen.
 */
public class JavaScriptSettings {

	public static final String DEFAULT_JS_NAME = "JavaSettings";

	private PardusWebView browser;

	private Activity activity;

	/**
	 * Constructor.
	 * 
	 * @param browser
	 *            the Pardus browser component
	 * @param activity
	 *            the activity the browser component runs in
	 */
	public JavaScriptSettings(PardusWebView browser, Activity activity) {
		this.browser = browser;
		this.activity = activity;
	}

	/**
	 * Retrieves all settings to initialize the Settings screen.
	 * 
	 * @return a string of all values delimited by ,
	 */
    @JavascriptInterface
	public String getSettings() {
		String settings = "";
		settings += Boolean.toString(PardusPreferences.isUseHttps());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isLogoutOnHide());
		settings += ",";
		settings += Integer.toString(PardusPreferences.getNavSizeHor());
		settings += ",";
		settings += Integer.toString(PardusPreferences.getNavSizeVer());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isPartialRefresh());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isShipAnimation());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isShipRotation());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isMobileChat());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isFullScreen());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isShowZoomControls());
		settings += ",";
		settings += Boolean.toString(PardusPreferences
				.isRememberPageProperties());
		return settings;
	}

	/**
	 * Changes the useHttps setting.
	 * 
	 * @param useHttps
	 */
    @JavascriptInterface
	public void setUseHttps(boolean useHttps) {
		PardusPreferences.setUseHttps(useHttps);
		String message = (browser.isLoggedIn()) ? " (must re-login!)" : "";
		if (useHttps) {
			PardusNotification.show("Enabled HTTPS use" + message);
		} else {
			PardusNotification.show("Disabled HTTPS use" + message);
		}
		browser.destroySession();
	}

	/**
	 * Changes the logoutOnHide setting.
	 * 
	 * @param logoutOnHide
	 */
    @JavascriptInterface
	public void setLogoutOnHide(boolean logoutOnHide) {
		PardusPreferences.setLogoutOnHide(logoutOnHide);
		if (logoutOnHide) {
			PardusNotification
					.show("Will log out when the app is sent to the background");
		} else {
			PardusNotification
					.show("Will stay logged in when the app is sent to the background");
		}
	}

	/**
	 * Changes the navSizeHor setting.
	 * 
	 * @param navSizeHor
	 */
    @JavascriptInterface
	public void setNavSizeHor(String navSizeHor) {
		PardusPreferences.setNavSizeHor(Integer.parseInt(navSizeHor));
		if (browser.isLoggedIn()) {
			browser.setCookies();
		}
		PardusNotification.show("Set Nav width to " + navSizeHor);
	}

	/**
	 * Changes the navSizeVer setting.
	 * 
	 * @param navSizeVer
	 */
    @JavascriptInterface
	public void setNavSizeVer(String navSizeVer) {
		PardusPreferences.setNavSizeVer(Integer.parseInt(navSizeVer));
		if (browser.isLoggedIn()) {
			browser.setCookies();
		}
		PardusNotification.show("Set Nav height to " + navSizeVer);
	}

	/**
	 * Changes the partialRefresh setting.
	 * 
	 * @param partialRefresh
	 */
    @JavascriptInterface
	public void setPartialRefresh(boolean partialRefresh) {
		PardusPreferences.setPartialRefresh(partialRefresh);
		String message = "";
		if (browser.isLoggedIn()) {
			browser.setCookies();
			message = " (must reload Nav screen!)";
		}
		if (partialRefresh) {
			PardusNotification
					.show("Enabled partial page refreshing" + message);
		} else {
			PardusNotification.show("Disabled partial page refreshing"
					+ message);
		}
	}

	/**
	 * Changes the shipAnimation setting.
	 * 
	 * @param shipAnimation
	 */
    @JavascriptInterface
	public void setShipAnimation(boolean shipAnimation) {
		PardusPreferences.setShipAnimation(shipAnimation);
		String message = "";
		if (browser.isLoggedIn()) {
			browser.setCookies();
			message = " (must reload Nav screen!)";
		}
		if (shipAnimation) {
			PardusNotification
					.show("Enabled ship movement animation" + message);
		} else {
			PardusNotification.show("Disabled ship movement animation"
					+ message);
		}
	}

	/**
	 * Changes the shipRotation setting.
	 * 
	 * @param shipRotation
	 */
    @JavascriptInterface
	public void setShipRotation(boolean shipRotation) {
		PardusPreferences.setShipRotation(shipRotation);
		String message = "";
		if (browser.isLoggedIn()) {
			browser.setCookies();
			message = " (must reload Nav screen!)";
		}
		if (shipRotation) {
			PardusNotification.show("Enabled ship rotation" + message);
		} else {
			PardusNotification.show("Disabled ship rotation" + message);
		}
	}

	/**
	 * Changes the mobileChat setting.
	 * 
	 * @param mobileChat
	 */
    @JavascriptInterface
	public void setMobileChat(boolean mobileChat) {
		PardusPreferences.setMobileChat(mobileChat);
		String message = "";
		if (browser.isLoggedIn()) {
			browser.setCookies();
			message = " (must reload Chat screen!)";
		}
		if (mobileChat) {
			PardusNotification.show("Enabled chat lines limit" + message);
		} else {
			PardusNotification.show("Disabled chat lines limit" + message);
		}
	}

	/**
	 * Changes the fullscreen setting.
	 * 
	 * @param fullScreen
	 */
    @JavascriptInterface
	public void setFullScreen(boolean fullScreen) {
		PardusPreferences.setFullScreen(fullScreen);
		Runnable runnable;
		String message = "";
		if (fullScreen) {
			runnable = new Runnable() {

				@Override
				public void run() {
					activity.getWindow().addFlags(
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
				}

			};
			message = "Enabled";
		} else {
			runnable = new Runnable() {

				@Override
				public void run() {
					activity.getWindow().clearFlags(
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
				}

			};
			message = "Disabled";
		}
		activity.runOnUiThread(runnable);
		message += " full-screen mode";
		PardusNotification.show(message);
	}

	/**
	 * Changes the showZoomControls setting.
	 * 
	 * @param showZoomControls
	 */
    @JavascriptInterface
	public void setShowZoomControls(boolean showZoomControls) {
		PardusPreferences.setShowZoomControls(showZoomControls);
		browser.setShowZoomControls(showZoomControls);
		if (showZoomControls) {
			PardusNotification.show("Zoom controls will be shown");
		} else {
			PardusNotification.show("Zoom controls will be hidden");
		}
	}

	/**
	 * Changes the rememberPageProperties setting.
	 * 
	 * @param rememberPageProperties
	 */
    @JavascriptInterface
	public void setRememberPageProperties(boolean rememberPageProperties) {
		PardusPreferences.setRememberPageProperties(rememberPageProperties);
		browser.setRememberPageProperties(rememberPageProperties);
		if (rememberPageProperties) {
			PardusNotification
					.show("Zoom level and scrolling position of pages will be remembered");
		} else {
			PardusNotification
					.show("Zoom level and scrolling position of pages will be the browser-default");
		}
	}

	/**
	 * Deletes all cached data.
	 */
    @JavascriptInterface
	public void clearCache() {
		browser.removeTraces();
		String message = "Emptied cache";
		if (browser.isLoggedIn()) {
			message += " (must re-login!)";
		}
		PardusNotification.show(message);
	}

}
