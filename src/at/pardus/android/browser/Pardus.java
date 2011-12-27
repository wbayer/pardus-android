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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Main activity - application entry point.
 */
public class Pardus extends Activity {

	private static final int DIALOG_APP_UPDATE_ID = 0;

	public static int displayWidthDp;

	public static int displayHeightDp;

	public static int displayWidthPx;

	public static int displayHeightPx;

	public static float displayDensityScale;

	private final Handler handler = new Handler();

	private PardusWebView browser;

	private ProgressBar progress;

	private PardusLinks links;

	private PardusMessageChecker messageChecker;

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
		// attach layout to screen
		setContentView(R.layout.browser);
		// initialize progress bar
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setMax(100);
		progress.setIndeterminate(false);
		// initialize message checker
		messageChecker = new PardusMessageChecker(handler,
				(TextView) findViewById(R.id.notify), 60000);
		// initialize browser and links
		browser = (PardusWebView) findViewById(R.id.browser);
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
		if (/* versionLastStart != -1 && */versionLastStart < currentVersion) {
			showDialog(DIALOG_APP_UPDATE_ID);
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
		if (item.getGroupId() == R.id.menu_group_unis) {
			switch (item.getItemId()) {
			case R.id.menu_artemis:
				browser.switchUniverse("Artemis");
				return true;
			case R.id.menu_orion:
				browser.switchUniverse("Orion");
				return true;
			case R.id.menu_pegasus:
				browser.switchUniverse("Pegasus");
				return true;
			case R.id.menu_account:
				browser.loadUrl((PardusPreferences.isUseHttps()) ? PardusConstants.loggedInUrlHttps
						: PardusConstants.loggedInUrl);
				return true;
			default:
				return super.onOptionsItemSelected(item);
			}
		}
		switch (item.getItemId()) {
		case R.id.option_showlinks:
			if (!linksVisible) {
				links.show();
			}
			return true;
		case R.id.option_refresh:
			browser.reload();
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
		menu.removeGroup(R.id.menu_group_unis);
		String universe = browser.getUniverse();
		String playedUniverses = PardusPreferences.getPlayedUniverses();
		if (universe != null && !playedUniverses.equals("")) {
			String switchStr = getString(R.string.option_switch);
			if (!universe.equals("artemis")
					&& playedUniverses.contains("artemis")) {
				menu.add(R.id.menu_group_unis, R.id.menu_artemis, 1, switchStr
						+ " Artemis");
			}
			if (!universe.equals("orion") && playedUniverses.contains("orion")) {
				menu.add(R.id.menu_group_unis, R.id.menu_orion, 2, switchStr
						+ " Orion");
			}
			if (!universe.equals("pegasus")
					&& playedUniverses.contains("pegasus")) {
				menu.add(R.id.menu_group_unis, R.id.menu_pegasus, 3, switchStr
						+ " Pegasus");
			}
		}
		if (browser.isLoggedIn()) {
			menu.add(R.id.menu_group_unis, R.id.menu_account, 4,
					getString(R.string.option_account));
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
		case DIALOG_APP_UPDATE_ID:
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
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && browser.canGoBack()) {
			browser.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
