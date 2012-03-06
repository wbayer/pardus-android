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
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import at.pardus.android.browser.js.JavaScriptLinks;
import at.pardus.android.browser.js.JavaScriptSettings;
import at.pardus.android.browser.js.JavaScriptUtils;
import at.pardus.android.content.LocalContentProvider;
import at.pardus.android.webview.gm.run.WebViewClientGm;
import at.pardus.android.webview.gm.store.ScriptStore;

/**
 * Class overriding the browser's default behavior on certain events.
 */
public class PardusWebViewClient extends WebViewClientGm {

	private static final String jsSkipSendMessageDeath = "if "
			+ "(document.getElementsByTagName('html')[0].innerHTML.indexOf('"
			+ "self.close()" + "') != -1) { " + "top.location.replace('"
			+ PardusConstants.msgPage + "'); }";

	private static final String jsFoundUniverse = "var htmlSource = "
			+ "document.getElementsByTagName('html')[0].innerHTML; "
			+ "JavaUtils.foundUniverse((htmlSource.indexOf('universe=Artemis') != -1), "
			+ "(htmlSource.indexOf('universe=Orion') != -1), "
			+ "(htmlSource.indexOf('universe=Pegasus') != -1));";

	private static final String jsHidePrivateInterfaces = JavaScriptLinks.DEFAULT_JS_NAME
			+ " = null; "
			+ JavaScriptSettings.DEFAULT_JS_NAME
			+ " = null; "
			+ JavaScriptUtils.DEFAULT_JS_NAME + " = null;";

	private ProgressBar progress;

