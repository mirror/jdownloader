/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdownloader.extensions.neembuu;

import java.util.concurrent.atomic.AtomicReference;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class DownloadSession {
    private final DownloadLink                               downloadLink;
    private final DownloadInterface                          di;
    private final PluginForHost                              plugin;
    private final URLConnectionAdapter                       connection;
    private final Browser                                    b;

    private final AtomicReference<WatchAsYouDownloadSession> watchAsYouDownloadSessionRef = new AtomicReference<WatchAsYouDownloadSession>(null);

    public DownloadSession(DownloadLink downloadLink, DownloadInterface di, PluginForHost plugin, URLConnectionAdapter connection, Browser b) {
        this.downloadLink = downloadLink;
        this.di = di;
        this.plugin = plugin;
        this.connection = connection;
        this.b = b;
    }

    public final URLConnectionAdapter getURLConnectionAdapter() {
        return connection;
    }

    public final DownloadInterface getDownloadInterface() {
        return di;
    }

    public final DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public final Browser getBrowser() {
        return b;
    }

    public final PluginForHost getPluginForHost() {
        return plugin;
    }

    void setWatchAsYouDownloadSession(WatchAsYouDownloadSession wayds) {
        if (!watchAsYouDownloadSessionRef.compareAndSet(null, wayds)) { throw new IllegalStateException("Already initialized"); }
    }

    public final WatchAsYouDownloadSession getWatchAsYouDownloadSession() {
        if (watchAsYouDownloadSessionRef.get() == null) throw new IllegalArgumentException("Not initialized");
        return watchAsYouDownloadSessionRef.get();
    }

    @Override
    public String toString() {
        return downloadLink.toString();
    }
}
