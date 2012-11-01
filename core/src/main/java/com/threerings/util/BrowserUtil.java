//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.util;

import java.net.URL;

import com.samskivert.util.ResultListener;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import static com.threerings.NenyaLog.log;

/**
 * Encapsulates a bunch of hackery needed to invoke an external web browser
 * from within a Java application.
 */
public class BrowserUtil
{
    /**
     * Opens the user's web browser with the specified URL.
     *
     * @param url the URL to display in an external browser.
     * @param listener a listener to be notified if we failed to launch the
     * browser. <em>Note:</em> it will not be notified of success.
     */
    public static void browseURL (URL url, ResultListener<Void> listener)
    {
        browseURL(url, listener, "firefox");
    }

    /**
     * Opens the user's web browser with the specified URL.
     *
     * @param url the URL to display in an external browser.
     * @param listener a listener to be notified if we failed to launch the
     * browser. <em>Note:</em> it will not be notified of success.
     * @param genagent the path to the browser to execute on non-Windows,
     * non-MacOS.
     */
    public static void browseURL (URL url, ResultListener<Void> listener, String genagent)
    {
        String[] cmd;
        if (RunAnywhere.isWindows()) {
            // TODO: test this on Vinders 98
//          cmd = new String[] { "rundll32", "url.dll,FileProtocolHandler",
//              url.toString() };
            String osName = System.getProperty("os.name");
            if (osName.indexOf("9") != -1 || osName.indexOf("Me") != -1) {
                cmd = new String[] { "command.com", "/c", "start",
                                     "\"" + url.toString() + "\"" };
            } else {
                cmd = new String[] { "cmd.exe", "/c", "start", "\"\"",
                                     "\"" + url.toString() + "\"" };
            }

        } else if (RunAnywhere.isMacOS()) {
            cmd = new String[] { "open", url.toString() };

        } else { // Linux, Solaris, etc
            cmd = new String[] { genagent, url.toString() };
        }

        // obscure any password information
        String logcmd = StringUtil.join(cmd, " ");
        logcmd = logcmd.replaceAll("password=[^&]*", "password=XXX");
        log.info("Browsing URL [cmd=" + logcmd + "].");

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BrowserTracker tracker = new BrowserTracker(process, url, listener);
            tracker.start();
        } catch (Exception e) {
            log.warning("Failed to launch browser [url=" + url +
                        ", error=" + e + "].");
            listener.requestFailed(e);
        }
    }

    protected static class BrowserTracker extends Thread
    {
        public BrowserTracker (Process process, URL url, ResultListener<Void> rl) {
            super("BrowserLaunchWaiter");
            setDaemon(true);
            _process = process;
            _url = url;
            _listener = rl;
        }

        @Override
        public void run () {
            try {
                _process.waitFor();
                int rv = _process.exitValue();
                if (rv == 0) {
                    return;
                }

                String errmsg = "Launched browser failed [rv=" + rv + "].";
                log.warning(errmsg);
                if (!RunAnywhere.isWindows()) {
                    _listener.requestFailed(new Exception(errmsg));
                    return;
                }

                // if we're on windows, make a last ditch effort
                String[] cmd = new String[] {
                    "C:\\Program Files\\Internet Explorer\\" +
                    "IEXPLORE.EXE", "\"" + _url.toString() + "\""};
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                rv = process.exitValue();
                if (rv != 0) {
                    errmsg = "Failed to launch iexplore.exe [rv=" + rv + "].";
                    log.warning(errmsg);
                    _listener.requestFailed(new Exception(errmsg));
                }

            } catch (Exception e) {
                _listener.requestFailed(e);
            }
        }

        protected Process _process;
        protected URL _url;
        protected ResultListener<Void> _listener;
    }
}
