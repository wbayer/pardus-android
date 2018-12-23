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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebViewDatabase;
import android.widget.ProgressBar;

import java.util.Date;

import at.pardus.android.browser.PardusPageProperties.PardusPageProperty;
import at.pardus.android.browser.js.JavaScriptLinks;
import at.pardus.android.browser.js.JavaScriptLogin;
import at.pardus.android.browser.js.JavaScriptSettings;
import at.pardus.android.browser.js.JavaScriptUtils;
import at.pardus.android.content.LocalContentProxy;
import at.pardus.android.webview.gm.run.WebViewGm;

/**
 * Pardus-modded browser component.
 */
public class PardusWebView extends WebViewGm {

	public enum RenderStatus {
		LOAD_START, LOAD_FINISH
	}

	private WebSettings settings;

	private Activity activity;

    private PardusMessageChecker messageChecker;

	private PardusDownloadListener downloadListener;

	private PardusPageProperties pageProperties;

	private PardusLinks links;

	private WebViewDatabase database;

	private CookieManager cookieManager;

	private GestureDetector gestureDetector;

    private int defaultInitialScale;

	private RenderStatus renderStatus = RenderStatus.LOAD_FINISH;

	private volatile boolean touchedAfterPageLoad = false;

	private volatile PardusPageProperty initialScroll = PardusPageProperties
			.getNoScrollProperty();

	private volatile boolean scrolling = false;

	private boolean loggedIn = false;

	private boolean loggingOut = false;

	private String universe = null;

	private boolean autoLogin;

	private int menuSensitivity;

	public PardusWebView(Context context) {
		super(context);
		init();
	}

