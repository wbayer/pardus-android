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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Locale;
import java.util.Stack;

import at.pardus.android.webview.gm.model.ScriptId;
import at.pardus.android.webview.gm.run.WebViewGm;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;
import at.pardus.android.webview.gm.store.ui.ScriptBrowser;
import at.pardus.android.webview.gm.store.ui.ScriptBrowser.ScriptBrowserWebViewClientGm;
import at.pardus.android.webview.gm.store.ui.ScriptEditor;
import at.pardus.android.webview.gm.store.ui.ScriptList;
import at.pardus.android.webview.gm.store.ui.ScriptManagerActivity;

/**
 * Main activity - application entry point.
 */
public class Pardus extends ScriptManagerActivity {

	public static int displayWidthDp;

	public static int displayHeightDp;

	public static int displayWidthPx;

	public static int displayHeightPx;

	public static float displayDensityScale;

	public static int displayDpi;

	public static int orientation;

	public static boolean isTablet;

	public static boolean hasMenuKey;

	private final Handler handler = new Handler();

	private View browserContainer;

	private PardusWebView browser;

	private ProgressBar progress;

	private PardusLinks links;

	private PardusMessageChecker messageChecker;

	private Stack<Integer> placeHistory = new Stack<Integer>();

	/**
	 * Sets the Pardus browser layout as the app's content.
	 */
	public void openPardusBrowser() {
		if (browserContainer == null) {
			browserContainer = getLayoutInflater().inflate(R.layout.browser,
					null);
		}
		setContentView(browserContainer);
		placeHistory.push(R.id.place_pardus);
	}

	/**
	 * Sets the Script list layout as the app's content.
	 */
	@Override
	public void openScriptList() {
		if (scriptList == null) {
			scriptList = new ScriptList(this, scriptStore);
		}
		setContentView(scriptList.getScriptList());
		placeHistory.push(R.id.place_scriptlist);
	}

	/**
	 * Sets the Script editor layout as the app's content.
	 * 
	 * @param scriptId
	 *            the ID of the script to load
	 */
	@Override
	public void openScriptEditor(ScriptId scriptId) {
		if (scriptEditor == null) {
			scriptEditor = new ScriptEditor(this, scriptStore);
		}
		setContentView(scriptEditor.getEditForm(scriptId));
		placeHistory.push(R.id.place_scripteditor);
	}

