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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.StringTokenizer;

/**
 * Class managing Pardus links.
 */
public class PardusLinks {

	public static final int HIDE_AFTER_SHOW_MILLIS = 5000;

	private static final int HIDE_AFTER_CLICK_MILLIS = 500;

	private static final int HIDE_ANIMATION_MILLIS = 2000;

	private static final int BUTTON_WIDTH_PX = Math
			.round(80 * Pardus.displayDensityScale);

	private static final int BUTTON_HEIGHT_PX = Math
			.round(40 * Pardus.displayDensityScale);

	private static final int ROW_VSPACING_PX = Math
			.round(Pardus.displayDensityScale);

	private PardusWebView browser;

	private GridView linksGridView;

	private RelativeLayout.LayoutParams layoutParams;

	private Animation fadeOutAnimation;

	private PardusLinkStore linkStore;

	private PardusButtonAdapter buttonAdapter;

	private final Handler handler;

	private final Runnable hideRunnable = this::hide;

	private boolean hidingScheduled = false;

	private boolean visible = false;

	/**
	 * Constructor.
	 * 
	 * @param handler
	 *            the handler created by the main/UI thread
	 * @param context
	 *            the application's context
	 * @param layoutInflater
	 *            the currently configured LayoutInflater
	 * @param browser
	 *            the Pardus browser component
	 * @param gridView
	 *            the GridView to contain the links
	 */
	public PardusLinks(Context context, Handler handler,
			LayoutInflater layoutInflater, PardusWebView browser,
			GridView gridView) {
		this.handler = handler;
		this.browser = browser;
		this.linksGridView = gridView;
		fadeOutAnimation = AnimationUtils.loadAnimation(context,
				R.anim.fade_out);
		fadeOutAnimation.setDuration(HIDE_ANIMATION_MILLIS);
		fadeOutAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				linksGridView.setVisibility(View.GONE);
				visible = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {

			}

		});
		linkStore = new PardusLinkStore();
		layoutParams = new RelativeLayout.LayoutParams(0, 0);
		layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		calcAndApplyDimensions();
		buttonAdapter = new PardusButtonAdapter(layoutInflater,
				R.layout.button, linkStore.getLinks());
		linksGridView.setAdapter(buttonAdapter);
		initClickListener();
	}

	/**
	 * Shows the grid view containing Pardus links.
	 */
	public void show() {
		if (hidingScheduled) {
			handler.removeCallbacks(hideRunnable);
			hidingScheduled = false;
		}
		if (visible) {
			return;
		}
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(), "Showing links");
		}
		linksGridView.setVisibility(View.VISIBLE);
		visible = true;
		if (BuildConfig.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Links grid view width/height set to (px): "
							+ layoutParams.width + "/" + layoutParams.height);
		}
	}

	/**
	 * Starts a timer to hide the grid view containing Pardus links.
	 * 
	 * @param millis
	 *            hide the grid view after millis milli-seconds
	 */
	public void startHideTimer(int millis) {
		if (hidingScheduled || !visible) {
			return;
		}
		hidingScheduled = true;
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(), "Hiding links after "
					+ millis + " milli-seconds");
		}
		handler.removeCallbacks(hideRunnable);
		handler.postDelayed(hideRunnable, millis);
	}

	/**
	 * Starts the fade-out animation to hide the grid view.
	 */
	public void hide() {
		if (!visible) {
			return;
		}
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getSimpleName(), "Hiding links");
		}
		linksGridView.startAnimation(fadeOutAnimation);
	}

	/**
	 * @return true if the grid view is visible, false else
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @return currently used links
	 */
	public PardusLink[] getLinks() {
		return linkStore.getLinks();
	}

	/**
	 * Updates the grid view with new links.
	 * 
	 * @param links
	 *            new array of Pardus links
	 */
    private void updateLinks(PardusLink[] links) {
		linkStore.updateLinks(links);
		calcAndApplyDimensions();
		buttonAdapter.updateLinks(links);
		linksGridView.invalidateViews();
	}

	/**
	 * Updates the grid view from a non-UI thread (for example the javascript
	 * bridge).
	 * 
	 * @param links
	 *            new array of Pardus links
	 */
	public void updateLinksViaHandler(final PardusLink[] links) {
		handler.post(() -> updateLinks(links));
	}

	/**
	 * Calculates and applies the dimensions of the links grid view.
	 */
	public void calcAndApplyDimensions() {
		int numButtons = linkStore.getLinks().length + 1;
		int w = Math.max(
				Math.min(numButtons * BUTTON_WIDTH_PX, Pardus.displayWidthPx),
				BUTTON_WIDTH_PX);
		int numColumns = (int) Math.floor((w + 2) / BUTTON_WIDTH_PX);
		int numRows = (int) Math.ceil(numButtons / (float) numColumns);
		int h = Math.max(
				Math.min(numRows * (BUTTON_HEIGHT_PX + ROW_VSPACING_PX),
						Pardus.displayHeightPx), BUTTON_HEIGHT_PX);
		layoutParams.width = w;
		layoutParams.height = h;
		linksGridView.setLayoutParams(layoutParams);
	}

	/**
	 * Adds a listener for onClick events.
	 */
	private void initClickListener() {
        /*
         * (non-Javadoc)
         *
         * @see
         * android.widget.AdapterView.OnItemClickListener#onItemClick(android
         * .widget.AdapterView, android.view.View, int, long)
         */
        linksGridView.setOnItemClickListener((parent, v, position, id) -> {
            String link = (String) (parent.getItemAtPosition(position));
            boolean linkLoaded = false;
            if (!link.startsWith("http://") && !link.startsWith("https://")
                    && !link.startsWith("file://")) {
                // relative url (game universe page)
                if (browser.isLoggedIn() && browser.getUniverse() != null) {
                    browser.loadUniversePage(link);
                    linkLoaded = true;
                } else {
                    PardusNotification.show("Please enter a universe!");
                }
            } else {
                // absolute url
                if ((link.startsWith(PardusConstants.chatUrlHttps) || link
                        .startsWith(PardusConstants.forumUrlHttps))
                        && browser.getUniverse() == null) {
                    PardusNotification.show("Please enter a universe!");
                } else {
                    browser.loadUrl(link);
                    linkLoaded = true;
                }
            }
            if (linkLoaded) {
                show();
                startHideTimer(PardusLinks.HIDE_AFTER_CLICK_MILLIS);
            }
        }

        );
	}

	/**
	 * Responsible for the retrieval/deserialization and setting/serialization
	 * of Pardus link objects.
	 */
	public static class PardusLinkStore {

		/**
		 * Serializes an array of PardusLink objects.
		 * 
		 * @param links
		 *            array to serialize
		 * @return the original array as string
		 */
		public static String serializeLinks(PardusLink[] links) {
			if (links.length == 0) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (PardusLink link : links) {
				sb.append(PardusLink.GLUE);
				sb.append(link);
			}
			return sb.toString().substring(1);
		}

		/**
		 * Deserializes a string into an array of PardusLink objects.
		 * 
		 * @param serialized
		 *            string of serialized objects
		 * @return array of deserialized PardusLink objects
		 */
		protected static PardusLink[] deserializeLinks(String serialized) {
			if (serialized.length() == 0) {
				return new PardusLink[0];
			}
			StringTokenizer tokenizer = new StringTokenizer(serialized,
					PardusLink.GLUE);
			PardusLink[] links = new PardusLink[tokenizer.countTokens() / 2];
			int i = 0;
			while (tokenizer.hasMoreTokens()) {
				String title = tokenizer.nextToken();
				String url = tokenizer.nextToken();
				links[i] = new PardusLink(title, url);
				i++;
			}
			return links;
		}

		private PardusLink[] linkCache = null;

		/**
		 * Retrieves the persisted Pardus links.
		 * 
		 * @return array of deserialized Pardus link objects
		 */
        protected PardusLink[] getLinks() {
			if (linkCache != null) {
				return linkCache;
			}
			String serialized = PardusPreferences.getLinks();
			if (serialized == null) {
				PardusLink[] standardLinks = getStandardLinks();
				updateLinks(standardLinks);
				return standardLinks;
			}
			PardusLink[] links = deserializeLinks(serialized);
			linkCache = links;
			return links;
		}

		/**
		 * Persists a new set of Pardus links.
		 * 
		 * @param links
		 *            array of Pardus link objects to serialize and persist
		 */
        protected void updateLinks(PardusLink[] links) {
			String serialized = serializeLinks(links);
			PardusPreferences.setLinks(serialized);
			linkCache = links;
		}

		/**
		 * @return the set of pre-installed links
		 */
		private PardusLink[] getStandardLinks() {
			return new PardusLink[] {
					new PardusLink("Nav", PardusConstants.navPage),
					new PardusLink("Overv.", PardusConstants.overviewPage),
					new PardusLink("Msgs", PardusConstants.msgPage),
					new PardusLink("Send", PardusConstants.sendMsgPage),
					new PardusLink("News", PardusConstants.newsPage),
					new PardusLink("Diplo", PardusConstants.diploPage),
					new PardusLink("Stats", PardusConstants.statsPage),
					new PardusLink("Option", PardusConstants.optionsPage),
					new PardusLink("Chat", PardusConstants.chatUrlHttps),
					new PardusLink("Forum", PardusConstants.forumUrlHttps) };
		}

	}

	/**
	 * Immutable object containing a title and a URL.
	 */
	public static class PardusLink {

		public static final String GLUE = "|";

		private String title;

		private String url;

		/**
		 * Constructor.
		 * 
		 * @param title
		 *            the title of the link
		 * @param url
		 *            its target URL
		 */
		public PardusLink(String title, String url) {
			this.title = title.replace(GLUE, "");
			this.url = url.replace(GLUE, "");
		}

		/**
		 * @return the title
		 */
        protected String getTitle() {
			return title;
		}

		/**
		 * @return the URL
		 */
        protected String getUrl() {
			return url;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return title + GLUE + url;
		}

	}

	/**
	 * Class responsible for creating buttons (TextViews with
	 * label/background/link) for AdapterViews.
	 */
	private class PardusButtonAdapter extends BaseAdapter {

		private LayoutInflater layoutInflater;

		private int layout;

		private PardusLink[] links;

		private final PardusLink configLink = new PardusLink("Edit",
				PardusConstants.linksConfigScreen);

		/**
		 * Constructor.
		 * 
		 * @param layoutInflater
		 *            the currently configured LayoutInflater
		 * @param layout
		 *            a button's template resource (expected to be a TextView)
		 * @param links
		 *            array of Pardus links to use
		 */
        protected PardusButtonAdapter(LayoutInflater layoutInflater, int layout, PardusLink[] links) {
			this.layoutInflater = layoutInflater;
			this.layout = layout;
			this.links = links;
			addConfigLink();
		}

		/**
		 * Updates the links to use and notifies the view.
		 * 
		 * @param links
		 *            new array of Pardus links
		 */
        protected void updateLinks(PardusLink[] links) {
			this.links = links;
			addConfigLink();
			notifyDataSetChanged();
		}

		/**
		 * Adds the link configuration button as last item.
		 */
		private void addConfigLink() {
			PardusLink[] newLinks = new PardusLink[links.length + 1];
			System.arraycopy(links, 0, newLinks, 0, links.length);
			newLinks[links.length] = configLink;
			links = newLinks;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getCount()
		 */
		@Override
		public int getCount() {
			return links.length;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItem(int)
		 */
		@Override
		public Object getItem(int position) {
			return links[position].getUrl();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItemId(int)
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getView(int, android.view.View,
		 * android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView button;
			if (convertView == null) {
				button = (TextView) layoutInflater.inflate(layout, null);
			} else {
				button = (TextView) convertView;
			}
			String title = links[position].getTitle();
			if (title.length() > 6) {
				title = title.substring(0, 6);
			}
			button.setText(title);
			return button;
		}

	}

}
