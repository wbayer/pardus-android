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

import at.pardus.android.browser.PardusNotification;
import at.pardus.android.browser.PardusPreferences;
import at.pardus.android.browser.PardusWebView;

/**
 * Contains methods to be called by JavaScript from the Settings screen.
 */
public class JavaScriptSettings {

	private PardusWebView browser;

	public JavaScriptSettings(PardusWebView browser) {
		this.browser = browser;
	}

	/**
	 * Retrieves all settings to initialize the Settings screen.
	 * 
	 * @return a string of all values delimited by ,
	 */
	public String getSettings() {
		String settings = "";
		settings += Boolean.toString(PardusPreferences.isUseHttps());
		settings += ",";
		settings += Boolean.toString(PardusPreferences.isLogoutOnHide());
		settings += ",";
		settings += Integer.toString(PardusPreferences.getNavSize());
		return settings;
	}

	/**
	 * Changes the useHttps setting.
	 * 
	 * @param useHttps
	 */
	public void setUseHttps(boolean useHttps) {
		PardusPreferences.setUseHttps(useHttps);
		String message = (browser.isLoggedIn()) ? " (must re-login!)" : "";
		if (useHttps) {
			PardusNotification.show("Enabled HTTPS use" + message);
		} else {
			PardusNotification.show("Disabled HTTPS use" + message);
		}
	}

	/**
	 * Changes the logoutOnHide setting.
	 * 
	 * @param logoutOnHide
	 */
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
	 * Changes the navSize setting.
	 * 
	 * @param navSize
	 */
	public void setNavSize(int navSize) {
		PardusPreferences.setNavSize(navSize);
		if (browser.isLoggedIn()) {
			browser.setCookies();
		}
		PardusNotification.show("Set Nav size to " + navSize);
	}

	/**
	 * Deletes all cached data.
	 */
	public void clearCache() {
		browser.removeTraces();
		String message = "Emptied cache";
		if (browser.isLoggedIn()) {
			message += " (must re-login!)";
		}
		PardusNotification.show(message);
	}

}
