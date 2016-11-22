package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;

import jd.http.Request;
import jd.network.rtmp.RtmpDump;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.RTMPStreamHandler;
import jd.plugins.download.RAFDownload;

/* Old librtmp handling in revision < 13938 */

/**
 * This is a wrapper for RTMP
 *
 * @author thomas
 * @author bismarck
 *
 */
public class RTMPDownload extends RAFDownload {

    protected final RtmpUrlConnection rtmpConnection;

    private final URL                 url;
    // don't name it plugin!
    protected final PluginForHost     plg;
    // don't name it downloadLink
    protected final DownloadLink      dLink;

    public RTMPDownload(final PluginForHost plugin, final DownloadLink downloadLink, final String rtmpURL) throws IOException, PluginException {
        super(plugin, downloadLink, null);
        this.plg = plugin;
        this.dLink = downloadLink;
        url = new URL(null, rtmpURL, new RTMPStreamHandler());
        rtmpConnection = new RtmpUrlConnection(url);
    }

    public void setInitialRequest(Request initialRequest) {
    }

    public RtmpUrlConnection getRtmpConnection() {
        return rtmpConnection;
    }

    public boolean startDownload() throws Exception {
        /*
         * Remove/replace chars which will cause rtmpdump to fail to find our download destination! TODO: Invalid download destination will
         * cause plugin defect - maybe catch this and show invalid download path instead!
         */
        String finalfilename = this.plg.getDownloadLink().getFinalFileName();
        if (finalfilename == null) {
            finalfilename = this.plg.getDownloadLink().getName();
        }
        finalfilename = finalfilename.replace("⁄", "_");
        this.plg.getDownloadLink().setFinalFileName(finalfilename);
        long before = 0;
        while (!externalDownloadStop()) {
            before = Math.max(before, downloadable.getDownloadBytesLoaded());
            final RtmpDump rtmpDump = rtmpDump();
            try {
                return rtmpDump.start(rtmpConnection);
            } catch (PluginException e) {
                logger.log(e);
                if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE && (rtmpDump.isReadPacketError(e.getMessage()) || rtmpDump.isTimeoutError(e.getMessage()))) {
                    if (downloadable.getDownloadBytesLoaded() > before && rtmpConnection.isResume()) {
                        Thread.sleep(1000);
                        continue;
                    }
                }
                throw e;
            }
        }
        return false;
    }

    public String getRtmpDumpChecksum() throws Exception {
        return rtmpDump().getRtmpDumpChecksum();
    }

    public String getRtmpDumpVersion() throws Exception {
        return rtmpDump().getRtmpDumpVersion();
    }

    private RtmpDump rtmpDump() throws Exception {
        return new RtmpDump(plg, dLink, String.valueOf(url));
    }

}