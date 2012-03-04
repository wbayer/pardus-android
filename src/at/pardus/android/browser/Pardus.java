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

import java.io.File;
import java.util.EmptyStackException;
import java.util.Stack;

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
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
			scriptBrowser = new ScriptBrowser(this, scriptStore,
					PardusConstants.scriptsUrl);
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
							String urlLower = url.toLowerCase();
							if (PardusWebViewClient.isPardusUrl(urlLower)
									&& !urlLower
											.equals(PardusConstants.scriptsUrl)
									&& !urlLower
											.equals(PardusConstants.scriptsUrlHttps)
									&& !urlLower
											.startsWith("http://static.pardus.at/downloads/")) {
								openPardusBrowser();
								return true;
							}
							return super.shouldOverrideUrlLoading(view, url);
						}

						@Override
						public void onPageStarted(WebView view, String url,
								Bitmap favicon) {
							String urlLower = url.toLowerCase();
							if (PardusWebViewClient.isPardusUrl(urlLower)
									&& !urlLower
											.equals(PardusConstants.scriptsUrl)
									&& !urlLower
											.equals(PardusConstants.scriptsUrlHttps)
									&& !urlLower
											.startsWith("http://static.pardus.at/downloads/")) {
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
		parseDisplayMetrics();
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Creating application");
			Log.v(this.getClass().getSimpleName(), "SDK Version "
					+ Build.VERSION.SDK_INT);
			Log.v(this.getClass().getSimpleName(), "Width/Height (dp): "
					+ displayWidthDp + "/" + displayHeightDp
					+ ", Width/Height (px): " + displayWidthPx + "/"
					+ displayHeightPx + ", Scale: " + displayDensityScale);
		}
		PardusPreferences.init(this);
		PardusNotification.init(this);
		CookieSyncManager.createInstance(this);
		// determine available storage directories (prefer external device)
		String storageDir = getExternalPardusDir();
		if (storageDir == null) {
			// no external storage available
			storageDir = getInternalPardusDir();
			if (storageDir == null) {
				Log.e(this.getClass().getSimpleName(),
						"Unable to determine storage directory");
				PardusNotification
						.showLong("No suitable place to store the image pack found");
				finish();
			}
		}
		if (PardusConstants.DEBUG) {
			Log.d(this.getClass().getSimpleName(), "Storage directory set to "
					+ storageDir);
		}
		String cacheDir = getCacheDir().getAbsolutePath();
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
		browser.initClients(progress, messageChecker);
		browser.initDownloadListener(storageDir, cacheDir);
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
			showDialog(R.id.dialog_app_update);
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
		return super.onPrepareOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case R.id.dialog_app_update:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_update_title)
					.setMessage(R.string.app_update_msg)
					.setNeutralButton(R.string.app_update_button,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}

							}).setCancelable(true);
			dialog = builder.create();
			break;
		default:
			dialog = null;
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
							|| thisPlace == prevPlace) {
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
		CookieSyncManager.getInstance().startSync();
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
		CookieSyncManager.getInstance().stopSync();
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
		Log.v(this.getClass().getSimpleName(), "Configuration change");
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
	}

	/**
	 * Gets (or creates if needed) the pardus directory on an external device.
	 * 
	 * @return the location of the pardus directory, or null if no external
	 *         device or if the directory cannot be created
	 */
	private String getExternalPardusDir() {
		String dir = null;
		File externalStorage = Environment.getExternalStorageDirectory();
		if (externalStorage.isDirectory()) {
			// external storage mounted
			if (PardusConstants.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"External storage directory at "
								+ externalStorage.getAbsolutePath());
			}
			dir = externalStorage.getAbsolutePath() + "/pardus/img";
			File externalPardusStorage = new File(dir);
			if (!externalPardusStorage.canRead()
					|| !externalPardusStorage.isDirectory()) {
				if (!externalPardusStorage.mkdirs()) {
					if (PardusConstants.DEBUG) {
						Log.v(this.getClass().getSimpleName(),
								"Cannot create external Pardus storage directory");
					}
					return null;
				}
			}
		}
		return dir;
	}

	/**
	 * Gets (or creates if needed) the internal pardus directory.
	 * 
	 * @return the location of the pardus directory, or null if it cannot be
	 *         created
	 */
	private String getInternalPardusDir() {
		String dir = getFilesDir().getAbsolutePath() + "/img";
		File storage = new File(dir);
		if (!storage.canRead() || !storage.isDirectory()) {
			if (!storage.mkdirs()) {
				return null;
			}
		}
		return dir;
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