	public PardusWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PardusWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * Initializes the Pardus browser's behavior.
	 */
	@SuppressLint({"SetJavaScriptEnabled", "InlinedApi"})
	private void init() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Initializing browser component");
		}
		// the following style is ignored when only defined in the xml file
		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		settings = getSettings();
		settings.setSupportMultipleWindows(false);
		settings.setGeolocationEnabled(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(false);
		settings.setAllowFileAccess(true);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		setShowZoomControls(PardusPreferences.isShowZoomControls());
		settings.setLoadsImagesAutomatically(true);
		settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		settings.setDatabaseEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " Pardus/" +
                PardusPreferences.getVersionCode());
		SharedPreferences prefs = getContext().getSharedPreferences(
				"WebViewSettings", Context.MODE_PRIVATE);
		if (prefs.getInt("double_tap_toast_count", 1) > 0) {
			// attempt to not display the automatic "double-tap tip"
			prefs.edit().putInt("double_tap_toast_count", 0).apply();
		}
		database = WebViewDatabase.getInstance(getContext());
		cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		setRememberPageProperties(PardusPreferences.isRememberPageProperties());
		setMenuSensitivity(PardusPreferences.getMenuSensitivity());
		clearCache(true);
		// default scales: 240dpi -> 150, 160dpi -> 100, 120dpi -> 75
		if (Pardus.displayDpi <= 160 || Pardus.isTablet) {
			defaultInitialScale = Math.round(Pardus.displayDpi / 1.6f);
		} else {
			// start 240dpi screens zoomed out if it's not a tablet
			defaultInitialScale = Math.round(Pardus.displayDpi / 2.4f);
		}
		PardusPreferences.init(null, defaultInitialScale);
		resetInitialScale();
	}

	/**
	 * Sets up and adds a web view client for browser events and a web chrome
	 * client for window handling.
	 * 
	 * 
	 * @param activity
	 *            the activity this browser component runs in
	 * @param progress
	 *            the loading progress bar of the browser
	 * @param messageChecker
	 *            the message checker to share cookies with
	 */
    @SuppressLint("NewApi")
	public void initClients(Activity activity, ProgressBar progress,
			PardusMessageChecker messageChecker) {
		this.activity = activity;
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Setting up web view client");
		}
		this.messageChecker = messageChecker;
        PardusWebViewClient viewClient = new PardusWebViewClient(getScriptStore(), getWebViewClient()
                .getJsBridgeName(), getWebViewClient().getSecret(), progress);
		setWebViewClient(viewClient);
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up web chrome client");
		}
        PardusWebChromeClient chromeClient = new PardusWebChromeClient(progress);
		setWebChromeClient(chromeClient);
	}

	/**
	 * Sets up the bridges between Java and Javascript.
	 */
	@SuppressLint("AddJavascriptInterface")
    public void initJavascriptBridges() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up javascript interfaces");
		}
		addJavascriptInterface(new JavaScriptSettings(this, activity),
				JavaScriptSettings.DEFAULT_JS_NAME);
        addJavascriptInterface(new JavaScriptUtils(this),
				JavaScriptUtils.DEFAULT_JS_NAME);
	}

	/**
	 * Sets up and adds a download listener for image packs.
	 *
	 * @param storageDir
	 *            final storage directory
	 * @param cacheDir
	 *            temporary download directory
	 */
	public void initDownloadListener(String storageDir, String cacheDir) {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Setting up download listener");
		}
		downloadListener = new PardusDownloadListener(this, getContext(),
				storageDir, storageDir, cacheDir);
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
	@SuppressLint("AddJavascriptInterface")
    public void initLinks(PardusLinks l) {
		links = l;
		addJavascriptInterface(new JavaScriptLinks(this, l),
				JavaScriptLinks.DEFAULT_JS_NAME);
		gestureDetector = new GestureDetector(getContext(),
				new SimpleOnGestureListener() {

					private int scrollRangeY;

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						scrolling = true;
						if (BuildConfig.DEBUG) {
							Log.v(PardusWebView.class.getSimpleName(),
									"onScroll: " + e1.getX() + "/" + e1.getY()
											+ " -> " + e2.getX() + "/"
											+ e2.getY() + ", distance "
											+ distanceX + "/" + distanceY);
						}
						float newPosY = getScrollY() + distanceY;
						if (distanceY != 0
								&& menuSensitivity >= 0
								&& (newPosY <= menuSensitivity * (-1) || newPosY >= scrollRangeY
										- getHeight() + menuSensitivity)) {
							links.show();
						}
						return false;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						if (BuildConfig.DEBUG) {
							Log.v(PardusWebView.class.getSimpleName(),
									"onFling: " + e1.getX() + "/" + e1.getY()
											+ " -> " + e2.getX() + "/"
											+ e2.getY() + ", velocity "
											+ velocityX + "/" + velocityY);
						}
						return false;
					}

					@Override
					public boolean onDown(MotionEvent e) {
						scrollRangeY = computeVerticalScrollRange();
						touchedAfterPageLoad = true;
						if (BuildConfig.DEBUG) {
							Log.v(PardusWebView.class.getSimpleName(),
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
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(), "onUp: " + ev.getX()
						+ "/" + ev.getY());
			}
			if (scrolling) {
				scrolling = false;
				if (BuildConfig.DEBUG) {
					Log.v(this.getClass().getSimpleName(), "Scrolling ended");
				}
				// start timer to hide links bar if shown
				if (links != null) {
					links.startHideTimer(PardusLinks.HIDE_AFTER_SHOW_MILLIS);
				}
			}
		}
        return super.onTouchEvent(ev);
	}

	/**
	 * Saves the zoom level and scroll position of the currently opened page.
	 */
    private void savePageProperties() {
		if (pageProperties != null) {
            pageProperties.save(getUrl(), Pardus.orientation,
                    ((PardusWebViewClient) getWebViewClient()).getScale(),
                    computeHorizontalScrollOffset(),
                    computeVerticalScrollOffset(),
                    computeHorizontalScrollRange(),
					computeVerticalScrollRange());
		}
	}

	/**
	 * Displays the local login screen.
	 * 
	 * @param autoLogin
	 *            whether to automatically log in if the account info is stored
	 */
	@SuppressLint("AddJavascriptInterface")
    public void login(boolean autoLogin) {
        if (BuildConfig.DEBUG) {
            Log.v(this.getClass().getSimpleName(), "Showing login screen");
		}
		this.autoLogin = autoLogin;
		String imagePath = PardusPreferences.getImagePath();
		PardusImagePack imagePack = new PardusImagePack(imagePath);
		if (imagePack.isInstalled()) {
			downloadListener.setUpdateStorageDir(imagePath);
			Date now = new Date();
			Date nextCheck = PardusPreferences.getNextImagePackUpdateCheck();
			if (!nextCheck.after(now)) {
				imagePack.updateCheck(activity);
				PardusPreferences.setNextImagePackUpdateCheck(new Date(now
						.getTime() + 86400000));
			}
		} else {
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "No image pack set yet");
			}
            selectImagePack();
			clearHistory();
			return;
		}
		stopLoading();
		addJavascriptInterface(new JavaScriptLogin(activity), JavaScriptLogin.DEFAULT_JS_NAME);
        loadUrl(PardusConstants.loginScreen);
		cookieManager.removeSessionCookies(null);
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
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Logging out");
		}
		loggingOut = true;
		stopLoading();
        loadUrl(PardusConstants.logoutUrlHttps);
		setUniverse(null);
		clearHistory();
	}

	/**
	 * Displays the local settings screen.
	 */
	public void showSettings() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Loading settings screen");
		}
		stopLoading();
		loadUrl(PardusConstants.settingsScreen);
	}

	/**
	 * Displays the local image pack selection screen.
	 */
	public void selectImagePack() {
		if (BuildConfig.DEBUG) {
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
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(),
						"Cannot load universe page (not logged in any universe)");
			}
			return;
		}
		stopLoading();
		loadUrl(PardusConstants.getUniverseUrl(universe, page));
	}

	/**
	 * Enters a given universe.
	 * 
	 * @param newUni
	 *            universe to enter
	 */
	public void switchUniverse(String newUni) {
		if (!loggedIn) {
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(),
						"Cannot enter universe (not logged in)");
			}
			return;
		}
		stopLoading();
		loadUrl(PardusConstants.loggedInUrlHttps + "&universe=" + newUni);
	}

	/**
	 * Sets Pardus cookies for custom settings (image path, etc.).
	 */
	public void setCookies() {
		String cookieInfo = "; path=/; domain=.pardus.at; secure;";
		String url = PardusConstants.loggedInUrlHttps;
        cookieManager.setCookie(url, "usehttps=1" + cookieInfo);
		String imagePathUri = LocalContentProxy.getInstance().getUri();
        if (imagePathUri != null) {
			cookieManager.setCookie(url, "image_path=" + imagePathUri + cookieInfo);
		}
		cookieManager.setCookie(url, "resolution_tiles=64" + cookieInfo);
		cookieManager.setCookie(url,
				"nav_size_hor=" + PardusPreferences.getNavSizeHor()
						+ cookieInfo);
		cookieManager.setCookie(url,
				"nav_size_ver=" + PardusPreferences.getNavSizeVer()
						+ cookieInfo);
		cookieManager.setCookie(url, "nav_size_dyn=0" + cookieInfo);
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
		cookieManager.setCookie(url, "mobile_chat=" + ((PardusPreferences.isMobileChat()) ? "1" : "0") +
                cookieInfo);
		if (BuildConfig.DEBUG) {
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
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Logged in");
			}
			setCookies();
		} else {
			if (BuildConfig.DEBUG) {
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
		cookieManager.setCookie(PardusConstants.loggedInUrlHttps,
				"sessionid=0; max-age=0" + cookieInfo);
		cookieManager.setCookie(PardusConstants.loggedInUrlHttps,
				"pardus_cookie=0; max-age=0" + cookieInfo);
		cookieManager.removeSessionCookies(null);
		messageChecker.setUniverse(null, null);
	}

	/**
	 * Deletes website cache, cookies, page properties and any stored form data.
	 */
	@SuppressWarnings("deprecation")
    public void removeTraces() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Clearing cache");
		}
		if (database != null) {
            database.clearUsernamePassword();
			database.clearFormData();
		}
        PardusPreferences.setStoreCredentials(PardusPreferences.StoreCredentials.NO);
		clearFormData();
		clearCache(true);
		cookieManager.removeSessionCookie();
		cookieManager.removeAllCookie();
		if (pageProperties != null) {
			pageProperties.forget();
		}
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
	 * @return the PardusPageProperties object used by this browser component
	 */
	public PardusPageProperties getPageProperties() {
		return pageProperties;
	}

	/**
	 * @param rememberPageProperties
	 *            whether to remember page-specific properties like the zoom
	 *            level and scrolling position
	 */
	public void setRememberPageProperties(boolean rememberPageProperties) {
		if (rememberPageProperties) {
			if (pageProperties == null) {
				pageProperties = new PardusPageProperties(getContext());
			}
		} else {
			if (pageProperties != null) {
				pageProperties.forget();
				pageProperties = null;
			}
		}
	}

	/**
	 * Resets the webview's zoom level to the configured default scale.
	 */
    private void resetInitialScale() {
		setInitialScale(defaultInitialScale);
	}

	/**
	 * @param showZoomControls
	 *            decides whether to show the zoom control buttons in the
	 *            browser component (always shown if the phone does not support
	 *            multi touch)
	 */
	public void setShowZoomControls(boolean showZoomControls) {
        settings.setDisplayZoomControls(showZoomControls || !getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) && !getContext()
                .getPackageManager().hasSystemFeature(PackageManager
                        .FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT));
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
	 * @return whether the user is logged in
	 */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	/**
	 * @return whether a logout action is in process
	 */
	public boolean isLoggingOut() {
		return loggingOut;
	}

	/**
	 * @param loggingOut
	 *            true at the start of any logout action, false once completed
	 */
	public void setLoggingOut(boolean loggingOut) {
		this.loggingOut = loggingOut;
	}

	/**
	 * @param url
	 *            the url to parse the universe from
	 */
	public void setUniverse(String url) {
		String newUniverse;
		if (url == null) {
			newUniverse = null;
		} else if (url.startsWith("https://artemis.pardus.at/")) {
			newUniverse = "artemis";
		} else if (url.startsWith("https://orion.pardus.at/")) {
			newUniverse = "orion";
		} else if (url.startsWith("https://pegasus.pardus.at/")) {
			newUniverse = "pegasus";
		} else {
			return;
		}
		boolean uniChange = false;
		if (newUniverse == null) {
			if (universe != null) {
				uniChange = true;
			}
		} else {
			if (!newUniverse.equals(universe)) {
				uniChange = true;
			}
		}
		if (uniChange) {
			universe = newUniverse;
			messageChecker.setUniverse(newUniverse, newUniverse == null ? null
					: cookieManager.getCookie(url));
            activity.runOnUiThread(() -> activity.invalidateOptionsMenu());
		}
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
	 * @return the renderStatus
	 */
	public RenderStatus getRenderStatus() {
		return renderStatus;
	}

	/**
	 * @param renderStatus
	 *            the renderStatus to set
	 */
    private void setRenderStatus(RenderStatus renderStatus) {
		this.renderStatus = renderStatus;
	}

	/**
	 * Saves the current page's properties and sets the initial scale of the
	 * page to be loaded.
	 * 
	 * @param url
	 *            the page to be loaded
	 */
	public void propertiesBeforePageLoad(String url) {
		if (url.startsWith("javascript:")) {
			return;
		}
		setRenderStatus(RenderStatus.LOAD_START);
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"New render status: LOAD_START");
		}
		if (pageProperties == null || PardusWebViewClient.isSkippedUrl(url)) {
			return;
		}
		// save current page's properties
		savePageProperties();
		// restore next page's zoom level (setInitialScale must be called early)
		PardusPageProperty property = pageProperties.get(url,
				Pardus.orientation);
		if (property != null) {
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Restoring zoom level for " + url + ": "
								+ (int) Math.ceil(property.scale * 100 - 0.5f));
			}
			setInitialScale((int) Math.ceil(property.scale * 100 - 0.5f));
		} else {
			resetInitialScale();
		}
	}

	/**
	 * Sets the page's scroll position.
	 */
	public void propertiesAfterPageLoad() {
		setRenderStatus(RenderStatus.LOAD_FINISH);
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"New render status: LOAD_FINISH");
		}
		touchedAfterPageLoad = false;
		String url = getUrl();
		if (pageProperties == null || PardusWebViewClient.isSkippedUrl(url)) {
			return;
		}
		// restore scroll position
		PardusPageProperty property = pageProperties.get(url,
				Pardus.orientation);
		if (property != null) {
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Restoring scroll position for " + url + ": "
								+ property.posX + "/" + property.posY);
			}
			initialScroll = property;
		} else {
			initialScroll = (url.contains("#")
					|| url.contains("view=getnewpost") || url
					.contains("view=findpost")) ? PardusPageProperties
					.getNoScrollProperty() : PardusPageProperties
					.getEmptyProperty();
		}
		if (initialScroll.posX != -1 || initialScroll.posY != -1) {
			scrollTo(initialScroll.posX, initialScroll.posY);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#scrollTo(int, int)
	 */
	public void scrollTo(int x, int y) {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"WebView#scrollTo called for " + x + "/" + y);
		}
		if (touchedAfterPageLoad || pageProperties == null
				|| (initialScroll.posX == -1 && initialScroll.posY == -1)) {
			super.scrollTo(x, y);
			return;
		}
		if (x != initialScroll.posX || y != initialScroll.posY) {
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Blocking scroll attempt to " + x + "/" + y
								+ " (expected " + initialScroll.posX + "/"
								+ initialScroll.posY + ")");
				// Log.v(this.getClass().getSimpleName(),
				// Log.getStackTraceString(new Throwable()));
			}
			return;
		}
		super.scrollTo(initialScroll.posX, initialScroll.posY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#onScrollChanged(int, int, int, int)
	 */
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Scrolled from " + oldl
					+ "/" + oldt + " to " + l + "/" + t + " (Scale "
					+ (int) Math.ceil(((PardusWebViewClient) getWebViewClient()).getScale() * 100 - 0.5f) + ")");
		}
		super.onScrollChanged(l, t, oldl, oldt);
		if (touchedAfterPageLoad || pageProperties == null
				|| (initialScroll.posX == -1 && initialScroll.posY == -1)) {
			return;
		}
		if (l != initialScroll.posX || t != initialScroll.posY) {
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Scrolling back to initial position "
								+ initialScroll.posX + "/" + initialScroll.posY);
			}
			super.scrollTo(initialScroll.posX, initialScroll.posY);
		}
	}

	/**
	 * Goes back to the previous location.
	 * 
	 * @return false if the browser history is empty
	 */
	public boolean back() {
		WebBackForwardList list = copyBackForwardList();
		int currentPos = list.getCurrentIndex();
		if (currentPos == 0) {
			return false;
		}
		int posBack = 1;
		String previousUrl = list.getItemAtIndex(currentPos - 1).getUrl();
		if (PardusWebViewClient.isSkippedUrl(previousUrl)) {
			if (currentPos >= 2) {
				previousUrl = list.getItemAtIndex(currentPos - 2).getUrl();
				posBack = 2;
			} else {
				clearHistory();
				return false;
			}
		}
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Going back to "
					+ previousUrl);
		}
		if (previousUrl.equals(PardusConstants.loginScreen)) {
			clearHistory();
			login(false);
		} else {
			propertiesBeforePageLoad(previousUrl);
			goBackOrForward(posBack * (-1));
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#loadUrl(java.lang.String)
	 */
	@Override
	public void loadUrl(String url) {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"WebView#loadUrl called for " + url);
		}
		propertiesBeforePageLoad(url);
		super.loadUrl(url);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#reload()
	 */
	@Override
	public void reload() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Reloading page " + getUrl());
		}
		if (pageProperties != null) {
			pageProperties.resetLastUrl();
		}
		propertiesBeforePageLoad(getUrl());
		super.reload();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#computeHorizontalScrollRange()
	 */
	@Override
	public int computeHorizontalScrollRange() {
		return super.computeHorizontalScrollRange();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#computeVerticalScrollRange()
	 */
	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

}
