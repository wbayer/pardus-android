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
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import at.pardus.android.browser.js.JavaScriptSettings;
import at.pardus.android.content.LocalContentProvider;

/**
 * Pardus-modded browser component.
 */
public class PardusWebView extends WebView {

	private WebSettings settings;

	private PardusWebViewClient viewClient;

	private PardusWebChromeClient chromeClient;

	private PardusDownloadListener downloadListener;

	private CookieSyncManager cookieSyncManager;

	private CookieManager cookieManager;

	private boolean loggedIn = false;

	private String universe = null;

	/**
	 * Initializes the Pardus browser's behavior.
	 */
	private void init() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Initializing browser component");
		}
		settings = getSettings();
		settings.setSupportMultipleWindows(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(false);
		settings.setAllowFileAccess(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setLoadsImagesAutomatically(true);
		settings.setSaveFormData(true);
		settings.setSavePassword(true);
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Setting up web view client");
		}
		viewClient = new PardusWebViewClient();
		setWebViewClient(viewClient);
		cookieSyncManager = CookieSyncManager.getInstance();
		cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up javascript interface");
		}
		addJavascriptInterface(new JavaScriptSettings(this), "App");
		clearCache(true);
	}

	/**
	 * Sets up and adds a download listener for image packs.
	 * 
	 * @param context
	 *            application context
	 * @param storageDir
	 *            final storage directory
	 * @param cacheDir
	 *            temporary download directory
	 */
	public void initDownloadListener(String storageDir, String cacheDir) {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up download listener");
		}
		downloadListener = new PardusDownloadListener(this, getContext(),
				storageDir, cacheDir);
		setDownloadListener(downloadListener);
	}

	/**
	 * Sets up and adds a web chrome client for window handling.
	 * 
	 * @param activity
	 *            activity hosting web view
	 */
	public void initChromeClient(Activity activity) {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up web chrome client");
		}
		chromeClient = new PardusWebChromeClient(activity);
		setWebChromeClient(chromeClient);
	}

	/**
	 * Displays the local login screen.
	 * 
	 * @param autoLogin
	 *            whether to automatically log in if the account info is stored
	 */
	public void login(boolean autoLogin) {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Showing login screen");
		}
		String imagePath = PardusPreferences.getImagePath();
		if (PardusPreferences.checkImagePath(imagePath)) {
			LocalContentProvider.FILEPATH = imagePath;
		} else {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "No image pack set yet");
			}
			selectImagePack();
			clearHistory();
			return;
		}
		String https = (PardusPreferences.isUseHttps()) ? "https;" : "";
		String auto = (autoLogin) ? "auto;" : "";
		stopLoading();
		loadUrl(PardusConstants.loginScreen + "?" + https + auto);
		cookieManager.removeSessionCookie();
		cookieSyncManager.sync();
		clearHistory();
	}

	/**
	 * Logs out of Pardus.
	 */
	public void logout() {
		if (!loggedIn) {
			// simply forward to the login screen if already logged out
			login(false);
			return;
		}
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Logging out");
		}
		String logoutUrl = (PardusPreferences.isUseHttps()) ? PardusConstants.logoutUrlHttps
				: PardusConstants.logoutUrl;
		stopLoading();
		loadUrl(logoutUrl);
		setUniverse(null);
		clearHistory();
	}

	/**
	 * Displays the local settings screen.
	 */
	public void showSettings() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Loading settings screen");
		}
		stopLoading();
		loadUrl(PardusConstants.settingsScreen);
	}

	/**
	 * Displays the local image pack selection screen.
	 */
	public void selectImagePack() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Loading image pack selection screen");
		}
		stopLoading();
		loadUrl(PardusConstants.imageSelectionScreen);
	}

	/**
	 * Displays the local about screen.
	 */
	public void showAbout() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Loading about screen");
		}
		stopLoading();
		loadUrl(PardusConstants.aboutScreen);
	}

	/**
	 * Loads a universe-specific page if logged in.
	 * 
	 * @param page
	 *            the page to append to the universe url
	 */
	public void loadUniversePage(String page) {
		if (universe == null) {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(),
						"Cannot load universe page (not logged in any universe)");
			}
			return;
		}
		String http = (PardusPreferences.isUseHttps()) ? "https" : "http";
		stopLoading();
		loadUrl(http + "://" + universe + ".pardus.at/" + page);
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 */
	public PardusWebView(Context context) {
		super(context);
		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param attrs
	 */
	public PardusWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public PardusWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * Deletes website cache, cookies and attempts to clear form data.
	 */
	public void removeTraces() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Clearing cache");
		}
		clearCache(true);
		// TODO clearFormData() does not delete the stored account/password
		clearFormData();
		cookieManager.removeSessionCookie();
		cookieManager.removeAllCookie();
	}

	/**
	 * Sets Pardus cookies for custom settings (image path, etc.).
	 */
	public void setCookies() {
		String cookieInfo = "; path=/; domain=.pardus.at;";
		String url = "";
		if (PardusPreferences.isUseHttps()) {
			cookieInfo += " secure;";
			url = PardusConstants.loggedInUrlHttps;
			cookieManager.setCookie(url, "usehttps=1" + cookieInfo);
		} else {
			url = PardusConstants.loggedInUrl;
			cookieManager.setCookie(url, "usehttps=1; max-age=0" + cookieInfo);
		}
		cookieManager.setCookie(url, "image_path=" + LocalContentProvider.URI
				+ cookieInfo);
		cookieManager.setCookie(url, "resolution_tiles=64" + cookieInfo);
		cookieManager.setCookie(url,
				"nav_size=" + PardusPreferences.getNavSize() + cookieInfo);
		cookieSyncManager.sync();
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Cookies set: "
					+ cookieManager.getCookie(url));
		}
	}

	/**
	 * Sets cookies if logged in.
	 * 
	 * @param loggedIn
	 *            true if logged in, false else
	 */
	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
		if (loggedIn) {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Logged in");
			}
			setCookies();
			// output a settings summary
			String settingsStr = "";
			settingsStr += (PardusPreferences.isUseHttps()) ? "Using HTTPS"
					: "Not using HTTPS";
			settingsStr += ". ";
			settingsStr += (PardusPreferences.isLogoutOnHide()) ? "Logging out on hide"
					: "Staying logged in on hide";
			settingsStr += ". ";
			settingsStr += "Space chart size " + PardusPreferences.getNavSize();
			settingsStr += ". ";
			PardusNotification.showLong(settingsStr);
		} else {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Logged out");
			}
		}
	}

	/**
	 * @return the loggedIn
	 */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	/**
	 * @param url
	 *            the url to parse the universe from
	 */
	public void setUniverse(String url) {
		if (url == null) {
			universe = null;
			return;
		}
		if (url.contains("://artemis.pardus.at/")) {
			universe = "artemis";
		} else if (url.contains("://orion.pardus.at/")) {
			universe = "orion";
		} else if (url.contains("://pegasus.pardus.at/")) {
			universe = "pegasus";
		}
	}

	/**
	 * @return the universe
	 */
	public String getUniverse() {
		return universe;
	}

}
