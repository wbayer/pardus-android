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

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class managing properties for each visited page and screen orientation (zoom
 * level, scroll position). Persists all data to disk.
 */
public class PardusPageProperties {

	private static final String FILENAME = "pageproperties.ser";

	private Map<PardusPageIdentifier, PardusPageProperty> properties = new HashMap<>();

	private String lastUrl;

	private int lastOrientation;

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
	 * @param orientation
	 *            the screen's orientation (Configuration.ORIENTATION_LANDSCAPE
	 *            or Configuration.ORIENTATION_PORTRAIT)
	 * @param scale
	 *            the zoom level
	 * @param posX
	 *            the x-coordinate of the top-left position of the viewport (in
	 *            dp * scale)
	 * @param posY
	 *            the y-coordinate of the top-left position of the viewport (in
	 *            dp * scale)
	 * @param totalX
	 *            the width of the web page (in dp * scale)
	 * @param totalY
	 *            the height of the web page (in dp * scale)
	 */
	public void save(String url, int orientation, float scale, int posX,
			int posY, int totalX, int totalY) {
		if (url == null || url.contains("/game.php")
				|| (url.equals(lastUrl) && orientation == lastOrientation)) {
			return;
		}
		lastUrl = url;
		lastOrientation = orientation;
		PardusPageIdentifier identifier = new PardusPageIdentifier(url,
				orientation);
		if (identifier.url == null) {
			return;
		}
		boolean noScroll = identifier.url.equals("FORUM/IN_THREAD")
				|| identifier.url.equals("FORUM/POST")
				|| identifier.url.equals("FORUM/SEARCH_RESULT");
		PardusPageProperty property = new PardusPageProperty(scale,
				noScroll ? -1 : posX, noScroll ? -1 : posY, totalX, totalY);
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(), "Saving properties for "
					+ identifier + " (" + url + "): " + property);
		}
		properties.put(identifier, property);
	}

	/**
	 * Looks up properties for a page.
	 * 
	 * @param url
	 *            the URL of the page
	 * @param orientation
	 *            the orientation of the screen
	 * @return the PardusPageProperty object or null if none available
	 */
	public PardusPageProperty get(String url, int orientation) {
		PardusPageIdentifier identifier = new PardusPageIdentifier(url,
				orientation);
		return (identifier.url == null) ? null : properties.get(identifier);
	}

	/**
	 * Persists all entries to the disk.
	 */
	public void persist() {
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Persisting " + properties.size() + " page properties ...");
		}
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(context.openFileOutput(FILENAME,
					Context.MODE_PRIVATE));
			os.writeObject(properties);
			os.close();
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Error persisting page properties. "
							+ Log.getStackTraceString(e));
			try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception ignored) {
			}
		}
	}

	/**
	 * Loads all entries from the disk.
	 */
	@SuppressWarnings("unchecked")
    private void loadFromDisk() {
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Loading page properties from disk ...");
		}
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(context.openFileInput(FILENAME));
			properties = (Map<PardusPageIdentifier, PardusPageProperty>) is
					.readObject();
		} catch (FileNotFoundException e) {
			if (BuildConfig.DEBUG) {
				Log.d(this.getClass().getSimpleName(),
						"No page properties saved yet");
			}
		} catch (InvalidClassException e) {
			Log.i(this.getClass().getSimpleName(),
					"Could not deserialize page properties, likely due to a class update. "
							+ e);
		} catch (Exception e) {
			Log.w(this.getClass().getSimpleName(),
					"Error loading page properties from disk. "
							+ Log.getStackTraceString(e));
		}
		try {
			if (is != null) {
				is.close();
			}
		} catch (Exception ignored) {
		}
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Loaded " + properties.size() + " page properties");
		}
	}

	/**
	 * Wipe all saved properties persistently.
	 */
	public void forget() {
		properties = new HashMap<>();
		persist();
		resetLastUrl();
	}

	/**
	 * Resets the last URL so its properties may be overridden (i.e. after a
	 * manual page refresh).
	 */
	public void resetLastUrl() {
		lastUrl = null;
	}

	/**
	 * @return a new PardusPageProperty object initialized with zero values
	 */
	public static PardusPageProperty getEmptyProperty() {
		return new PardusPageProperty(0.0f, 0, 0, 0, 0);
	}

	/**
	 * @return a new PardusPageProperty object with scroll positions set to -1
	 *         to indicate that no automatic scrolling should take place
	 */
	public static PardusPageProperty getNoScrollProperty() {
		return new PardusPageProperty(0.0f, -1, -1, 0, 0);
	}

	/**
	 * Immutable object serving as identifier of a page (URL and screen
	 * orientation).
	 */
	protected static class PardusPageIdentifier implements Serializable {

		private static final long serialVersionUID = 1L;

		protected final String url;

		protected final int orientation;

		/**
		 * Constructor. URL will be trimmed and set to null if invalid.
		 * 
		 * @param url
		 *            the page's URL
		 * @param orientation
		 *            the screen's orientation
		 */
		private PardusPageIdentifier(String url, int orientation) {
			this.url = trimUrl(url);
			this.orientation = orientation;
		}

		/**
		 * Removes/replaces parts of a URL in order to shorten it and have
		 * certain pages share one property (i.e. no protocol dependency, no
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
				url = "PORTAL/"
						+ url.substring(url.indexOf(".pardus.at/") + 11);
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return url
					+ " ("
					+ (orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape"
							: "portrait") + ")";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + orientation;
			result = prime * result + ((url == null) ? 0 : url.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PardusPageIdentifier other = (PardusPageIdentifier) obj;
			if (orientation != other.orientation)
				return false;
			if (url == null) {
                return other.url == null;
			} else
                return url.equals(other.url);
        }

	}

	/**
	 * Immutable object containing display attributes of a page.
	 */
	public static class PardusPageProperty implements Serializable {

		private static final long serialVersionUID = 2L;

		public final float scale;

		public final int posX;
        public final int posY;
        protected final int totalX;
        protected final int totalY;

		@SuppressWarnings("unused")
        public final long timestamp = System.currentTimeMillis();

		/**
		 * Constructor.
		 * 
		 * @param scale
		 *            the zoom level
		 * @param posX
		 *            the x-coordinate of the top-left position of the viewport
		 *            (in dp * scale)
		 * @param posY
		 *            the y-coordinate of the top-left position of the viewport
		 *            (in dp * scale)
		 * @param totalX
		 *            the width of the web page (in dp * scale)
		 * @param totalY
		 *            the height of the web page (in dp * scale)
		 */
		private PardusPageProperty(float scale, int posX, int posY, int totalX,
				int totalY) {
			this.scale = scale;
			this.posX = posX;
			this.posY = posY;
			this.totalX = totalX;
			this.totalY = totalY;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Scale " + (int) Math.ceil(scale * 100 - 0.5f)
					+ ", Scroll-X " + posX + "/" + totalX + ", Scroll-Y "
					+ posY + "/" + totalY;
		}

	}

}
