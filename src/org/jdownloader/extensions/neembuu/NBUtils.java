/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class NBUtils {
    public static URLConnectionAdapter copyConnection(DownloadLink downloadLink, DownloadInterface di, PluginForHost plugin, long startByte, Browser b, URLConnectionAdapter connection, int retriesMade) {
        long start = startByte;
        Logger logger = plugin.getLogger();
        // String end = (endByte > 0 ? endByte + 1 : "") + "";

        /*
         * if (start == 0) { di.logger.finer("Takeover 0 Connection"); return connection; }
         */
        long oldConnectionStart = 0; // assumption :-x
        if (connection.getRange() != null) oldConnectionStart = connection.getRange()[0];
        if (oldConnectionStart == (start) && retriesMade < 6) {
            // if retried this a number of times,
            // we must try making a newconnection.
            logger.finer("Takeover connection at " + oldConnectionStart);
            return connection;
        }
        try {
            /* only forward referer if referer already has been sent! */
            boolean forwardReferer = /* plugin.getBrowser() */b.getHeaders().contains("Referer");
            Browser br = /* plugin.getBrowser() */b.cloneBrowser();
            br.setReadTimeout(plugin.getBrowser().getReadTimeout());
            br.setConnectTimeout(plugin.getBrowser().getConnectTimeout());
            /* set requested range */

            Map<String, String> request = connection.getRequestProperties();
            if (request != null) {
                String value;
                for (Entry<String, String> next : request.entrySet()) {
                    if (next.getValue() == null) continue;
                    value = next.getValue().toString();
                    br.getHeaders().put(next.getKey(), value);
                }
            }
            if (!forwardReferer) {
                /* only forward referer if referer already has been sent! */
                br.setCurrentURL(null);
            }
            URLConnectionAdapter con = null;
            // clonedconnection = true;
            if (connection.getRequestMethod() == RequestMethod.POST) {
                connection.getRequest().getHeaders().put("Range", "bytes=" + start + "-");
                con = br.openRequestConnection(connection.getRequest());
            } else {
                br.getHeaders().put("Range", "bytes=" + start + "-");
                con = br.openGetConnection(connection.getURL() + "");
            }
            if (!con.isOK()) {
                try {
                    /* always close connections that got opened */
                    con.disconnect();
                } catch (Throwable e) {
                }
                if (con.getResponseCode() != 416) {
                    logger.severe(LinkStatus.ERROR_DOWNLOAD_FAILED + "Server: " + con.getResponseMessage());
                } else {
                    logger.warning("HTTP 416, maybe finished last chunk?");
                }
                return null;
            }
            if (con.getHeaderField("Location") != null) {
                try {
                    /* always close connections that got opened */
                    con.disconnect();
                } catch (Throwable e) {
                }
                logger.severe(LinkStatus.ERROR_DOWNLOAD_FAILED + "Server: Redirect");
                return null;
            }
            return con;
        } catch (Exception e) {
            LogSource.exception(logger, e);
            logger.log(Level.SEVERE, "ERROR_RETRY", e);
        }
        return null;
    }
}