	/**
	 * Constructor.
	 * 
	 * @param scriptStore
	 *            the script database to query for scripts to run when a page
	 *            starts/finishes loading
	 * @param jsBridgeName
	 *            the variable name to access the webview GM functions from
	 *            javascript code
	 * @param secret
	 *            a random string that is added to calls of the GM API
	 * @param progress
	 *            the loading progress bar of the browser
	 */
	public PardusWebViewClient(ScriptStore scriptStore, String jsBridgeName,
			String secret, ProgressBar progress) {
		super(scriptStore, jsBridgeName, secret);
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
		// non-frame target user actions and redirects might trigger this
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Attempting to load " + url);
		}
		PardusWebView pardusView = (PardusWebView) view;
		if (!isAllowedUrl(url)) {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Not loading " + url);
			}
			if (url.equals(PardusConstants.loginUrlOrig)
					|| url.equals(PardusConstants.loginUrlHttpsOrig)) {
				pardusView.login(false);
			} else {
				PardusNotification
						.showLong("The following URL is not permitted to be opened in the Pardus App: "
								+ url);
			}
			// abort
			return true;
		}
		// continue
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView,
	 * java.lang.String, android.graphics.Bitmap)
	 */
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		// triggered by anything changing the URL (non-frame target)
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Started loading " + url);
			Log.v(this.getClass().getSimpleName(),
					"Webview Original URL is set to " + view.getOriginalUrl());
			Log.v(this.getClass().getSimpleName(), "Webview URL is set to "
					+ view.getUrl());
		}
		PardusWebView pardusView = (PardusWebView) view;
		// URL checks again due to shouldOverrideUrlLoading being unreliable
		// redirecting to local login page if access to the URL is disallowed
		if (!pardusView.isLoggedIn()) {
			if (!isAllowedUrlLoggedOut(url)) {
				if (PardusConstants.DEBUG) {
					Log.d(this.getClass().getSimpleName(),
							"Access to "
									+ url
									+ " denied while not logged in, redirecting to local login page");
				}
				pardusView.login(false);
				return;
			}
		} else {
			if (!isAllowedUrl(url)) {
				if (PardusConstants.DEBUG) {
					Log.d(this.getClass().getSimpleName(), "Access to " + url
							+ " denied, redirecting to local login page");
				}
				pardusView.login(false);
				return;
			}
		}
		// URL checks OK
		pardusView.setUniverse(url);
		if (url.startsWith(PardusConstants.loggedInUrl)
				|| url.startsWith(PardusConstants.loggedInUrlHttps)
				|| url.startsWith(PardusConstants.newCharUrl)
				|| url.startsWith(PardusConstants.newCharUrlHttps)) {
			// account play or new char page: set loggedIn true and continue
			pardusView.setLoggedIn(true);
		} else if (url.equals(PardusConstants.logoutUrl)
				|| url.equals(PardusConstants.logoutUrlHttps)) {
			// logout page: set loggedIn false and continue
			pardusView.setLoggedIn(false);
		} else if (url.endsWith(".pardus.at/" + PardusConstants.gameFrame)) {
			// game frame (without params): redirect to previous page or nav
			redirectBack(pardusView, PardusConstants.gameFrame,
					PardusConstants.navPage);
			return;
		} else if (url.endsWith(".pardus.at/" + PardusConstants.msgFrame)) {
			// msg frame (without params): redirect to previous page or bb
			redirectBack(pardusView, PardusConstants.msgFrame,
					PardusConstants.bulletinBoardPage);
			return;
		}
		progress.setProgress(0);
		progress.setVisibility(View.VISIBLE);
		// user scripts
		if (!isLocalUrl(url)) {
			runMatchingScripts(view, url, false, jsHidePrivateInterfaces, null);
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
		// triggered when a URL has completed loading and is being displayed
		// if a page sends a redirect header only the next page triggers this
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Finished loading " + url);
			Log.v(this.getClass().getSimpleName(),
					"Webview Original URL is set to " + view.getOriginalUrl());
			Log.v(this.getClass().getSimpleName(), "Webview URL is set to "
					+ view.getUrl());
		}
		PardusWebView pardusView = (PardusWebView) view;
		progress.setVisibility(View.GONE);
		if (url.equals(PardusConstants.loginScreen)) {
			// local login page: apply query parameters via javascript
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Applying query parameters for login screen");
			}
			view.loadUrl("javascript:applyParameters("
					+ (PardusPreferences.isUseHttps() ? "true" : "false")
					+ ", " + (pardusView.isAutoLogin() ? "true" : "false")
					+ ");");
		} else if (url.equals(PardusConstants.loginUrlHttps)
				|| url.equals(PardusConstants.loginUrl)) {
			// login POST target: only finishes loading if login was invalid
			PardusNotification.showLong("Login failed!");
			pardusView.login(false);
			return;
		} else if (url.contains(PardusConstants.sendMsgPage)) {
			// sendmsg page: redirect back to messages page after sending
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Checking send message page for self.close()");
			}
			view.loadUrl("javascript:(function() { " + jsSkipSendMessageDeath
					+ " })()");
		} else if (url.equals(PardusConstants.loggedInUrl)
				|| url.equals(PardusConstants.loggedInUrlHttps)) {
			// account play page: save the available characters/universes
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Parsing account play page for available universes");
			}
			view.loadUrl("javascript:(function() { " + jsFoundUniverse
					+ " })()");
		} else if (url.contains(PardusConstants.msgPagePrivate)
				|| url.contains(PardusConstants.msgPageAlliance)
				|| url.contains(PardusConstants.tradeLogsPage)
				|| url.contains(PardusConstants.tradeLogsEqPage)
				|| url.contains(PardusConstants.missionsLogPage)
				|| url.contains(PardusConstants.combatLogPage)
				|| url.contains(PardusConstants.paymentLogPage)) {
			// messages/logs page: refresh new messages/logs display
			pardusView.refreshNotification();
		} else if (url.contains(PardusConstants.bbAcceptFrame)) {
			// bulletin board accept frame: redirect to bulletin board
			pardusView.loadUniversePage(PardusConstants.bulletinBoardPage);
			return;
		}
		// user scripts
		if (!isLocalUrl(url)) {
			runMatchingScripts(view, url, true, jsHidePrivateInterfaces, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onLoadResource(android.webkit.WebView,
	 * java.lang.String)
	 */
	@Override
	public void onLoadResource(WebView view, String url) {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Loading resource " + url);
		}
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

	/**
	 * Redirects the browser from a specified (universe) page back to the
	 * previous location or, if it is the same as the current one or not
	 * belonging to the same universe, redirects it to a specified fallback
	 * (universe) page.
	 * 
	 * @param view
	 *            Pardus browser
	 * @param fromPage
	 *            page to redirect from
	 * @param fallbackPage
	 *            page to use as fallback
	 */
	private void redirectBack(PardusWebView view, String fromPage,
			String fallbackPage) {
		view.stopLoading();
		String previousUrl = view.getOriginalUrl();
		if (previousUrl.contains(".pardus.at/" + fromPage)
				|| (!previousUrl.contains("://artemis.pardus.at/")
						&& !previousUrl.contains("://orion.pardus.at/") && !previousUrl
							.contains("://pegasus.pardus.at/"))
				|| !previousUrl.contains(view.getUniverse())) {
			// prev page is the page to redirect from or
			// not uni-specific or another uni: redirect to fallback page
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(), "Redirecting from "
						+ fromPage + " to fallback " + fallbackPage);
			}
			view.loadUniversePage(fallbackPage);
			return;
		}
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Redirecting from "
					+ fromPage + " back to " + previousUrl);
		}
		view.loadUrl(previousUrl);
	}

	/**
	 * @param url
	 *            URL to check
	 * @return true for any Pardus URL, false else
	 */
	public static boolean isPardusUrl(String url) {
		return (url.startsWith("http://www.pardus.at/")
				|| url.startsWith("https://www.pardus.at/")
				|| url.startsWith("http://pardus.at/")
				|| url.startsWith("https://pardus.at/")
				|| url.startsWith("http://artemis.pardus.at/")
				|| url.startsWith("https://artemis.pardus.at/")
				|| url.startsWith("http://orion.pardus.at/")
				|| url.startsWith("https://orion.pardus.at/")
				|| url.startsWith("http://pegasus.pardus.at/")
				|| url.startsWith("https://pegasus.pardus.at/")
				|| url.startsWith("http://chat.pardus.at/")
				|| url.startsWith("https://chat.pardus.at/")
				|| url.startsWith("http://forum.pardus.at/")
				|| url.startsWith("https://forum.pardus.at/")
				|| url.startsWith("http://static.pardus.at/") || url
					.startsWith("https://static.pardus.at/"));
	}

	/**
	 * @param url
	 *            URL to check
	 * @return true for any local content or javascript, false else
	 */
	public static boolean isLocalUrl(String url) {
		return (url.startsWith("file:///android_asset/")
				|| url.startsWith(LocalContentProvider.URI) || url
					.startsWith("javascript:"));
	}

	/**
	 * Decides whether a URL is allowed to be loaded in the Pardus app.
	 * 
	 * @param url
	 *            URL to check
	 * @return true for any Pardus URL but the online login and for any local
	 *         content, false for anything else
	 */
	public static boolean isAllowedUrl(String url) {
		if (url.equals(PardusConstants.loginUrlOrig)
				|| url.equals(PardusConstants.loginUrlHttpsOrig)) {
			return false;
		}
		return (isPardusUrl(url) || isLocalUrl(url));
	}

	/**
	 * @param url
	 *            URL to check
	 * @return true for any local content or javascript, Pardus URLs except
	 *         account pages; false else
	 */
	public static boolean isAllowedUrlLoggedOut(String url) {
		return (isLocalUrl(url) || url.startsWith("https://static.pardus.at/")
				|| url.startsWith("http://static.pardus.at/")
				|| url.startsWith(PardusConstants.loggedInUrlHttps)
				|| url.startsWith(PardusConstants.loggedInUrl)
				|| url.startsWith(PardusConstants.newCharUrl)
				|| url.startsWith(PardusConstants.newCharUrlHttps) || (!url
				.contains("/index.php?section=account_") && (url
				.startsWith("https://www.pardus.at/") || url
				.startsWith("http://www.pardus.at/"))));
	}
}
