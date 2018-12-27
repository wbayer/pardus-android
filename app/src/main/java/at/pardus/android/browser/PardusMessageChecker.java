/*
 *    Copyright 2011 Werner Bayer
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

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to periodically send HTTP GET requests to the currently logged in
 * Pardus universe server to check for new messages and logs.
 */
public class PardusMessageChecker {

	private static final int TIMEOUT_MILLIS = 5000;

	private static final Pattern patternStatus = Pattern
			.compile("px;'> (.*)</font></td></tr></table>");

	private static final Pattern patternPm = Pattern
			.compile("<span id=\"new_msg\">(\\d+)</span>");

	private static final Pattern patternAm = Pattern
			.compile("<span id=\"new_amsg\">(\\d+)</span>");

	private static final Pattern patternTrade = Pattern
			.compile("<span id=\"new_tl\">(\\d+)</span>");

	private static final Pattern patternMission = Pattern
			.compile("<span id=\"new_ml\">(\\d+)</span>");

	private static final Pattern patternPay = Pattern
			.compile("<span id=\"new_pl\">(\\d+)</span>");

	private static final Pattern patternCombat = Pattern
			.compile("<span id=\"new_cl\">(\\d+)</span>");

	private static final Pattern patternMo = Pattern
			.compile("<span id=\"new_mo\">(\\d+)</span>");

	private final TextView notifyView;

	private final int delayMillis;

	private String universe;
	private URL url;
	private String cookies;

	private final Timer timer = new Timer();
	private TimerTask task;

	private final Handler handler;

	/**
	 * Constructor.
	 * 
	 * @param handler
	 *            the handler created by the main/UI thread
	 * @param notifyView
	 *            the view to use to notify the user
	 * @param delayMillis
	 *            the period between checks in milli-seconds
	 */
	public PardusMessageChecker(Handler handler, TextView notifyView,
			int delayMillis) {
		this.handler = handler;
		this.notifyView = notifyView;
		this.delayMillis = delayMillis;
	}

	/**
	 * Tries to get the msgFrame via an HTTP GET request, parses it for new
	 * messages/logs and displays the result.
	 * 
	 * Does not need to and should not be run on the UI thread.
	 */
	public void check() {
		if (universe == null) {
			return;
		}
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Checking for new messages/logs");
		}
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Cookie", cookies);
            urlConnection.setReadTimeout(TIMEOUT_MILLIS);
            urlConnection.setConnectTimeout(TIMEOUT_MILLIS);
            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(this.getClass().getSimpleName(),
                        "Could not check for messages, response code " + urlConnection.getResponseCode());
                return;
            }
        } catch (IOException e) {
            Log.w(this.getClass().getSimpleName(), "Could not check for messages", e);
            return;
        }
        try (InputStream in = new BufferedInputStream(urlConnection.getInputStream()); InputStreamReader
                reader = new InputStreamReader(in)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int i;
            while ((i = reader.read(buffer, 0, buffer.length)) >= 0) {
                if (i > 0) {
                    sb.append(buffer, 0, i);
                }
            }
            String responseStr = sb.toString();
            parseResponse(responseStr);
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(), "Could not check for messages", e);
        }
	}

	/**
	 * Helper method to parse the msgFrame for new messages/logs and display the
	 * result.
	 * 
	 * @param response
	 *            the content of the msgFrame
	 */
	private void parseResponse(String response) {
		Matcher matcher;
		String text = "";
		matcher = patternStatus.matcher(response);
		if (matcher.find()) {
			text += matcher.group(1).replaceAll("<.*?>", "") + "\n";
		}
		matcher = patternPm.matcher(response);
		if (matcher.find()) {
			text += "PM:" + matcher.group(1) + " ";
		}
		matcher = patternAm.matcher(response);
		if (matcher.find()) {
			text += "AM:" + matcher.group(1) + " ";
		}
		matcher = patternTrade.matcher(response);
		if (matcher.find()) {
			text += "Trade:" + matcher.group(1) + " ";
		}
		matcher = patternMission.matcher(response);
		if (matcher.find()) {
			text += "Mission:" + matcher.group(1) + " ";
		}
		matcher = patternPay.matcher(response);
		if (matcher.find()) {
			text += "Pay:" + matcher.group(1) + " ";
		}
		matcher = patternCombat.matcher(response);
		if (matcher.find()) {
			text += "Combat:" + matcher.group(1) + " ";
		}
		matcher = patternMo.matcher(response);
		if (matcher.find()) {
			text += "MO:" + matcher.group(1) + " ";
		}
		NotifyRunnable notify = new NotifyRunnable(text);
		handler.post(notify);
	}

	/**
	 * Pauses the background message checking.
	 */
	public void pause() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Pausing Pardus Message Checker");
		}
		task.cancel();
	}

	/**
	 * Resumes the background message checking.
	 */
	public void resume() {
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Resuming Pardus Message Checker");
		}
		final String startingUniverse = universe;
		task = new TimerTask() {

			@Override
			public void run() {
			    if (startingUniverse != null && startingUniverse.equals(universe)) {
                    check();
                }
			}

		};
		timer.schedule(task, 1500, delayMillis);
	}

	/**
	 * Restarts the background message checking task and immediately checks for
	 * new messages.
	 */
	public void restart() {
		pause();
		resume();
	}

	/**
	 * Sets the currently logged in universe along with the required
	 * authentication cookies. Immediately checks for new messages/logs
	 * afterwards.
	 * 
	 * @param universe
	 *            the universe to check messages for
	 * @param cookies
	 *            the cookies to authenticate
	 */
	public void setUniverse(String universe, String cookies) {
		if ((universe == null && this.universe == null) || (universe != null && universe.equals(this
                .universe))) {
			return;
		}
		this.universe = universe;
        this.cookies = cookies;
		if (universe == null) {
			if (BuildConfig.DEBUG) {
				Log.v(this.getClass().getSimpleName(),
						"Message checker assumes the user logged out");
			}
			NotifyRunnable notify = new NotifyRunnable("");
			handler.post(notify);
			return;
		}
        try {
            url = new URL(PardusConstants.getUniverseUrl(universe, PardusConstants.msgFrame));
        } catch (MalformedURLException e) {
            Log.e(this.getClass().getSimpleName(), "Error parsing URL " + url);
            this.universe = null;
            return;
        }
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Message checker will work with URL " + url);
		}
		restart();
	}

	/**
	 * Inner class used to be sent to a handler on the UI thread to update the
	 * new messages/logs notification text.
	 */
	private class NotifyRunnable implements Runnable {

		private String text;

		/**
		 * Constructor.
		 * 
		 * @param text
		 *            new text to set
		 */
		public NotifyRunnable(String text) {
			this.text = text;
		}

		@Override
		public void run() {
			if (BuildConfig.DEBUG) {
				if (!text.equals("")) {
					Log.v(this.getClass().getSimpleName(),
							"Setting notify text to " + text);
				}
			}
			if (universe != null || text.equals("")) {
				notifyView.setText(text);
			}
		}
	}

}
