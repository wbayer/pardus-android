/*
 *    Copyright 2011 Werner Bayer
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

import android.util.Log;
import at.pardus.android.browser.PardusConstants;
import at.pardus.android.browser.PardusPreferences;
import at.pardus.android.browser.PardusWebView;

/**
 * Contains utility methods to be called by JavaScript from various pages.
 */
public class JavaScriptUtils {

	public static final String DEFAULT_JS_NAME = "JavaUtils";

	private PardusWebView pardusView;

	/**
	 * Constructor.
	 * 
	 * @param pardusView
	 *            browser component
	 */
	public JavaScriptUtils(PardusWebView pardusView) {
		this.pardusView = pardusView;
	}

	/**
	 * Sets the available universes to switch to through the menu.
	 * 
	 * @param artemis
	 *            true if a character exists in Artemis
	 * @param orion
	 *            true if a character exists in Orion
	 * @param pegasus
	 *            true if a character exists in Pegasus
	 */
	public void foundUniverse(boolean artemis, boolean orion, boolean pegasus) {
		String universes = "";
		if (artemis) {
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Character exists in Artemis");
			}
			universes += "artemis" + PardusPreferences.GLUE;
		}
		if (orion) {
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Character exists in Orion");
			}
			universes += "orion" + PardusPreferences.GLUE;
		}
		if (pegasus) {
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Character exists in Pegasus");
			}
			universes += "pegasus" + PardusPreferences.GLUE;
		}
		PardusPreferences.setPlayedUniverses(universes);
	}

	/**
	 * Refreshes the notification display due to the assumption of having
	 * received a (status) message.
	 */
	public void refreshNotification() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Looking for a new status or system message (initiated by JS)");
		}
		pardusView.refreshNotification();
	}
}
