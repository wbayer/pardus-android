/*
 *    Copyright 2016 Werner Bayer
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

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import at.pardus.android.browser.PardusPreferences;
import at.pardus.android.browser.R;

/**
 * Contains methods to be called by JavaScript from the Login screen.
 */
public class JavaScriptLogin {

    public static final String DEFAULT_JS_NAME = "JavaLogin";

    private Activity activity;

    /**
     * Constructor.
     *
     * @param activity the activity the browser component runs in
     */
    public JavaScriptLogin(Activity activity) {
        this.activity = activity;
    }

    /**
     * Saves account and password if confirmed by the user.
     */
    @JavascriptInterface
    public void askSavePassword(final String account, final String password) {
        PardusPreferences.StoreCredentials storeCredentials = PardusPreferences.getStoreCredentials();
        if (storeCredentials == PardusPreferences.StoreCredentials.NEVER) {
            return;
        }
        if (storeCredentials == PardusPreferences.StoreCredentials.NO) {
            // ask whether to save the user's password
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bundle bundle = new Bundle();
                    bundle.putString("account", account);
                    bundle.putString("password", password);
                    activity.showDialog(R.id.dialog_save_password, bundle);
                }
            });
        } else if (storeCredentials == PardusPreferences.StoreCredentials.YES) {
            // update password
            PardusPreferences.setAccount(account);
            PardusPreferences.setPassword(password);
        }
    }

}
