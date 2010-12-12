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

	public static final String loginScreen = "file:///android_asset/login.html";

	public static final String imageSelectionScreen = "file:///android_asset/img.html";

	public static final String aboutScreen = "file:///android_asset/about.html";

	public static final String loginUrl = "https://www.pardus.at/index.php?section=login";

	public static final String loggedInUrl = "http://www.pardus.at/index.php?section=account_play";

	public static final String loggedInUrlHttps = "https://www.pardus.at/index.php?section=account_play";

	public static final String newCharUrl = "http://www.pardus.at/index.php?section=account_newchar";

	public static final String newCharUrlHttps = "https://www.pardus.at/index.php?section=account_newchar";

	public static final String logoutUrl = "http://www.pardus.at/index.php?section=account_logout";

	public static final String logoutUrlHttps = "https://www.pardus.at/index.php?section=account_logout";

	public static final String signupUrl = "http://www.pardus.at/index.php?section=signup";

	public static final String signupUrlHttps = "https://www.pardus.at/index.php?section=signup";

	public static final String navPage = "game.php";

	public static final String msgPage = "game.php?messages=1";

	public static final String sendMsgPage = "sendmsg.php";

	public static final String forumPage = "game.php?forum=1";

	public static final String chatPage = "game.php?chat=1";

}
