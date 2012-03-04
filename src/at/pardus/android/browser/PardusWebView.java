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
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebViewDatabase;
import android.widget.ProgressBar;
import at.pardus.android.browser.js.JavaScriptLinks;
import at.pardus.android.browser.js.JavaScriptSettings;
import at.pardus.android.browser.js.JavaScriptUtils;
import at.pardus.android.content.LocalContentProvider;
import at.pardus.android.webview.gm.run.WebViewGm;

/**
 * Pardus-modded browser component.
 */
public class PardusWebView extends WebViewGm {

	private WebSettings settings;

	private PardusWebViewClient viewClient;

	private PardusWebChromeClient chromeClient;

	private PardusMessageChecker messageChecker;

	private PardusDownloadListener downloadListener;

	private WebViewDatabase database;

	private CookieSyncManager cookieSyncManager;

	private CookieManager cookieManager;

	private GestureDetector gestureDetector;

	private PardusLinks links;

	private int menuSensitivity;

	private boolean scrolling = false;

	private boolean loggedIn = false;

	private String universe = null;

	private boolean autoLogin;

	/**
	 * Initializes the Pardus browser's behavior.
	 */
	private void init() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Initializing browser component");
		}
		// the following style is ignored when only defined in the xml file
		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		settings = getSettings();
		settings.setSupportMultipleWindows(false);
		settings.setPluginsEnabled(false);
		settings.setGeolocationEnabled(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(false);
		settings.setAllowFileAccess(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setLoadsImagesAutomatically(true);
		settings.setSaveFormData(true);
		settings.setSavePassword(true);
		settings.setDatabaseEnabled(true);
		database = WebViewDatabase.getInstance(this.getContext());
		cookieSyncManager = CookieSyncManager.getInstance();
		cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up javascript interfaces");
		}
		setMenuSensitivity(PardusPreferences.getMenuSensitivity());
		addJavascriptInterface(new JavaScriptSettings(this), "JavaSettings");
		addJavascriptInterface(new JavaScriptUtils(), "JavaUtils");
		clearCache(true);
	}

	/**
	 * Sets up and adds a web view client for browser events and a web chrome
	 * client for window handling.
	 * 
	 * @param progress
	 *            the loading progress bar of the browser
	 * @param messageChecker
	 *            the message checker to share cookies with
	 */
	public void initClients(ProgressBar progress,
			PardusMessageChecker messageChecker) {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Setting up web view client");
		}
		this.messageChecker = messageChecker;
		viewClient = new PardusWebViewClient(getScriptStore(),
				getWebViewClient().getJsBridgeName(), getWebViewClient()
						.getSecret(), progress);
		setWebViewClient(viewClient);
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up web chrome client");
		}
		chromeClient = new PardusWebChromeClient(progress, messageChecker);
		setWebChromeClient(chromeClient);
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
	 * Sets up the javascript interface to edit the Pardus links and initializes
	 * a gesture detector to display them whenever the view is scrolled to a
	 * border on the y-axis.
	 * 
	 * @param l
	 *            PardusLinks object to show
	 */
	public void initLinks(PardusLinks l) {
		links = l;
		addJavascriptInterface(new JavaScriptLinks(this, l), "JavaLinks");
		gestureDetector = new GestureDetector(new SimpleOnGestureListener() {

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				scrolling = true;
				if (PardusConstants.DEBUG) {
					Log.v(this.getClass().getSimpleName(),
							"onScroll: " + e1.getX() + "/" + e1.getY() + " -> "
									+ e2.getX() + "/" + e2.getY()
									+ ", distance " + distanceX + "/"
									+ distanceY);
				}
				float newPosY = getScrollY() + distanceY;
				if (distanceY != 0
						&& menuSensitivity >= 0
						&& (newPosY <= menuSensitivity * (-1) || newPosY >= computeVerticalScrollRange()
								- getHeight() + menuSensitivity)) {
					links.show();
				}
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				if (PardusConstants.DEBUG) {
					Log.v(this.getClass().getSimpleName(),
							"onLongPress: " + e.getX() + "/" + e.getY());
				}
				if (Build.VERSION.SDK_INT < 11) {
					// v2.3- webviews need help going into selection mode
					fakeEmulateShiftHeld();
				}
				return;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				if (PardusConstants.DEBUG) {
					Log.v(this.getClass().getSimpleName(),
							"onFling: " + e1.getX() + "/" + e1.getY() + " -> "
									+ e2.getX() + "/" + e2.getY()
									+ ", velocity " + velocityX + "/"
									+ velocityY);
				}
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				if (PardusConstants.DEBUG) {
					Log.v(this.getClass().getSimpleName(),
							"onDown: " + e.getX() + "/" + e.getY());
				}
				return true;
			}

		});
		gestureDetector.setIsLongpressEnabled(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		gestureDetector.onTouchEvent(ev);
		if (ev.getAction() == MotionEvent.ACTION_UP) {
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(), "onUp: " + ev.getX()
						+ "/" + ev.getY());
			}
			if (scrolling) {
				scrolling = false;
				if (PardusConstants.DEBUG) {
					Log.v(this.getClass().getSimpleName(), "Scrolling ended");
				}
				if (links != null) {
					links.startHideTimer(PardusLinks.HIDE_AFTER_SHOW_MILLIS);
				}
			}
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * Switches into text selection mode.
	 */
	public void fakeEmulateShiftHeld() {
		try {
			KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
					KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
			shiftPressEvent
					.dispatch(this, new KeyEvent.DispatcherState(), null);
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(),
					"Exception faking emulateShiftHeld() method", e);
		}
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
		this.autoLogin = autoLogin;
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
		stopLoading();
		loadUrl(PardusConstants.loginScreen);
		cookieManager.removeSessionCookie();
		cookieSyncManager.sync();
		setUniverse(null);
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
	 * Enters a given universe.
	 * 
	 * @param newUni
	 *            universe to enter
	 */
	public void switchUniverse(String newUni) {
		if (!loggedIn) {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(),
						"Cannot enter universe (not logged in)");
			}
			return;
		}
		String loggedInUrl = (PardusPreferences.isUseHttps()) ? PardusConstants.loggedInUrlHttps
				: PardusConstants.loggedInUrl;
		stopLoading();
		loadUrl(loggedInUrl + "&universe=" + newUni);
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
	 * Sets Pardus cookies for custom settings (image path, etc.).
	 */
	public void setCookies() {
		String cookieInfo = "; path=/; domain=.pardus.at;";
		String url = "";
		if (PardusPreferences.isUseHttps()) {
			url = PardusConstants.loggedInUrlHttps;
			cookieManager.setCookie(url, "usehttps=1" + cookieInfo);
			cookieInfo += " secure;";
		} else {
			url = PardusConstants.loggedInUrl;
			cookieManager.setCookie(url, "usehttps=1; max-age=0" + cookieInfo);
		}
		cookieManager.setCookie(url, "image_path=" + LocalContentProvider.URI
				+ cookieInfo);
		cookieManager.setCookie(url, "resolution_tiles=64" + cookieInfo);
		cookieManager.setCookie(url,
				"nav_size=" + PardusPreferences.getNavSize() + cookieInfo);
		cookieManager.setCookie(url,
				"partial_refresh="
						+ ((PardusPreferences.isPartialRefresh()) ? "1" : "0")
						+ cookieInfo);
		cookieManager.setCookie(url,
				"ship_animation="
						+ ((PardusPreferences.isShipAnimation()) ? "1" : "0")
						+ cookieInfo);
		cookieManager.setCookie(url,
				"ship_rotation="
						+ ((PardusPreferences.isShipRotation()) ? "1" : "0")
						+ cookieInfo);
		cookieManager.setCookie(url,
				"mobile_chat="
						+ ((PardusPreferences.isMobileChat()) ? "1" : "0")
						+ cookieInfo);
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
			settingsStr += "\n";
			settingsStr += (PardusPreferences.isLogoutOnHide()) ? "Logging out on hide"
					: "Staying logged in on hide";
			settingsStr += "\n";
			settingsStr += "Space chart size " + PardusPreferences.getNavSize();
			settingsStr += "\n";
			settingsStr += "Partial refreshing "
					+ ((PardusPreferences.isPartialRefresh() ? "enabled"
							: "disabled"));
			settingsStr += "\n";
			settingsStr += "Ship animation "
					+ ((PardusPreferences.isShipAnimation() ? "enabled"
							: "disabled"));
			settingsStr += "\n";
			settingsStr += "Ship rotation "
					+ ((PardusPreferences.isShipRotation() ? "enabled"
							: "disabled"));
			settingsStr += "\n";
			settingsStr += "Chat lines limit "
					+ ((PardusPreferences.isMobileChat() ? "enabled"
							: "disabled"));
			PardusNotification.showLong(settingsStr);
		} else {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Logged out");
			}
			destroySession();
		}
	}

	/**
	 * Destroys the session locally.
	 * 
	 * This means deleting all cookies used for authorization in the browser and
	 * message checker.
	 */
	public void destroySession() {
		String cookieInfo = "; path=/; domain=.pardus.at;";
		cookieManager.setCookie(PardusConstants.loggedInUrlHttps,
				"accountid=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrl,
				"accountid=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrlHttps,
				"sessionid=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrl,
				"sessionid=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrlHttps,
				"pardus_cookie=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrl,
				"pardus_cookie=0; max-age=0" + cookieInfo);
		cookieManager.removeExpiredCookie();
		cookieManager.removeSessionCookie();
		messageChecker.setUniverse(null, null);
	}

	/**
	 * Deletes website cache, cookies and any stored form data.
	 */
	public void removeTraces() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Clearing cache");
		}
		if (database != null) {
			database.clearUsernamePassword();
			database.clearFormData();
		}
		clearFormData();
		clearCache(true);
		cookieManager.removeSessionCookie();
		cookieManager.removeAllCookie();
		setUniverse(null);
	}

	/**
	 * Restarts the message checking background task to have it update the new
	 * messages/logs display immediately.
	 */
	public void refreshNotification() {
		messageChecker.restart();
	}

	/**
	 * Sets the menu sensitivity in px.
	 * 
	 * @param menuSensitivity
	 *            the menuSensitivity in inches * 10
	 */
	public void setMenuSensitivity(int menuSensitivity) {
		this.menuSensitivity = Math.round(menuSensitivity * 16
				* Pardus.displayDensityScale);
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
			messageChecker.setUniverse(null, null);
			return;
		}
		if (url.startsWith("http://artemis.pardus.at/")
				|| url.startsWith("https://artemis.pardus.at/")) {
			universe = "artemis";
		} else if (url.startsWith("http://orion.pardus.at/")
				|| url.startsWith("https://orion.pardus.at/")) {
			universe = "orion";
		} else if (url.startsWith("http://pegasus.pardus.at/")
				|| url.startsWith("https://pegasus.pardus.at/")) {
			universe = "pegasus";
		}
		messageChecker.setUniverse(universe, cookieManager.getCookie(url));
	}

	/**
	 * @return the universe
	 */
	public String getUniverse() {
		return universe;
	}

	/**
	 * @return whether to automatically log in
	 */
	public boolean isAutoLogin() {
		return autoLogin;
	}

	/**
	 * Goes back to the previous location.
	 * 
	 * @return false if the browser history is empty
	 */
	public boolean back() {
		if (canGoBack()) {
			goBack();
			return true;
		}
		return false;
	}

}
