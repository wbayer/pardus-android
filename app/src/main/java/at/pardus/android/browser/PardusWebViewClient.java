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

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import at.pardus.android.browser.PardusWebView.RenderStatus;
import at.pardus.android.browser.js.JavaScriptLinks;
import at.pardus.android.browser.js.JavaScriptLogin;
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

	private static final String jsNewMsgCheck = "if (typeof newMsg != 'undefined' && newMsg) { "
			+ "JavaUtils.refreshNotification(); }";

	private static final String jsHidePrivateInterfaces = JavaScriptLinks.DEFAULT_JS_NAME
			+ " = null; "
            + JavaScriptLogin.DEFAULT_JS_NAME + " = null; "
			+ JavaScriptSettings.DEFAULT_JS_NAME + " = null; "
			+ JavaScriptUtils.DEFAULT_JS_NAME + " = null;";

	private ProgressBar progress;

    /**
     * Executes javascript code on the current web page.
     *
     * @param webView the webview to run the script in
     * @param script the piece of javascript code to run
     */
    private static void evaluateJavascript(WebView webView, String script) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, null);
        } else {
            webView.loadUrl("javascript:" + script);
        }
    }

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
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Attempting to load " + url);
		}
		PardusWebView pardusView = (PardusWebView) view;
		if (!isAllowedUrl(url)) {
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Not loading " + url);
			}
			if (url.equals(PardusConstants.loginUrlOrig)
					|| url.equals(PardusConstants.loginUrlHttpsOrig)) {
				pardusView.login(false);
			} else {
                PardusNotification
                        .showLong("Opening the default browser for " + url);
                view.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
			// abort
			return true;
		}
		pardusView.propertiesBeforePageLoad(url);
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
		if (BuildConfig.DEBUG) {
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
				if (BuildConfig.DEBUG) {
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
				if (BuildConfig.DEBUG) {
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
		} else if (url.equals(PardusConstants.loggedOutUrl)
				|| url.equals(PardusConstants.loggedOutUrlHttps)) {
			// index page: stop loading if redirected from the logout page
			if (pardusView.isLoggingOut()) {
				pardusView.loadUrl("about:blank");
				pardusView.clearHistory();
				pardusView.setLoggingOut(false);
			}
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
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Finished loading " + url);
			Log.v(this.getClass().getSimpleName(),
					"Webview Original URL is set to " + view.getOriginalUrl());
			Log.v(this.getClass().getSimpleName(), "Webview URL is set to "
					+ view.getUrl());
		}
		PardusWebView pardusView = (PardusWebView) view;
		pardusView.propertiesAfterPageLoad();
		progress.setVisibility(View.GONE);
		boolean lookForNewMsg = true;
		if (url.equals(PardusConstants.loginScreen)) {
			// local login page: prefill any stored account data and apply query parameters via javascript
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (PardusPreferences.getStoreCredentials() == PardusPreferences.StoreCredentials.YES) {
                    String account = PardusPreferences.getAccount();
                    String password = PardusPreferences.getPassword();
                    evaluateJavascript(view, "document.getElementById('acc').value = '" + account + "'; " +
                            "document.getElementById('pw').value = '" + password + "';");
                }
                // remove login javascript bridge (takes effect after the next page load)
                view.removeJavascriptInterface(JavaScriptLogin.DEFAULT_JS_NAME);
            }
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(), "Applying query parameters for login screen");
			}
            evaluateJavascript(view, "applyParameters(" + (pardusView.isAutoLogin() ? "true" : "false") + ");");
			view.clearHistory();
			return;
		} else if (url.equals(PardusConstants.loginUrlHttps)
				|| url.equals(PardusConstants.loginUrl)) {
			// login POST target: only finishes loading if login was invalid
			PardusNotification.showLong("Login failed or aborted!");
			pardusView.login(false);
			return;
		} else if (url.contains(PardusConstants.sendMsgPage)) {
			// sendmsg page: redirect back to messages page after sending
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Checking send message page for self.close()");
			}
            evaluateJavascript(view, "(function() { " + jsSkipSendMessageDeath + " })()");
		} else if (url.equals(PardusConstants.loggedInUrl)
				|| url.equals(PardusConstants.loggedInUrlHttps)) {
			// account play page: save the available characters/universes
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Parsing account play page for available universes");
			}
            evaluateJavascript(view, "(function() { " + jsFoundUniverse + " })()");
		} else if (url.contains(PardusConstants.msgPagePrivate)
				|| url.contains(PardusConstants.msgPageAlliance)
				|| url.contains(PardusConstants.tradeLogsPage)
				|| url.contains(PardusConstants.tradeLogsEqPage)
				|| url.contains(PardusConstants.missionsLogPage)
				|| url.contains(PardusConstants.combatLogPage)
				|| url.contains(PardusConstants.paymentLogPage)) {
			// messages/logs page: refresh new messages/logs display
			pardusView.refreshNotification();
			lookForNewMsg = false;
		} else if (url.contains(PardusConstants.bbAcceptFrame)) {
			// bulletin board accept frame: redirect to bulletin board
			pardusView.loadUniversePage(PardusConstants.bulletinBoardPage);
			return;
		}
		if (!isSkippedUrl(url)) {
			// user scripts
			if (!isLocalUrl(url)) {
				runMatchingScripts(view, url, true, jsHidePrivateInterfaces,
						null);
			}
			// new (status) message check
			if (lookForNewMsg && isPardusUniUrl(url)) {
                evaluateJavascript(view, "(function() { " + jsNewMsgCheck + " })()");
			}
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
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Loading resource " + url);
		}
		// new (status) message check after ajax loads
		if (((PardusWebView) view).getRenderStatus() != RenderStatus.LOAD_START
				&& url.contains(".pardus.at/main_ajax.php")) {
			evaluateJavascript(view, "(function() { setTimeout(function() { " + jsNewMsgCheck + " }, 3000) " +
                    "})()");
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebViewClient#onScaleChanged(android.webkit.WebView,
	 * float, float)
	 */
	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Scale changed from " + Math.ceil(oldScale * 100 - 0.5f)
							+ " to " + Math.ceil(newScale * 100 - 0.5f));
		}
		super.onScaleChanged(view, oldScale, newScale);
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
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(), "Redirecting from "
						+ fromPage + " to fallback " + fallbackPage);
			}
			view.loadUniversePage(fallbackPage);
			return;
		}
		if (BuildConfig.DEBUG) {
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
		return (isPardusUniUrl(url) || url.startsWith("http://www.pardus.at/")
				|| url.startsWith("https://www.pardus.at/")
				|| url.startsWith("http://pardus.at/")
				|| url.startsWith("https://pardus.at/")
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
	 * @return true for any Pardus universe URL, false else
	 */
	public static boolean isPardusUniUrl(String url) {
		return (url.startsWith("http://artemis.pardus.at/")
				|| url.startsWith("https://artemis.pardus.at/")
				|| url.startsWith("http://orion.pardus.at/")
				|| url.startsWith("https://orion.pardus.at/")
				|| url.startsWith("http://pegasus.pardus.at/") || url
					.startsWith("https://pegasus.pardus.at/"));
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
	 * @return true for any local content or javascript, about:blank, Pardus
	 *         URLs except account pages; false else
	 */
	public static boolean isAllowedUrlLoggedOut(String url) {
		return (isLocalUrl(url) || url.equals("about:blank")
				|| url.startsWith("https://static.pardus.at/")
				|| url.startsWith("http://static.pardus.at/")
				|| url.startsWith(PardusConstants.loggedInUrlHttps)
				|| url.startsWith(PardusConstants.loggedInUrl)
				|| url.startsWith(PardusConstants.newCharUrl)
				|| url.startsWith(PardusConstants.newCharUrlHttps) || (!url
				.contains("/index.php?section=account_") && (url
				.startsWith("https://www.pardus.at/") || url
				.startsWith("http://www.pardus.at/"))));
	}

	/**
	 * @param url
	 *            URL to check
	 * @return true for Pardus pages that are skipped in the Android app (i.e.
	 *         game.php)
	 */
	public static boolean isSkippedUrl(String url) {
		return (url == null || url.endsWith(".pardus.at/"
				+ PardusConstants.gameFrame));
	}
}
