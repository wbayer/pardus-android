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

package at.pardus.android.content;

import android.os.Build;
import android.util.Log;

import org.nanohttpd.webserver.SimpleWebServer;

import java.io.File;
import java.io.IOException;

import at.pardus.android.browser.BuildConfig;

/**
 * Provides a URI to local files through either a web server running on localhost (Android 4.4+) or
 * a content provider (Android 4.3-).
 */
public class LocalContentProxy {

    private static final LocalContentProxy INSTANCE = new LocalContentProxy();

    private static final String HOST = "localhost";
    private static final int PORT = 42983;

    private SimpleWebServer simpleWebServer;
    private String uri;

    private LocalContentProxy() {
    }

    /**
     * @return the LocalContentProxy instance
     */
    public static LocalContentProxy getInstance() {
        return INSTANCE;
    }

    public String getUri() {
        return uri;
    }

    /**
     * Decides whether to have local files served directly by a content provider (Android 4.3-) or
     * through a web server (Android 4.4+).
     *
     * @param path the path to the files to serve
     */
    public synchronized void start(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // android versions before kitkat make use of the local content provider directly
            uri = LocalContentProvider.URI;
            return;
        }
        // start the local image pack webserver
        if (BuildConfig.DEBUG) {
            Log.v(getClass().getSimpleName(),
                    "Starting web server at " + HOST + ":" + PORT + " to serve files from " + path);
        }
        if (simpleWebServer == null) {
            simpleWebServer = new SimpleWebServer(HOST, PORT, new File(path), true);
        }
        if (simpleWebServer.isAlive()) {
            if (BuildConfig.DEBUG) {
                Log.v(getClass().getSimpleName(), "Web server is still alive, not restarting");
            }
            return;
        }
        try {
            simpleWebServer.start();
            uri = "http://" + HOST + ":" + PORT;
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error starting web server", e);
            uri = null;
        }
    }

    /**
     * Stops the web server if it was at all used.
     */
    public void stop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // android versions before kitkat make use of the local content provider directly
            return;
        }
        if (simpleWebServer != null) {
            if (BuildConfig.DEBUG) {
                Log.v(getClass().getSimpleName(), "Stopping web server");
            }
            simpleWebServer.stop();
        }
    }

}
