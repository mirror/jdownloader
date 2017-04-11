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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ul-instant.pw" }, urls = { "https?://ul\\-instant\\.pw/dl/[A-F0-9\\-]+" })
public class UlInstantPw extends PluginForHost {

    public UlInstantPw(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ul-instant.pw/";
    }

    private boolean                    resumeSupported = true;
    private int                        maxChunks       = 0;
    private final static AtomicInteger maxDownloads    = new AtomicInteger(10);

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
            } else if (connection.getResponseCode() == 503) {
                /* Usually with http response 'file is in queue...' and "Retry-After"-HeaderField */
                return AvailableStatus.UNCHECKABLE;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxDownloads.get();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String headerRetryAfter = br.getHttpConnection().getHeaderField("Retry-After");
        if (headerRetryAfter != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 wait and retry", Integer.parseInt(headerRetryAfter) * 1000l);
        }
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
