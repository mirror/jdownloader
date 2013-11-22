package org.jdownloader.extensions.streaming;

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

public class StreamingDownloadInterface extends DownloadInterface {

    private ManagedThrottledConnectionHandler connectionHandler;
    private DownloadLink                      downloadLink;
    private PluginForHost                     plugin;
    private Request                           request;
    private Browser                           browser;
    private URLConnectionAdapter              connection;

    public StreamingDownloadInterface(PluginForHost plugin, DownloadLink downloadLink, Request request) throws Exception {
        connectionHandler = new ManagedThrottledConnectionHandler();
        this.downloadLink = downloadLink;
        this.plugin = plugin;
        browser = plugin.getBrowser().cloneBrowser();
        this.request = request;
        plugin.setDownloadInterface(this);
    }

    @Override
    public boolean startDownload() throws Exception {
        return false;
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return 0;
    }

    @Override
    public boolean isResumable() {
        return true;
    }

    @Override
    public URLConnectionAdapter getConnection() {
        return connection;
    }

    @Override
    public void stopDownload() {
    }

    @Override
    public boolean externalDownloadStop() {
        return true;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        String rangeRequest = downloadLink.getStringProperty("streamingRange", null);
        if (rangeRequest != null) {
            request.getHeaders().put("Range", rangeRequest);
        } else {
            request.getHeaders().remove("Range");
        }
        browser.connect(request);
        if (this.plugin.getBrowser().isDebug()) plugin.getLogger().finest("\r\n" + request.printHeaders());
        connection = request.getHttpConnection();
        if (request.getLocation() != null) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, BrowserAdapter.ERROR_REDIRECTED);
        if (connection.getRange() != null) {
            /* we have a range response, let's use it */
            if (connection.getRange()[2] > 0) {
                this.downloadLink.setDownloadSize(connection.getRange()[2]);
            }
        } else if (rangeRequest == null && connection.getLongContentLength() > 0 && connection.isOK()) {
            this.downloadLink.setDownloadSize(connection.getLongContentLength());
        }
        return connection;
    }

    @Override
    public long getStartTimeStamp() {
        return 0;
    }

    @Override
    public void close() {
    }

}
