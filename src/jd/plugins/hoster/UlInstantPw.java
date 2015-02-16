package jd.plugins.hoster;

import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 28942 $", interfaceVersion = 3, names = { "ul-instant.pw" }, urls = { "http://ul-instant\\.pw/download/[a-f0-9]{32}/[a-f0-9]{32}/\\d+/[\\w]+/[\\w]+/[\\%\\w\\.\\-]+" }, flags = { 0 })
public class UlInstantPw extends PluginForHost {

    public UlInstantPw(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ul-instant.pw/";
    }

    private boolean                    resumeSupported = false;
    private int                        maxChunks       = 1;
    private final static AtomicInteger maxDownloads    = new AtomicInteger(1);

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        URLConnectionAdapter connection = null;
        try {
            connection = br.openHeadConnection(link.getDownloadURL());
            if (connection.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (connection.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
            }
            if (connection.isOK() && connection.isContentDisposition()) {
                link.setFinalFileName(getFileNameFromHeader(connection));
                link.setVerifiedFileSize(connection.getCompleteContentLength());
                resumeSupported = "true".equals(connection.getHeaderField("X-ResumeSupported"));
                final String headerMaxChunks = connection.getHeaderField("X-MaxChunks");
                if (headerMaxChunks != null && headerMaxChunks.matches("\\d+")) {
                    final int max = Integer.parseInt(headerMaxChunks);
                    if (max > 1) {
                        maxChunks = -max;
                    } else if (max <= 0) {
                        maxChunks = max;
                    } else {
                        maxChunks = 1;
                    }
                }
                final String headerMaxDownloads = connection.getHeaderField("X-MaxDownloads");
                if (headerMaxDownloads != null && headerMaxDownloads.matches("\\d+")) {
                    maxDownloads.set(Integer.parseInt(headerMaxDownloads));
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return AvailableStatus.TRUE;
        } finally {
            if (connection != null) {
                br.followConnection();
                connection.disconnect();
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxDownloads.get();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, link.getDownloadURL(), resumeSupported, maxChunks);
        if (!dl.getConnection().isOK() || !dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
