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

import android.app.Activity;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Class handling window and javascript events.
 */
public class PardusWebChromeClient extends WebChromeClient {

	private Activity activity;

	/**
	 * Constructor.
	 * 
	 * @param activity
	 *            the web view's host
	 */
	public PardusWebChromeClient(Activity activity) {
		this.activity = activity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.webkit.WebChromeClient#onProgressChanged(android.webkit.WebView,
	 * int)
	 */
	@Override
	public void onProgressChanged(WebView view, int newProgress) {
		activity.setProgress(newProgress * 100);
	}

}
