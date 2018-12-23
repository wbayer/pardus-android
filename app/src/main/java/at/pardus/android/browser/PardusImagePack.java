/*
 *    Copyright 2013 Werner Bayer
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
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import at.pardus.android.webview.gm.util.UnicodeReader;

/**
 * Encapsulates properties of an image pack. Handles path finding and updates.
 */
public class PardusImagePack {

	private String path;

	/**
	 * Constructor with an already determined path to the Pardus image pack.
	 * 
	 * @param path
	 *            the path to the Pardus image pack directory
	 */
	public PardusImagePack(String path) {
		this.path = path;
	}

	/**
	 * Constructor with the image pack path to be determined.
	 * 
	 * @param externalDir
	 *            may be either the external root directory of the device or the app
	 * @param internalDir
	 *            the internal directory of the app
	 */
	public PardusImagePack(File externalDir, File internalDir) {
		path = determinePath(externalDir, internalDir);
	}

	/**
	 * @return true if the image pack is installed, false else
	 */
	public boolean isInstalled() {
		return PardusImagePack.isInstalled(path);
	}

	/**
	 * Starts a separate thread to check for an image pack update at srcUrl (if
	 * set) and prompts for its download.
	 * 
	 * @param activity
	 *            the activity handling dialogs
	 */
	public void updateCheck(final Activity activity) {
		if (path == null) {
			return;
		}
		new Thread() {
			public void run() {
				final String srcUrl = getSrcUrl();
				long lastUpdate = getLastUpdate();
				if (srcUrl == null || !IMAGEPACKUPDATES.containsKey(srcUrl)
						|| lastUpdate == -1) {
					if (BuildConfig.DEBUG) {
						Log.d("PardusImagePack",
								"Could not retrieve image pack info - no update reminders");
					}
					return;
				}
				long lastSrcUpdate = -1;
                HttpURLConnection con;
                try {
                    URL u = new URL(srcUrl + "/nfo_upd");
                    con = (HttpURLConnection) u.openConnection();
                    con.setUseCaches(false);
                    con.setRequestMethod("GET");
                    con.setReadTimeout(5000);
                } catch (IOException e) {
                    Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
                    return;
                }
                try (Reader in = new UnicodeReader(con.getInputStream(), con.getContentEncoding())) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
                        if (bytesRead > 0) {
                            sb.append(buffer, 0, bytesRead);
                        }
                    }
                    lastSrcUpdate = Long.valueOf(sb.toString());
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        Log.w(this.getClass().getSimpleName(), Log.getStackTraceString(e));
                    }
                }
				if (lastSrcUpdate > lastUpdate) {
					if (BuildConfig.DEBUG) {
						Log.v("PardusImagePack",
								"Image pack update available (local v"
										+ lastUpdate + ", remote v"
										+ lastSrcUpdate + ")");
					}
					activity.runOnUiThread(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("updateUrl",
                                IMAGEPACKUPDATES.get(srcUrl));
                        activity.showDialog(R.id.dialog_ip_update, bundle);
                    });
				} else {
					if (BuildConfig.DEBUG) {
						Log.v("PardusImagePack", "Image pack up to date (v"
								+ lastUpdate + ")");
					}
				}
			}
		}.start();
	}

	/**
	 * @return the URL this image pack is originally hosted at or null if that
	 *         information is not available
	 */
	public String getSrcUrl() {
        try (Scanner in = new Scanner(new FileReader(path + "/nfo_src")).useDelimiter("[\\r\\n]+")) {
            return in.next();
        } catch (Exception e) {
            return null;
        }
	}

	/**
	 * @return the date of the last update of the installed image pack
	 *         (yyyymmdd[0-9][0-9]) or -1 if that information is not available
	 */
	public long getLastUpdate() {
        try (Scanner in = new Scanner(new FileReader(path + "/nfo_upd")).useDelimiter("[\\r\\n]+")) {
            return in.nextLong();
        } catch (Exception e) {
            return -1;
        }
	}

	/**
	 * @return the absolute path to the Pardus image pack directory or null if
	 *         it could not be determined
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Checks if an image pack is installed.
	 * 
	 * Creates a .nomedia file to hide from Android's gallery app.
	 * 
	 * @param imagePath
	 *            the directory to look for image pack files in
	 * @return true if found, false else
	 */
	private static boolean isInstalled(String imagePath) {
		if (imagePath == null || imagePath.equals("")) {
			return false;
		}
		boolean installed = false;
		File galleryHideFile = new File(imagePath + "/.nomedia");
		if (galleryHideFile.exists() && galleryHideFile.canRead()) {
			// .nomedia file exists - expect image pack to be there
			installed = true;
		} else {
			// .nomedia file does not exist
			File imagePathCheckFile = new File(imagePath + "/vip.png");
			if (imagePathCheckFile.exists() && imagePathCheckFile.canRead()) {
				installed = true;
				// image pack exists -> create .nomedia file
                boolean nomediaFileCreated = false;
				try {
                    nomediaFileCreated = galleryHideFile.createNewFile();
				} catch (IOException ignored) {
				}
                if (BuildConfig.DEBUG) {
                    if (!nomediaFileCreated) {
                        Log.w(PardusImagePack.class.getSimpleName(), "Error creating new file " +
                                galleryHideFile.getAbsolutePath());

                    }
                }
			}
		}
		return installed;
	}

    /**
     * Determines the path to store the Pardus image pack files in.
     *
     * @param externalDir may be either the external root directory of the device or the app
     * @param internalDir the internal directory of the app
     * @return the absolute path to the Pardus image pack directory or null if
     * it could not be determined
     */
    private static String determinePath(File externalDir, File internalDir) {
        // determine available storage directories (prefer external device)
        String path = getPardusDir(externalDir, "/pardus/img");
        if (path == null) {
            // no external storage available
            if (BuildConfig.DEBUG) {
                Log.d(PardusImagePack.class.getSimpleName(),
                        "Using internal storage space for the image pack");
            }
            path = getPardusDir(internalDir, "/img");
        }
        return path;
    }

    /**
     * Gets (or creates if needed) the pardus directory within the specified path.
     *
     * @param rootPath     the root to look for the pardus directory in
     * @param pardusSubDir the subdirectory for pardus
     * @return the location of the pardus directory, or null if the directory cannot be created
     */
    private static String getPardusDir(File rootPath, String pardusSubDir) {
        if (rootPath == null || !rootPath.canRead() || !rootPath.isDirectory()) {
            return null;
        }
        String pardusDir = rootPath.getAbsolutePath() + pardusSubDir;
        File pardusStorage = new File(pardusDir);
        if (!pardusStorage.canRead()
                || !pardusStorage.isDirectory()) {
            if (!pardusStorage.mkdirs()) {
                if (BuildConfig.DEBUG) {
                    Log.d(PardusImagePack.class.getSimpleName(),
                            "Cannot create Pardus storage directory at " + pardusDir);
                }
                return null;
            }
        }
        return pardusDir;
    }

	private static final Map<String, String> IMAGEPACKUPDATES;

	static {
		IMAGEPACKUPDATES = new HashMap<>();
		IMAGEPACKUPDATES
				.put("https://static.pardus.at/img/std",
						"https://static.pardus.at/downloads/update_images_standard64.zip");
		IMAGEPACKUPDATES
				.put("https://static.pardus.at/img/stdhq",
						"https://static.pardus.at/downloads/update_images_standardhq64.zip");
		IMAGEPACKUPDATES
				.put("https://static.pardus.at/img/xolarix",
						"https://static.pardus.at/downloads/update_images_xolarix64.zip");
		IMAGEPACKUPDATES.put("https://static.pardus.at/images",
				"https://static.pardus.at/downloads/update_images.zip");
		IMAGEPACKUPDATES.put("https://static.pardus.at/img/kora",
				"https://static.pardus.at/downloads/update_images_kora.zip");

	}

}
