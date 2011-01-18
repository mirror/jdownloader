package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freecaster.tv" }, urls = { "http://videos.freecaster.com/.++" }, flags = { 0 })
public class FreecasterTv extends PluginForHost {

    public FreecasterTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://freecaster.tv/info/general-terms-of-service";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        try {
            if (!br.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
                long size = br.getHttpConnection().getLongContentLength();
                downloadLink.setDownloadSize(Long.valueOf(size));
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setFinalFileName(link.getStringProperty("filename", "NA"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }
}