	/**
	 * Sets the Script browser layout as the app's content.
	 */
	@Override
	public void openScriptBrowser() {
		if (scriptBrowser == null) {
			scriptBrowser = new ScriptBrowser(
					this,
					scriptStore,
					PardusPreferences.isUseHttps() ? PardusConstants.scriptsUrlHttps
							: PardusConstants.scriptsUrl);
			WebViewGm scriptBrowserWebView = scriptBrowser.getWebView();
			scriptBrowserWebView
					.setWebViewClient(new ScriptBrowserWebViewClientGm(
							scriptStore,
							scriptBrowserWebView.getWebViewClient()
									.getJsBridgeName(),
							scriptBrowserWebView.getWebViewClient().getSecret(),
							scriptBrowser) {
						@Override
						public boolean shouldOverrideUrlLoading(WebView view,
								final String url) {
							String urlLower = url.toLowerCase(Locale.ENGLISH);
							if (PardusWebViewClient.isPardusUrl(urlLower)
									&& !urlLower
											.startsWith(PardusConstants.downloadPageUrl)
									&& !urlLower
											.startsWith(PardusConstants.downloadPageUrlHttps)
									&& !urlLower
											.startsWith(PardusConstants.downloadUrl)) {
								openPardusBrowser();
								return true;
							}
							return super.shouldOverrideUrlLoading(view, url);
						}

						@Override
						public void onPageStarted(WebView view, String url,
								Bitmap favicon) {
							String urlLower = url.toLowerCase(Locale.ENGLISH);
							if (PardusWebViewClient.isPardusUrl(urlLower)
									&& !urlLower
											.startsWith(PardusConstants.downloadPageUrl)
									&& !urlLower
											.startsWith(PardusConstants.downloadPageUrlHttps)
									&& !urlLower
											.startsWith(PardusConstants.downloadUrl)) {
								view.stopLoading();
								openPardusBrowser();
								return;
							}
							super.onPageStarted(view, url, favicon);
						}
					});

		}
		setContentView(scriptBrowser.getBrowser());
		placeHistory.push(R.id.place_scriptbrowser);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isTablet = getResources().getBoolean(R.bool.isTablet);
		if (Build.VERSION.SDK_INT <= 10) {
			hasMenuKey = true;
		} else if (Build.VERSION.SDK_INT <= 13) {
			hasMenuKey = false;
		} else {
			try {
				Method hasPermanentMenuKey = Class.forName(
						"android.view.ViewConfiguration").getMethod(
						"hasPermanentMenuKey", (Class[]) null);
				hasMenuKey = (Boolean) hasPermanentMenuKey.invoke(
						ViewConfiguration.get(this), (Object[]) null);
			} catch (Exception e) {
				Log.w(this.getClass().getSimpleName(),
						"Exception while attempting to call hasPermanentMenuKey via reflection (API level "
								+ Build.VERSION.SDK_INT + "): " + e);
				hasMenuKey = false;
			}
		}
		if (!isTablet && hasMenuKey) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		PardusPreferences.init(this, null);
		PardusNotification.init(this);
		if (PardusPreferences.isFullScreen()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		parseDisplayMetrics();
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Creating application");
			Log.v(this.getClass().getSimpleName(), "SDK Version "
					+ Build.VERSION.SDK_INT);
			Log.v(this.getClass().getSimpleName(), "Width/Height (dp): "
					+ displayWidthDp + "/" + displayHeightDp
					+ ", Width/Height (px): " + displayWidthPx + "/"
					+ displayHeightPx + ", Scale: " + displayDensityScale
					+ ", Density (dpi): " + displayDpi);
		}
		PardusImagePack imagePack = new PardusImagePack(Environment
				.getExternalStorageDirectory().getAbsolutePath(), getFilesDir()
				.getAbsolutePath());
		if (imagePack.getPath() == null) {
			Log.e(this.getClass().getSimpleName(),
					"Unable to determine storage directory");
			PardusNotification
					.showLong("No suitable place to store the image pack found");
			finish();
		} else {
			if (PardusConstants.DEBUG) {
				Log.d(PardusImagePack.class.getClass().getSimpleName(),
						"Pardus image pack directory set to "
								+ imagePack.getPath());
			}
		}
		// load the script store
		scriptStore = new ScriptStoreSQLite(this);
		scriptStore.open();
		// attach layout to screen
		openPardusBrowser();
		// initialize progress bar
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setMax(100);
		progress.setIndeterminate(false);
		// initialize message checker
		messageChecker = new PardusMessageChecker(handler,
				(TextView) findViewById(R.id.notify), 60000);
		// initialize browser and links
		browser = (PardusWebView) findViewById(R.id.browser);
		browser.setScriptStore(scriptStore);
		browser.initClients(this, progress, messageChecker);
		browser.initJavascriptBridges();
		browser.initDownloadListener(imagePack.getPath(), getCacheDir()
				.getAbsolutePath());
		GridView linksGridView = (GridView) findViewById(R.id.links);
		links = new PardusLinks(this, handler, getLayoutInflater(), browser,
				linksGridView);
		browser.initLinks(links);
		registerForContextMenu(browser);
		// check if a new version was installed
		int versionLastStart = PardusPreferences.getVersionCode();
		int currentVersion = getVersionCode();
		if (versionLastStart == -1) {
			PardusPreferences.setVersionCode(currentVersion);
		} else if (versionLastStart < currentVersion) {
			if (versionLastStart < getResources().getInteger(
					R.integer.last_major_update)) {
				if (getString(R.string.app_update_msg).length() > 0) {
					showDialog(R.id.dialog_app_update, null);
				}
			} else {
				if (getString(R.string.app_update_msg_minor).length() > 0) {
					showDialog(R.id.dialog_app_update_minor, null);
				}
			}
			PardusPreferences.setVersionCode(currentVersion);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean linksVisible = links.isVisible();
		if (linksVisible) {
			links.hide();
		}
		int itemId = item.getItemId();
		if (currentPlace() != R.id.place_pardus
				&& itemId != R.id.option_scripts_get
				&& itemId != R.id.option_scripts_manage
				&& itemId != R.id.option_scripts_submenu
				&& itemId != R.id.option_userscripts) {
			openPardusBrowser();
		}
		switch (itemId) {
		case R.id.option_showlinks:
			if (!linksVisible) {
				links.show();
			}
			return true;
		case R.id.option_refresh:
			browser.reload();
			return true;
		case R.id.option_userscripts:
			openScriptBrowser();
			scriptBrowser.loadUrl(PardusConstants.userscriptUrl);
			return true;
		case R.id.option_scripts_get:
			openScriptBrowser();
			return true;
		case R.id.option_scripts_manage:
			openScriptList();
			return true;
		case R.id.option_settings:
			browser.showSettings();
			return true;
		case R.id.option_login:
			browser.login(false);
			return true;
		case R.id.option_logout:
			finish();
			return true;
		case R.id.option_artemis:
			browser.switchUniverse("Artemis");
			return true;
		case R.id.option_orion:
			browser.switchUniverse("Orion");
			return true;
		case R.id.option_pegasus:
			browser.switchUniverse("Pegasus");
			return true;
		case R.id.option_account:
			browser.loadUrl((PardusPreferences.isUseHttps()) ? PardusConstants.loggedInUrlHttps
					: PardusConstants.loggedInUrl);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.removeGroup(R.id.option_group_scripts);
		menu.removeGroup(R.id.option_group_pardus);
		// removing the groups leaves items that are in the submenu
		menu.removeItem(R.id.option_showlinks);
		menu.removeItem(R.id.option_refresh);
		menu.removeItem(R.id.option_artemis);
		menu.removeItem(R.id.option_orion);
		menu.removeItem(R.id.option_pegasus);
		menu.removeItem(R.id.option_account);
		menu.removeItem(R.id.option_userscripts);
		menu.removeItem(R.id.option_pardus);
		if (currentPlace() == R.id.place_pardus) {
			menu.add(R.id.option_group_pardus, R.id.option_showlinks, 0,
					R.string.option_showlinks);
			menu.add(R.id.option_group_pardus, R.id.option_refresh, 0,
					R.string.option_refresh);
			String universe = browser.getUniverse();
			String playedUniverses = PardusPreferences.getPlayedUniverses();
			if (universe != null && !playedUniverses.equals("")) {
				String switchStr = getString(R.string.option_switch);
				if (!universe.equals("artemis")
						&& playedUniverses.contains("artemis")) {
					menu.add(R.id.option_group_pardus, R.id.option_artemis, 2,
							switchStr + " Artemis");
				}
				if (!universe.equals("orion")
						&& playedUniverses.contains("orion")) {
					menu.add(R.id.option_group_pardus, R.id.option_orion, 2,
							switchStr + " Orion");
				}
				if (!universe.equals("pegasus")
						&& playedUniverses.contains("pegasus")) {
					menu.add(R.id.option_group_pardus, R.id.option_pegasus, 2,
							switchStr + " Pegasus");
				}
			}
			if (browser.isLoggedIn()) {
				menu.add(R.id.option_group_pardus, R.id.option_account, 2,
						R.string.option_account);
			}
		} else {
			menu.add(R.id.option_group_scripts, R.id.option_userscripts, 0,
					R.string.option_userscripts);
			menu.add(R.id.option_group_scripts, R.id.option_pardus, 0,
					R.string.option_pardus);
		}
		if (isTablet || !hasMenuKey) {
			// declare action bar items on tablets and devices without menu key
			try {
				Method showAsAction = Class
						.forName("android.view.MenuItem")
						.getMethod("setShowAsAction", new Class[] { int.class });
				int[] actionItemIds = { R.id.option_showlinks,
						R.id.option_orion, R.id.option_artemis,
						R.id.option_pegasus, R.id.option_logout };
				for (int itemId : actionItemIds) {
					MenuItem item = menu.findItem(itemId);
					if (item != null) {
						int showProperty = -1;
						if (itemId == R.id.option_showlinks
								|| itemId == R.id.option_logout) {
							showProperty = 2;
						} else {
							if (Pardus.displayWidthDp > 400
									&& Pardus.displayHeightDp > 400) {
								showProperty = 1;
							}
						}
						if (showProperty != -1) {
							showAsAction.invoke(item,
									new Object[] { showProperty });
						}
					}
				}
			} catch (Exception e) {
				Log.i(this.getClass().getSimpleName(),
						"Running a tablet or device without menu button without action bar support. "
								+ e);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		Dialog dialog = null;
		if (id == R.id.dialog_app_update || id == R.id.dialog_app_update_minor) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_update_title)
					.setMessage(
							(id == R.id.dialog_app_update) ? R.string.app_update_msg
									: R.string.app_update_msg_minor)
					.setNeutralButton(R.string.app_update_button,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}

							}).setCancelable(true);
			dialog = builder.create();
		} else if (id == R.id.dialog_ip_update) {
			final String updateUrl = args.getString("updateUrl");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.ip_update_title)
					.setMessage(R.string.ip_update_msg)
					.setNegativeButton(R.string.ip_update_button_neg,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									PardusPreferences
											.setNextImagePackUpdateCheck(new Date(
													new Date().getTime() + 86400000 * 7));
									dialog.dismiss();
								}

							})
					.setPositiveButton(R.string.ip_update_button_pos,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
									browser.loadUrl(updateUrl);
								}

							}).setCancelable(true);
			dialog = builder.create();
		}
		return dialog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		try {
			Integer thisPlace = placeHistory.pop();
			if (R.id.place_pardus == thisPlace && browser.back()) {
				placeHistory.push(thisPlace);
			} else if (R.id.place_scriptbrowser == thisPlace
					&& scriptBrowser.back()) {
				placeHistory.push(thisPlace);
			} else {
				while (true) {
					Integer prevPlace = placeHistory.pop();
					if (R.id.place_scripteditor == prevPlace
							|| thisPlace.equals(prevPlace)) {
						continue;
					}
					if (R.id.place_pardus == prevPlace) {
						openPardusBrowser();
						return;
					}
					if (R.id.place_scriptlist == prevPlace) {
						openScriptList();
						return;
					}
					if (R.id.place_scriptbrowser == prevPlace) {
						openScriptBrowser();
						return;
					}
				}
			}
		} catch (EmptyStackException e) {
			super.onBackPressed();
		}
	}

	/**
	 * @return the currently displayed place
	 */
	private int currentPlace() {
		try {
			return placeHistory.peek();
		} catch (EmptyStackException e) {
			return -1;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Starting (or restarting) application");
		}
		super.onStart();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Resuming (or starting) application");
		}
		if (scriptStore != null) {
			scriptStore.open();
		}
		// wake up the browser
		try {
			Class.forName("android.webkit.WebView")
					.getMethod("onResume", (Class[]) null)
					.invoke(browser, (Object[]) null);
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Cannot wake up browser threads: "
							+ Log.getStackTraceString(e));
		}
		browser.resumeTimers();
		if (!browser.isLoggedIn()) {
			// open the login page if not logged in
			browser.login(true);
		}
		messageChecker.resume();
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Pausing application (to be resumed or stopped or killed)");
		}
		browser.stopLoading();
		if (isFinishing() || PardusPreferences.isLogoutOnHide()) {
			browser.logout();
		}
		progress.setVisibility(View.GONE);
		// keep the browser from working in the background
		browser.pauseTimers();
		try {
			Class.forName("android.webkit.WebView")
					.getMethod("onPause", (Class[]) null)
					.invoke(browser, (Object[]) null);
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Cannot pause browser threads: "
							+ Log.getStackTraceString(e));
		}
		messageChecker.pause();
		if (scriptEditor != null
				&& placeHistory.peek() != R.id.place_scripteditor) {
			scriptEditor = null;
		}
		if (scriptStore != null) {
			scriptStore.close();
		}
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Stopping application (to be destroyed or restarted)");
		}
		// this method may not be reached pre-honeycomb
		// used for time-consuming low-priority tasks
		PardusPageProperties pageProperties = browser.getPageProperties();
		if (pageProperties != null) {
			pageProperties.persist();
		}
		super.onStop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.Activity#onConfigurationChanged(android.content.res.Configuration
	 * )
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (PardusConstants.DEBUG) {
			Log.i(this.getClass().getSimpleName(), "Configuration change");
		}
		super.onConfigurationChanged(newConfig);
		parseDisplayMetrics();
		links.calcAndApplyDimensions();
	}

	/**
	 * Updates static variables regarding display configuration.
	 */
	private void parseDisplayMetrics() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		displayWidthPx = displayMetrics.widthPixels;
		displayHeightPx = displayMetrics.heightPixels;
		displayWidthDp = (int) Math.ceil(displayMetrics.widthPixels
				/ displayMetrics.density);
		displayHeightDp = (int) Math.ceil(displayMetrics.heightPixels
				/ displayMetrics.density);
		displayDensityScale = displayMetrics.density;
		displayDpi = displayMetrics.densityDpi;
		orientation = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || displayWidthPx > displayHeightPx) ? Configuration.ORIENTATION_LANDSCAPE
				: Configuration.ORIENTATION_PORTRAIT;
	}

	/**
	 * @return the app's version code
	 */
	private int getVersionCode() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			return -1;
		}
	}

}
