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

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import at.pardus.android.content.LocalContentProvider;

/**
 * Class overriding the browser's default behavior on certain events.
 */
public class PardusWebViewClient extends WebViewClient {

	private ProgressBar progress;

	private boolean onSendMessageScreen = false;

	private static final String jsSkipSendMessageDeath = "if "
			+ "(document.getElementsByTagName('html')[0].innerHTML.indexOf('"
			+ "self.close()" + "') != -1) { " + "top.location.replace('"
			+ PardusConstants.msgPage + "'); }";

	/**
	 * Constructor.
	 * 
	 * @param progress
	 *            the loading progress bar of the browser
	 */
	public PardusWebViewClient(ProgressBar progress) {
		this.progress = progress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.
	 * WebView, java.lang.String)
	 */
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		// FIXME this method is not called if not a user action (click)
		// FIXME this method is not called if the target is a frame
		if (!url.contains(".pardus.at")
				&& !url.startsWith("file:///android_asset/")
				&& !url.startsWith(LocalContentProvider.URI)
				&& !url.startsWith("javascript:")) {
			// let our app only handle pardus or local addresses
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Not handling " + url);
			}
			return false;
		}
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Following link");
		}
		onSendMessageScreen = false;
		PardusWebView pardusView = (PardusWebView) view;
		if (!pardusView.isLoggedIn()
				&& !url.startsWith("file:///android_asset/")
				&& !url.startsWith("http://static.pardus.at/")
				&& !url.startsWith(PardusConstants.loginUrl)
				&& !url.startsWith(PardusConstants.loginUrlHttps)
				&& !url.startsWith(PardusConstants.loggedInUrl)
				&& !url.startsWith(PardusConstants.loggedInUrlHttps)
				&& !url.startsWith(PardusConstants.newCharUrl)
				&& !url.startsWith(PardusConstants.newCharUrlHttps)
				&& !url.startsWith(PardusConstants.signupUrl)
				&& !url.startsWith(PardusConstants.signupUrlHttps)) {
			// only allow local pages, login post action, login redirection,
			// signup page, signup redirection while not logged in
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Access to " + url
						+ " denied while not logged in");
			}
			pardusView.login(false);
		} else {
			if (url.equals(PardusConstants.loginUrlOrig)
					|| url.equals(PardusConstants.loginUrlHttpsOrig)) {
				// redirect from online login page to local one
				if (PardusConstants.DEBUG) {
					Log.d(this.getClass().getSimpleName(),
							"Redirecting from online login page to local one");
				}
				pardusView.login(true);
			} else {
				// load the requested url
				view.loadUrl(url);
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView,
	 * java.lang.String, android.graphics.Bitmap)
	 */
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		// FIXME this method is not called if the target is a frame
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Started loading " + url);
		}
		progress.setProgress(0);
		progress.setVisibility(View.VISIBLE);
		PardusWebView pardusView = (PardusWebView) view;
		pardusView.setUniverse(url);
		if (url.startsWith(PardusConstants.loggedInUrl)
				|| url.startsWith(PardusConstants.loggedInUrlHttps)
				|| url.startsWith(PardusConstants.newCharUrl)
				|| url.startsWith(PardusConstants.newCharUrlHttps)) {
			// loading page on login or signup success
			pardusView.setLoggedIn(true);
		} else if (url.startsWith(PardusConstants.logoutUrl)
				|| url.startsWith(PardusConstants.logoutUrlHttps)) {
			// loading log out url
			pardusView.setLoggedIn(false);
		} else if (url.contains("/sendmsg.php")) {
			// loading send message screen
			onSendMessageScreen = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView,
	 * java.lang.String)
	 */
	@Override
	public void onPageFinished(WebView view, String url) {
		// FIXME this method is not called if the target is a frame
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Finished loading " + url);
		}
		progress.setVisibility(View.GONE);
		if (onSendMessageScreen) {
			// new message popups are opened in the same window => redirect back
			// to the game after sending
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Checking send message page for self.close()");
			}
			view.loadUrl("javascript:(function() { " + jsSkipSendMessageDeath
					+ " })()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.webkit.WebViewClient#onReceivedSslError(android.webkit.WebView,
	 * android.webkit.SslErrorHandler, android.net.http.SslError)
	 */
	@Override
	public void onReceivedSslError(WebView view, SslErrorHandler handler,
			SslError error) {
		// android version < 2.2 does not recognize the ca
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "SSL certificate error "
					+ error.getPrimaryError());
		}
		handler.proceed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onReceivedError(android.webkit.WebView,
	 * int, java.lang.String, java.lang.String)
	 */
	@Override
	public void onReceivedError(WebView view, int errorCode,
			String description, String failingUrl) {
		Log.w(this.getClass().getSimpleName(), "Error at " + failingUrl + "\n"
				+ errorCode + " " + description);
		PardusNotification.show(description);
	}

}
