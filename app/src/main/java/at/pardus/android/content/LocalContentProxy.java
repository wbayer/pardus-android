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

import android.util.Log;

import java.io.File;
import java.io.IOException;

import at.pardus.android.browser.BuildConfig;
import fi.iki.elonen.SimpleWebServer;

/**
 * Provides a URI to files served by a local webserver.
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
     * Starts a local web server to serve files.
     *
     * @param path the path to the files to serve
     */
    public synchronized void start(String path) {
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
     * Stops the web server.
     */
    public void stop() {
        if (simpleWebServer != null) {
            if (BuildConfig.DEBUG) {
                Log.v(getClass().getSimpleName(), "Stopping web server");
            }
            simpleWebServer.stop();
        }
    }

}
