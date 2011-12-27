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

/**
 * Pardus constants: URLs, etc.
 */
public abstract class PardusConstants {

	public static final boolean DEBUG = false;

	/* Local pages (absolute URLs) */

	public static final String loginScreen = "file:///android_asset/login.html";

	public static final String settingsScreen = "file:///android_asset/settings.html";

	public static final String imageSelectionScreen = "file:///android_asset/img.html";

	public static final String linksConfigScreen = "file:///android_asset/links.html";

	public static final String aboutScreen = "file:///android_asset/about.html";

	/* Remote pages (absolute URLs) */

	public static final String loginUrlOrig = "http://www.pardus.at/index.php?section=login";

	public static final String loginUrlHttpsOrig = "https://www.pardus.at/index.php?section=login";

	public static final String loginUrl = loginUrlOrig + "&mobile";

	public static final String loginUrlHttps = loginUrlHttpsOrig + "&mobile";

	public static final String loggedInUrl = "http://www.pardus.at/index.php?section=account_play";

	public static final String loggedInUrlHttps = "https://www.pardus.at/index.php?section=account_play";

	public static final String newCharUrl = "http://www.pardus.at/index.php?section=account_newchar";

	public static final String newCharUrlHttps = "https://www.pardus.at/index.php?section=account_newchar";

	public static final String logoutUrl = "http://www.pardus.at/index.php?section=account_logout";

	public static final String logoutUrlHttps = "https://www.pardus.at/index.php?section=account_logout";

	public static final String signupUrl = "http://www.pardus.at/index.php?section=signup";

	public static final String signupUrlHttps = "https://www.pardus.at/index.php?section=signup";

	/* Universe-specific pages, default menu links (absolute URLs) */

	public static final String chatUrlHttps = "https://chat.pardus.at/chat.php";

	public static final String forumUrlHttps = "https://forum.pardus.at/index.php";

	/* Universe-specific pages, default menu links (relative URLs) */

	public static final String gameFrame = "game.php";

	public static final String msgFrame = "msgframe.php";

	public static final String navPage = "main.php";

	public static final String overviewPage = "overview.php";

	public static final String msgPage = "messages.php";

	public static final String sendMsgPage = "sendmsg.php";

	public static final String newsPage = "news.php";

	public static final String diploPage = "diplo_page.php";

	public static final String statsPage = "statistics.php";

	public static final String optionsPage = "options.php";

	/* Other universe-specific pages (relative URLs) */

	public static final String msgPagePrivate = "messages_private.php";

	public static final String msgPageAlliance = "messages_alliance.php";

	public static final String tradeLogsPage = "overview_tl_res.php";

	public static final String tradeLogsEqPage = "overview_tl_eq.php";

	public static final String missionsLogPage = "overview_missions_log.php";

	public static final String combatLogPage = "overview_combat_log.php";

	public static final String paymentLogPage = "overview_payment_log.php";

}
