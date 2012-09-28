/*
 *    Copyright 2012 Werner Bayer
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

import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;

/**
 * Class managing properties for each visited page (zoom level, scroll position,
 * screen orientation). Persists all data to disk.
 */
public class PardusPageProperties {

	private static final String FILENAME = "pageproperties.ser";

	private Map<String, PardusPageProperty> history = new HashMap<String, PardusPageProperty>();

	private String lastUrl = null;

	private Context context;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            the app's context
	 */
	public PardusPageProperties(Context context) {
		this.context = context;
		loadFromDisk();
	}

	/**
	 * Saves a page's zoom level and scroll position.
	 * 
	 * @param url
	 *            the URL of the page
	 * @param scale
	 *            the zoom level
	 * @param scrollXRel
	 *            the horizontal scroll position relative to the content's width
	 * @param scrollYRel
	 *            the vertical scroll position relative to the content's height
	 * @param landscape
	 *            the screen's orientation, true if in landscape mode
	 */
	public void save(String url, float scale, float scrollXRel,
			float scrollYRel, boolean landscape) {
		if (url == null || url.equals(lastUrl)) {
			return;
		}
		lastUrl = url;
		String trimmedUrl = trimUrl(url);
		if (trimmedUrl == null) {
			return;
		}
		boolean noScroll = trimmedUrl.startsWith("FORUM/") && url.contains("#");
		PardusPageProperty property = new PardusPageProperty(scale,
				noScroll ? 0.0f : scrollXRel, noScroll ? 0.0f : scrollYRel,
				landscape);
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Saving properties for "
					+ trimmedUrl + " (" + url + "): " + property);
		}
		history.put(trimmedUrl, property);
	}

	/**
	 * Looks up properties for a page.
	 * 
	 * @param url
	 *            the URL of the page
	 * @return the PardusPageProperty object or null if none available
	 */
	public PardusPageProperty get(String url) {
		return history.get(trimUrl(url));
	}

	/**
	 * Persists all entries to the disk.
	 */
	public void persist() {
		if (PardusConstants.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Persisting " + history.size() + " page properties ...");
		}
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(context.openFileOutput(FILENAME,
					Context.MODE_PRIVATE));
			os.writeObject(history);
			os.close();
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Error persisting page properties. "
							+ Log.getStackTraceString(e));
			try {
				os.close();
			} catch (Exception e2) {
			}
		}
	}

	/**
	 * Loads all entries from the disk.
	 */
	@SuppressWarnings("unchecked")
	public void loadFromDisk() {
		if (PardusConstants.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Loading page properties from disk ...");
		}
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(context.openFileInput(FILENAME));
			history = (Map<String, PardusPageProperty>) is.readObject();
			is.close();
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Error loading page properties from disk. "
							+ Log.getStackTraceString(e));
			try {
				is.close();
			} catch (Exception e2) {
			}
		}
		if (PardusConstants.DEBUG) {
			Log.d(this.getClass().getSimpleName(), "Loaded " + history.size()
					+ " page properties");
		}
	}

	/**
	 * Wipe all saved properties persistently.
	 */
	public void forget() {
		history = new HashMap<String, PardusPageProperty>();
		persist();
	}

	/**
	 * Removes/replaces parts of a URL in order to shorten it and have certain
	 * pages share one property (i.e. no protocol dependency, no
	 * universe-specific domain, no query parameters in many cases).
	 * 
	 * @param url
	 *            the URL to work on
	 * @return the trimmed URL
	 */
	private String trimUrl(String url) {
		if (url == null) {
			return null;
		}
		int pos = url.indexOf("://");
		if (pos == -1 || pos > 5) {
			return null;
		}
		url = url.substring(pos + 3);
		boolean stripQueryParams = false;
		if (url.startsWith("artemis.pardus.at/")
				|| url.startsWith("orion.pardus.at/")
				|| url.startsWith("pegasus.pardus.at/")) {
			url = "GAME/" + url.substring(url.indexOf(".pardus.at/") + 11);
			if (url.startsWith("GAME/main.php")
					|| url.startsWith("GAME/overview_")
					|| url.startsWith("GAME/messages_")
					|| url.startsWith("GAME/news.php")
					|| url.startsWith("GAME/ship_equipment.php")
					|| url.startsWith("GAME/bounties.php")) {
				stripQueryParams = true;
			}
		} else if (url.startsWith("chat.pardus.at/")) {
			url = "CHAT/" + url.substring(url.indexOf(".pardus.at/") + 11);
			stripQueryParams = true;
		} else if (url.startsWith("forum.pardus.at/")) {
			String forumSection = "INDEX";
			if (url.contains("showtopic=") || url.contains("act=ST")
					|| url.contains("view=findpost")) {
				forumSection = "IN_THREAD";
			} else if (url.contains("showforum=") || url.contains("act=SF")) {
				forumSection = "IN_FORUM";
			} else if (url.contains("act=Post")) {
				forumSection = "POST";
			} else if (url.contains("searchid=")) {
				forumSection = "SEARCH_RESULT";
			} else if (url.contains("act=Search")) {
				forumSection = "SEARCH";
			}
			url = "FORUM/" + forumSection;
		} else if (url.startsWith("www.pardus.at/")) {
			url = "PORTAL/" + url.substring(url.indexOf(".pardus.at/") + 11);
		}
		if (url.contains("page=")) {
			stripQueryParams = true;
		}
		if (stripQueryParams) {
			pos = url.indexOf("?");
			if (pos != -1) {
				url = url.substring(0, pos);
			}
		}
		return url;
	}

	/**
	 * Immutable object containing property values of a page.
	 */
	public static class PardusPageProperty implements Serializable {

		private static final long serialVersionUID = 1L;

		public final float scale;

		public final float scrollXRel, scrollYRel;

		public final boolean landscape;

		/**
		 * Constructor.
		 * 
		 * @param scale
		 *            the zoom level
		 * @param scrollXRel
		 *            the horizontal scroll position
		 * @param scrollYRel
		 *            the vertical scroll position
		 * @param landscape
		 *            the screen's orientation, true if in landscape mode
		 */
		private PardusPageProperty(float scale, float scrollXRel,
				float scrollYRel, boolean landscape) {
			this.scale = scale;
			this.scrollXRel = scrollXRel;
			this.scrollYRel = scrollYRel;
			this.landscape = landscape;
		}

		/**
		 * @return Javascript scrollTo method to scroll to the saved values
		 */
		public String getScrollJs() {
			return String
					.format("window.scrollTo("
							+ "Math.round(%f * document.getElementsByTagName('html')[0].scrollWidth),"
							+ "Math.round(%f * document.getElementsByTagName('html')[0].scrollHeight));",
							scrollXRel, scrollYRel);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Scale " + scale + ", Scroll " + scrollXRel + "/"
					+ scrollYRel + ", Landscape " + Boolean.toString(landscape);
		}

	}

}
