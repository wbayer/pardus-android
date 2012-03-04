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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.DownloadListener;

/**
 * Class handling image pack downloads.
 */
public class PardusDownloadListener implements DownloadListener {

	private PardusWebView browser;

	private Context context;

	private String storageDir;

	private String cacheFile;

	private boolean working = false;

	private ProgressDialog dialog = null;

	private final Handler handler = new Handler() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			String message = msg.getData().getString("message");
			if (message != null) {
				if (dialog != null) {
					// remove old dialog
					dialog.dismiss();
				}
				if (message.equals("")) {
					// move to login page
					browser.login(true);
				} else if (message.equals("error")) {
					// display error
					PardusNotification
							.showLong("Error while getting image pack");
				} else {
					// create new progress dialog
					dialog = new ProgressDialog(context);
					dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					dialog.setCancelable(false);
					dialog.setMessage(message);
					dialog.show();
				}
			} else {
				// set new progress and max values
				int progress = msg.arg1;
				int max = msg.arg2;
				dialog.setProgress(progress);
				if (max != 0) {
					dialog.setMax(max);
				}
			}
		}

	};

	/**
	 * Thread to download and unzip an image pack. Threaded so the UI (progress
	 * bar) can update.
	 */
	private class GetImagePackThread extends Thread {

		private String url;

		private long contentLength;

		/**
		 * Constructor.
		 * 
		 * @param url
		 *            URL of the image pack to download
		 * @param contentLength
		 *            size of the download
		 */
		public GetImagePackThread(String url, long contentLength) {
			super();
			this.url = url;
			this.contentLength = contentLength;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			getImagePack();
		}

		/**
		 * Downloads, unzips and sets the new image pack path.
		 */
		private void getImagePack() {
			if (PardusConstants.DEBUG) {
				Log.d(this.getClass().getSimpleName(), "Downloading");
			}
			if (downloadFile(url, contentLength)) {
				// successfully downloaded
				if (PardusConstants.DEBUG) {
					Log.d(this.getClass().getSimpleName(), "Unzipping");
				}
				if (unzipFile()) {
					// successfully unzipped and moved
					if (PardusConstants.DEBUG) {
						Log.d(this.getClass().getSimpleName(),
								"Storing image pack location");
					}
					PardusPreferences.setImagePath(storageDir);
					// make ui thread switch to login page
					setDialogMessage("");
				} else {
					// unzipping failed
					dialog.dismiss();
					setDialogMessage("error");
				}
			} else {
				// downloading failed
				dialog.dismiss();
				setDialogMessage("error");
			}
			stopWorking();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.DownloadListener#onDownloadStart(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, long)
	 */
	@Override
	public void onDownloadStart(String url, String userAgent,
			String contentDisposition, String mimetype, long contentLength) {
		if (url.startsWith("http://static.pardus.at/downloads/")
				&& url.endsWith(".zip") && startWorking()) {
			new GetImagePackThread(url, contentLength).start();
		}
	}

	/**
	 * Sets a new progress bar message.
	 * 
	 * @param message
	 *            message text
	 */
	private void setDialogMessage(String message) {
		Message msg = handler.obtainMessage();
		Bundle b = new Bundle();
		b.putString("message", message);
		msg.setData(b);
		handler.sendMessage(msg);
	}

	/**
	 * Sets a new progress value.
	 * 
	 * @param progress
	 *            new progress
	 */
	public void setDialogProgress(int progress) {
		Message msg = handler.obtainMessage();
		msg.arg1 = progress;
		msg.arg2 = 0;
		handler.sendMessage(msg);
	}

	/**
	 * Sets a new progress max and resets the progress value to 0.
	 * 
	 * @param max
	 *            new max
	 */
	public void setDialogMax(int max) {
		Message msg = handler.obtainMessage();
		msg.arg1 = 0;
		msg.arg2 = max;
		handler.sendMessage(msg);
	}

	/**
	 * Unzips a file.
	 * 
	 * @return true if successful, false else
	 */
	private boolean unzipFile() {
		setDialogMessage("Unzipping ...");
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(cacheFile);
			setDialogMax(zipFile.size());
			int filesExtracted = 0;
			Enumeration<? extends ZipEntry> zipFiles = zipFile.entries();
			// delete old image pack files
			deleteDir(new File(storageDir));
			new File(storageDir).mkdir();
			// extract new image pack archive
			while (zipFiles.hasMoreElements()) {
				ZipEntry zipEntry = zipFiles.nextElement();
				if (zipEntry.isDirectory()) {
					// directories will be created as needed below
					continue;
				}
				File file = new File(storageDir, zipEntry.getName());
				// create directory path if needed
				if (!file.getParentFile().exists()
						&& !file.getParentFile().mkdirs()) {
					Log.e(this.getClass().getSimpleName(),
							"Unable to create directory");
					return false;
				}
				// extract file
				FileOutputStream fos = null;
				BufferedInputStream bis = null;
				try {
					fos = new FileOutputStream(file);
					bis = new BufferedInputStream(
							zipFile.getInputStream(zipEntry), 10240);
					byte[] buffer = new byte[10240];
					int bytesRead = 0;
					while ((bytesRead = bis.read(buffer, 0, 10240)) != -1) {
						fos.write(buffer, 0, bytesRead);
					}
				} finally {
					try {
						fos.close();
					} catch (Exception e) {
					}
					try {
						bis.close();
					} catch (Exception e) {
					}
				}
				filesExtracted++;
				if (filesExtracted % 50 == 0) {
					setDialogProgress(filesExtracted);
				}
			}
		} catch (IOException e) {
			Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
			return false;
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
			return false;
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
			new File(cacheFile).delete();
		}
		return true;
	}

	/**
	 * Downloads a file and saves it as cacheFile.
	 * 
	 * @param url
	 *            URL to download
	 * @param contentLength
	 *            size of the download
	 * @return true if successful, false else
	 */
	private boolean downloadFile(String url, long contentLength) {
		setDialogMessage("Downloading ...");
		setDialogMax((int) (contentLength / 1024));
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		HttpURLConnection con = null;
		try {
			File f = new File(cacheFile);
			if (f.exists()) {
				f.delete();
			}
			fos = new FileOutputStream(f);
			URL u = new URL(url);
			con = (HttpURLConnection) u.openConnection();
			con.setReadTimeout(5000);
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			con.connect();
			InputStream is = con.getInputStream();
			bis = new BufferedInputStream(is, 10240);
			byte[] buffer = new byte[10240];
			int bytesRead = 0;
			int totalRead = 0;
			int numReads = 0;
			while ((bytesRead = bis.read(buffer, 0, 10240)) != -1) {
				fos.write(buffer, 0, bytesRead);
				totalRead += bytesRead;
				numReads++;
				if (numReads % 10 == 0) {
					setDialogProgress(totalRead / 1024);
				}
			}
		} catch (MalformedURLException e) {
			Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
			return false;
		} catch (IOException e) {
			Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
			return false;
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
			return false;
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
			}
			try {
				bis.close();
			} catch (Exception e) {
			}
			con.disconnect();
		}
		return true;
	}

	/**
	 * Constructor.
	 * 
	 * @param browser
	 *            webview object
	 * @param context
	 *            context the webview is running in
	 * @param storageDir
	 *            final storage directory
	 * @param cacheDir
	 *            temporary download directory
	 */
	public PardusDownloadListener(PardusWebView browser, Context context,
			String storageDir, String cacheDir) {
		this.browser = browser;
		this.context = context;
		this.storageDir = storageDir;
		this.cacheFile = cacheDir + "/img.zip";
	}

	/**
	 * Sets working to true if not already working.
	 * 
	 * @return whether working was set
	 */
	private synchronized boolean startWorking() {
		if (!working) {
			working = true;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Sets working to false.
	 */
	private synchronized void stopWorking() {
		working = false;
	}

	/**
	 * Recursively deletes a directory.
	 * 
	 * @param dir
	 *            directory to delete
	 * @return true if successful, false else
	 */
	private static boolean deleteDir(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			return false;
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				deleteDir(files[i]);
			} else {
				files[i].delete();
			}
		}
		return (dir.delete());
	}

}
