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

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

/**
 * Class handling window and javascript events.
 */
public class PardusWebChromeClient extends WebChromeClient {

	private ProgressBar progress;

	/**
	 * Constructor.
	 * 
	 * @param progress
	 *            the loading progress bar of the browser
	 */
	public PardusWebChromeClient(ProgressBar progress) {
		this.progress = progress;
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
		progress.setProgress(newProgress);
	}

}
