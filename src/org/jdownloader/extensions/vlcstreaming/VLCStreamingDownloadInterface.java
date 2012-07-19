package org.jdownloader.extensions.vlcstreaming;

import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

public class VLCStreamingDownloadInterface extends DownloadInterface {

    public VLCStreamingDownloadInterface(VLCStreamingThread vlcStreamingThread, PluginForHost plugin, DownloadLink downloadLink, Request request) throws Exception {
        super(plugin, downloadLink, request);
    }

    @Override
    protected boolean checkResumabled() {
        return true;
    }

    @Override
    public boolean isRangeRequestSupported() {
        return true;
    }

    @Override
    public boolean isResumable() {
        return true;
    }

    @Override
    public URLConnectionAdapter connect() throws Exception {
        String rangeRequest = downloadLink.getStringProperty("streamingRange", null);
        if (rangeRequest != null) {
            request.getHeaders().put("Range", rangeRequest);
        } else {
            request.getHeaders().remove("Range");
        }
        browser.connect(request);
        if (this.plugin.getBrowser().isDebug()) logger.finest("\r\n" + request.printHeaders());
        connection = request.getHttpConnection();
        if (request.getLocation() != null) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, DownloadInterface.ERROR_REDIRECTED);
        if (connection.getRange() != null) {
            /* we have a range response, let's use it */
            if (connection.getRange()[2] > 0) {
                this.setFilesizeCheck(true);
                this.downloadLink.setDownloadSize(connection.getRange()[2]);
            }
        } else if (rangeRequest == null && connection.getLongContentLength() > 0 && connection.isOK()) {
            this.setFilesizeCheck(true);
            this.downloadLink.setDownloadSize(connection.getLongContentLength());
        }
        return connection;
    }

    @Override
    public boolean startDownload() throws Exception {
        return false;
    }

    @Override
    public synchronized void stopDownload() {
    }

    @Override
    public synchronized boolean externalDownloadStop() {
        return true;
    }

    @Override
    protected void onChunksReady() {
    }

    @Override
    protected void setupChunks() throws Exception {
    }

    @Override
    public void cleanupDownladInterface() {
    }

    @Override
    protected boolean writeChunkBytes(Chunk chunk) {
        return false;
    }

}
