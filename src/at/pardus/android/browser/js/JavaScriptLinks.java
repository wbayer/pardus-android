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

package at.pardus.android.browser.js;

import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import android.util.Log;
import at.pardus.android.browser.PardusConstants;
import at.pardus.android.browser.PardusLinks;
import at.pardus.android.browser.PardusLinks.PardusLink;

/**
 * Contains methods to be called by JavaScript from the Links configuration
 * screen.
 */
public class JavaScriptLinks {

	private PardusLinks links;

	/**
	 * Constructor.
	 * 
	 * @param links
	 *            a PardusLinks object to work on
	 */
	public JavaScriptLinks(PardusLinks links) {
		this.links = links;
	}

	/**
	 * @return all links in serialized form
	 */
	public String getLinks() {
		String serialized = PardusLinks.PardusLinkStore.serializeLinks(links
				.getLinks());
		if (PardusConstants.DEBUG) {
			Log.v(this.getClass().getSimpleName(),
					"Sending links to javascript: " + serialized);
		}
		return serialized;
	}

	/**
	 * Saves links as received from javascript.
	 * 
	 * @param links
	 *            string containing order, title, url for each link
	 */
	public void saveLinks(String links) {
		if (PardusConstants.DEBUG) {
			Log.d(this.getClass().getSimpleName(),
					"Receiving updated links from javascript: " + links);
		}
		SortedMap<Integer, PardusLink> sortedMap = new TreeMap<Integer, PardusLink>();
		StringTokenizer tokenizer = new StringTokenizer(links, PardusLink.GLUE);
		while (tokenizer.hasMoreTokens()) {
			int order;
			try {
				order = Integer.parseInt(tokenizer.nextToken());
			} catch (NumberFormatException e) {
				order = 99;
			}
			String title = tokenizer.nextToken();
			String url = tokenizer.nextToken();
			PardusLink link = new PardusLink(title, url);
			while (sortedMap.containsKey(order)) {
				order++;
			}
			sortedMap.put(order, link);
		}
		PardusLink[] linkArray = sortedMap.values().toArray(new PardusLink[0]);
		this.links.updateLinksViaHandler(linkArray);
	}
}
